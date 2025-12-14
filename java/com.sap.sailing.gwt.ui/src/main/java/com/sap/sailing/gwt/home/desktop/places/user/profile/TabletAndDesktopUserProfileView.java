package com.sap.sailing.gwt.home.desktop.places.user.profile;

import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabPanel;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabPanelPlaceSelectionEvent;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabView;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.places.user.profile.preferencestab.UserProfilePreferencesTabView;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.SailorProfileTabView;
import com.sap.sailing.gwt.home.desktop.places.user.profile.subscriptionstab.UserProfileSubscriptionsTabView;
import com.sap.sailing.gwt.home.shared.app.ApplicationHistoryMapper;
import com.sap.sailing.gwt.home.shared.places.user.profile.AbstractUserProfilePlace;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;
import com.sap.sse.security.ui.authentication.app.AuthenticationContext;
import com.sap.sse.security.ui.userprofile.desktop.userheader.UserHeader;

public class TabletAndDesktopUserProfileView extends Composite
        implements UserProfileView<AbstractUserProfilePlace, UserProfileView.Presenter> {
    private static final ApplicationHistoryMapper historyMapper = GWT
            .<ApplicationHistoryMapper> create(ApplicationHistoryMapper.class);

    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    private UserProfileView.Presenter currentPresenter;

    interface MyBinder extends UiBinder<Widget, TabletAndDesktopUserProfileView> {
    }

    @UiField
    StringMessages i18n;

    @UiField(provided = true)
    TabPanel<AbstractUserProfilePlace, UserProfileView.Presenter, UserProfileTabView<AbstractUserProfilePlace>> tabPanelUi;

    @UiField(provided = true)
    UserHeader headerUi;

    @UiField(provided = true)
    UserProfilePreferencesTabView preferencesTabUi;

    @UiField(provided = true)
    SailorProfileTabView sailorProfileTabUi;
    
    @UiField(provided = true)
    UserProfileSubscriptionsTabView subscriptionTabUi;

    private final DesktopPlacesNavigator homePlacesNavigator;
    private final FlagImageResolver flagImageResolver;

    public TabletAndDesktopUserProfileView(final DesktopPlacesNavigator homePlacesNavigator, final FlagImageResolver flagImageResolver) {
        this.homePlacesNavigator = homePlacesNavigator;
        this.flagImageResolver = flagImageResolver;
        UserProfileResources.INSTANCE.css().ensureInjected();
    }

    @Override
    public void registerPresenter(final UserProfileView.Presenter currentPresenter) {
        this.currentPresenter = currentPresenter;
        tabPanelUi = new TabPanel<>(currentPresenter, historyMapper);

        headerUi = new UserHeader(SharedResources.INSTANCE);

        preferencesTabUi = new UserProfilePreferencesTabView(flagImageResolver);

        sailorProfileTabUi = new SailorProfileTabView(flagImageResolver);
        
        subscriptionTabUi = new UserProfileSubscriptionsTabView(homePlacesNavigator);

        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void navigateTabsTo(final AbstractUserProfilePlace place) {
        tabPanelUi.activatePlace(place);
        final StringBuilder titleBuilder = new StringBuilder(ClientConfiguration.getInstance().isBrandingActive() 
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing()).append(" - ");

        titleBuilder.append(place.getLocationTitle());

        final String currentTabTitle = tabPanelUi.getCurrentTabTitle();
        if (currentTabTitle != null && !currentTabTitle.isEmpty()) {
            titleBuilder.append(" - ").append(currentTabTitle);
        }
        Window.setTitle(titleBuilder.toString());
    }

    @Override
    public void setAuthenticationContext(final AuthenticationContext authenticationContext) {
        headerUi.setAuthenticationContext(authenticationContext);
        tabPanelUi.getCurrentTab().setAuthenticationContext(authenticationContext);
    }

    @SuppressWarnings("unchecked")
    @UiHandler("tabPanelUi")
    public void onTabSelection(final TabPanelPlaceSelectionEvent e) {
        currentPresenter.handleTabPlaceSelection((TabView<?, UserProfileView.Presenter>) e.getSelectedActivity());
    }

    @Override
    public void showErrorInCurrentTab(final IsWidget errorView) {
        tabPanelUi.overrideCurrentContentInTab(errorView);
    }
}
