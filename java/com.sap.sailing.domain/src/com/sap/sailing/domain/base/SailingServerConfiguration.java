package com.sap.sailing.domain.base;

import java.io.Serializable;

/**
 * Represents the configuration of a (local) instance of a sailing server.
 */
public interface SailingServerConfiguration extends Serializable {
    boolean isStandaloneServer();

    void setStandaloneServer(boolean isStandaloneServer);
}
