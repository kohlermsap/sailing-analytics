package com.sap.sse.security.shared;

public class AdminRole extends RolePrototype {
    
    private static final AdminRole INSTANCE = new AdminRole();
    private static final String UUID_STRING = "dc77e3d1-d405-435e-8699-ce7245f6fd7a";
    
    AdminRole() {
        super("admin", UUID_STRING, new WildcardPermission(WildcardPermission.WILDCARD_TOKEN));
    }
    
    public static AdminRole getInstance() {
        return INSTANCE;
    }
}
