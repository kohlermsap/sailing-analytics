package com.sap.sailing.domain.base.impl;

import com.sap.sailing.domain.base.SailingServerConfiguration;

public class SailingServerConfigurationImpl implements SailingServerConfiguration {
    private static final long serialVersionUID = -639945748229124558L;

    /** indicates if the server is running in standalone mode or not  */
    private boolean isStandaloneServer;

    public SailingServerConfigurationImpl(boolean isStandaloneServer) {
        this.isStandaloneServer = isStandaloneServer;
    }

    @Override
    public boolean isStandaloneServer() {
        return isStandaloneServer;
    }

    @Override
    public void setStandaloneServer(boolean isStandaloneServer) {
        this.isStandaloneServer = isStandaloneServer;
    }
}
