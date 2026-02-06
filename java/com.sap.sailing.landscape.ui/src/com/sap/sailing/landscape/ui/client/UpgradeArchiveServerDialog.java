package com.sap.sailing.landscape.ui.client;

import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;

public class UpgradeArchiveServerDialog extends AbstractApplicationReplicaSetDialog<UpgradeArchiveServerDialog.UpgradeArchiveServerInstructions> {
    
    public static class UpgradeArchiveServerInstructions extends AbstractApplicationReplicaSetDialog.AbstractApplicationReplicaSetInstructions {
        private final String optionalInstanceType;
        public UpgradeArchiveServerInstructions(String releaseNameOrNullForLatestMaster, String masterReplicationBearerToken, String replicaReplicationBearerToken, String optionalInstanceType) {
            super(releaseNameOrNullForLatestMaster, masterReplicationBearerToken, replicaReplicationBearerToken);
            this.optionalInstanceType = optionalInstanceType;
        }
        public String getOptionalInstanceType() {
            return optionalInstanceType;
        }
    }
    
    private final StringMessages stringMessages;
    private final ListBox sharedInstanceTypeListBox;

    public UpgradeArchiveServerDialog(LandscapeManagementWriteServiceAsync landscapeManagementService, Iterable<String> releaseNames,
            StringMessages stringMessages, ErrorReporter errorReporter, DialogCallback<UpgradeArchiveServerInstructions> callback) {
        super(stringMessages.upgradeArchiveServer(), landscapeManagementService, releaseNames, stringMessages, errorReporter, /* validator */ null, callback);
        this.stringMessages = stringMessages;
        sharedInstanceTypeListBox = LandscapeDialogUtil.createInstanceTypeListBox(this, landscapeManagementService,
                stringMessages, SharedLandscapeConstants.DEFAULT_SHARED_INSTANCE_TYPE_NAME, errorReporter, /* canBeDeployedInNlbInstanceBasedTargetGroup */ false);
        
    }
    
    protected ListBox getSharedInstanceTypeListBox() {
        return sharedInstanceTypeListBox;
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        final Grid result = new Grid(4, 2);
        int row=0;
        result.setWidget(row, 0, new Label(stringMessages.release()));
        result.setWidget(row++, 1, getReleaseNameBox());
        result.setWidget(row, 0, new Label(stringMessages.instanceType()));
        result.setWidget(row++, 1, getSharedInstanceTypeListBox());
        result.setWidget(row, 0, new Label(stringMessages.bearerTokenForSecurityReplication()));
        result.setWidget(row++, 1, getMasterReplicationBearerTokenBox());
        result.setWidget(row, 0, new Label(stringMessages.bearerTokenOrNullForArchive()));
        result.setWidget(row++, 1, getReplicaReplicationBearerTokenBox());
        return result;
    }

    @Override
    public FocusWidget getInitialFocusWidget() {
        return getReleaseNameBox().getValueBox();
    }
    
    @Override
    protected UpgradeArchiveServerInstructions getResult() {
        return new UpgradeArchiveServerInstructions(getReleaseNameBoxValue(), getMasterReplicationBearerTokenBox().getValue(),
                getReplicaReplicationBearerTokenBox().getValue(), getSharedInstanceTypeListBox().getSelectedValue()); 
    }
}
