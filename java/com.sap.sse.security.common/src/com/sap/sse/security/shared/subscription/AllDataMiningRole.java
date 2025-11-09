package com.sap.sse.security.shared.subscription;

import java.util.UUID;

import com.sap.sse.security.shared.RolePrototype;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

/**
 * Specifies a role that when associated to a user gives access to the date mining functionality on all servers.
 */
public class AllDataMiningRole extends RolePrototype {
    private static final UUID ROLE_ID = UUID.fromString("de4205b5-ccf9-49b2-91e1-9a41b4db166b");
    private static final AllDataMiningRole INSTANCE = new AllDataMiningRole();

    AllDataMiningRole() {
        super("all_data_mining", ROLE_ID.toString(),
                WildcardPermission.builder().withTypes(SecuredSecurityTypes.SERVER)
                        .withActions(SecuredSecurityTypes.ServerActions.DATA_MINING).build());
    }

    public static AllDataMiningRole getInstance() {
        return INSTANCE;
    }

    public static UUID getRoleId() {
        return ROLE_ID;
    }
}
