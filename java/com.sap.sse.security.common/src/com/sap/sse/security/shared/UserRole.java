package com.sap.sse.security.shared;

import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

public class UserRole extends RolePrototype {
    private static final UserRole INSTANCE = new UserRole();

    UserRole() {
        super("user", "ad1d5148-b13d-4464-90c4-7c396e4d4e2e",
                WildcardPermission.builder()
                        .withActions(DefaultActions.plus(SecuredSecurityTypes.PublicReadableActions.READ_PUBLIC))
                        .build());
    }

    public static UserRole getInstance() {
        return INSTANCE;
    }
}
