package com.sap.sse.security.shared.subscription;

import java.util.UUID;

import com.sap.sse.security.shared.RolePrototype;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

/**
 * Specifies a role that when associated to a user gives access to the date mining functionality on archive server.
 */
public class ArchiveDataMiningRole extends RolePrototype {
    private static final UUID ROLE_ID = UUID.fromString("f2993a7a-c08d-11ec-9d64-0242ac120002");
    private static final ArchiveDataMiningRole INSTANCE = new ArchiveDataMiningRole();

    ArchiveDataMiningRole() {
        super("archive_data_mining", ROLE_ID.toString(),
                WildcardPermission.builder().withTypes(SecuredSecurityTypes.SERVER)
                        .withActions(SecuredSecurityTypes.ServerActions.DATA_MINING).withIds("ARCHIVE").build());
    }

    public static ArchiveDataMiningRole getInstance() {
        return INSTANCE;
    }

    public static UUID getRoleId() {
        return ROLE_ID;
    }
}
