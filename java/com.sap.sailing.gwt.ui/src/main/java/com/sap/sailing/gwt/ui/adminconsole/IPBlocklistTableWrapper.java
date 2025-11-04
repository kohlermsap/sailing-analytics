package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.TimedLock;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.AdminRole;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.ServerAdminRole;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.RoleWithSecurityDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.SelectedElementsCountingButton;

abstract class IPBlocklistTableWrapper
        extends TableWrapper<IpToTimedLockDTO, RefreshableSelectionModel<IpToTimedLockDTO>> {
    private final UserService userService;
    private final LabeledAbstractFilterablePanel<IpToTimedLockDTO> filterField;
    private final HasPermissions securedDomainType;
    private final String errorMessageOnDataFailureString;

    protected abstract void fetchData(AsyncCallback<HashMap<String, TimedLock>> callback);

    protected abstract void unlockIP(String ip, AsyncCallback<Void> asyncCallback);

    public IPBlocklistTableWrapper(final SailingServiceWriteAsync sailingServiceWrite, final UserService userService,
            final HasPermissions securedDomainType, final String errorMessageOnDataFailureString,
            final StringMessages stringMessages, final ErrorReporter errorReporter) {
        super(sailingServiceWrite, stringMessages, errorReporter, true, true,
                new EntityIdentityComparator<IpToTimedLockDTO>() {
                    @Override
                    public boolean representSameEntity(IpToTimedLockDTO dto1, IpToTimedLockDTO dto2) {
                        return dto1.ip.equals(dto2.ip);
                    }

                    @Override
                    public int hashCode(IpToTimedLockDTO t) {
                        return t.ip.hashCode();
                    }
                });
        this.securedDomainType = securedDomainType;
        this.userService = userService;
        this.errorMessageOnDataFailureString = errorMessageOnDataFailureString;
        this.filterField = composeFilterField();
        this.asWidget().ensureDebugId("wrappedTable");
        this.table.ensureDebugId("cellTable");
        configureDataColumns();
        setButtonsAndFilterOnMainPanel();
        loadDataAndPopulateTable();
    }

    private void setButtonsAndFilterOnMainPanel() {
        final HorizontalPanel searchPanel = new HorizontalPanel();
        searchPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        searchPanel.setSpacing(5);
        final Label label = new Label(getStringMessages().filterIpAddresses() + ": ");
        searchPanel.add(label);
        searchPanel.add(filterField.getTextBox());
        // inserted with indices to put them above the table
        mainPanel.insert(searchPanel, 0);
        mainPanel.insert(composeButtonPanel(), 1);
        mainPanel.setSpacing(5);
    }

    // admin, server admin and those with the permission can all unlock
    private boolean canUnlock() {
        final UserDTO user = userService.getCurrentUser();
        final Iterable<RoleWithSecurityDTO> roles = user.getRoles();
        boolean isAdmin = false;
        boolean isServerAdmin = false;
        boolean isDeleteActionPermittedOnDomain = false;
        for (RoleWithSecurityDTO role : roles) {
            isAdmin = role.getName().equals(AdminRole.getInstance().getName());
            if (isAdmin) {
                break;
            }
            isServerAdmin = role.getName().equals(ServerAdminRole.getInstance().getName());
            if (isServerAdmin) {
                break;
            }
        }
        final Iterable<WildcardPermission> permissions = user.getPermissions();
        for (WildcardPermission permission : permissions) {
            isDeleteActionPermittedOnDomain = permission.toString()
                    .equals(securedDomainType.getStringPermission(DefaultActions.DELETE));
            if (isDeleteActionPermittedOnDomain) {
                break;
            }
        }
        return isAdmin || isServerAdmin || isDeleteActionPermittedOnDomain;
    }

    private AccessControlledButtonPanel composeButtonPanel() {
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, securedDomainType);
        final Button refreshbutton = buttonPanel.addAction(getStringMessages().refresh(), () -> true, new Command() {
            @Override
            public void execute() {
                loadDataAndPopulateTable();
            }
        });
        refreshbutton.ensureDebugId("refreshButton");
        if (canUnlock()) {
            final Button unlockButton = new SelectedElementsCountingButton<IpToTimedLockDTO>(
                    getStringMessages().unlock(), getSelectionModel(), new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            for (IpToTimedLockDTO e : getSelectionModel().getSelectedSet()) {
                                unlockIP(e.ip, new AsyncCallback<Void>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError(errorMessageOnDataFailureString);
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        filterField.remove(e);
                                    }
                                });
                            }
                        }
                    });
            unlockButton.ensureDebugId("unlockButton");
            buttonPanel.insertWidgetAtPosition(unlockButton, 1);
        }
        return buttonPanel;
    }

    private void loadDataAndPopulateTable() {
        final AsyncCallback<HashMap<String, TimedLock>> dataInitializationCallback = new AsyncCallback<HashMap<String, TimedLock>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(errorMessageOnDataFailureString);
            }

            @Override
            public void onSuccess(HashMap<String, TimedLock> result) {
                filterField.clear();
                clear();
                final ArrayList<IpToTimedLockDTO> iterable = new ArrayList<IpToTimedLockDTO>();
                for (Entry<String, TimedLock> e : result.entrySet()) {
                    if (e.getValue().isLocked()) {
                        iterable.add(new IpToTimedLockDTO(e.getKey(), e.getValue()));
                    }
                }
                filterField.addAll(iterable);
            }
        };
        fetchData(dataInitializationCallback);
    }

    private void configureDataColumns() {
        final ListHandler<IpToTimedLockDTO> columnListHandler = getColumnSortHandler();
        addColumn(record -> record.ip, getStringMessages().ipAddress());
        final Comparator<IpToTimedLockDTO> expiryComparator = (o1, o2) -> {
            return o1.timedLock.getLockedUntil().compareTo(o2.timedLock.getLockedUntil());
        };
        addColumn(record -> record.timedLock.getLockedUntil().toString(), getStringMessages().lockedUntil(),
                expiryComparator);
        table.addColumnSortHandler(columnListHandler);
    }

    private LabeledAbstractFilterablePanel<IpToTimedLockDTO> composeFilterField() {
        final LabeledAbstractFilterablePanel<IpToTimedLockDTO> filterField = new LabeledAbstractFilterablePanel<IpToTimedLockDTO>(
                new Label(getStringMessages().filterIpAddresses()), new ArrayList<>(), getDataProvider(),
                getStringMessages()) {
            @Override
            public Iterable<String> getSearchableStrings(IpToTimedLockDTO dto) {
                final List<String> string = new ArrayList<String>();
                string.add(dto.ip);
                return string;
            }

            @Override
            public AbstractCellTable<IpToTimedLockDTO> getCellTable() {
                return table;
            }
        };
        registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        return filterField;
    }
}