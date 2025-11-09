package com.sap.sse.security.shared;

import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

/**
 * A role prototype for role {@code server_admin} which grants all {@link SecuredSecurityTypes#SERVER} permissions.
 * This is what a user will need in order to fully configure a server or replica set environment. It includes permissions
 * to export and replica data as well as to determine a server's local configuration, manage its remote server
 * references, and use the data mining functionality.
 * 
 * @see SecuredSecurityTypes.ServerActions
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ServerAdminRole extends RolePrototype {
    private static final ServerAdminRole INSTANCE = new ServerAdminRole();

    ServerAdminRole() {
        super("server_admin", "dbf26f1d-15bd-4aff-a91a-c415f5b85a35",
                WildcardPermission.builder().withTypes(SecuredSecurityTypes.SERVER).build());
    }

    public static ServerAdminRole getInstance() {
        return INSTANCE;
    }
}
