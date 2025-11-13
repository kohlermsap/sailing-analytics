package com.sap.sailing.server.security;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.security.SecuredDomainType.EventActions;
import com.sap.sse.security.shared.RolePrototype;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;

/**
 * Specifies a role that when associated to a user gives access to create objects on a server and to upload media for an
 * event; these are all permissions that an event manager will need as long as he/she doesn't need to change underlying
 * server infrastructure configuration.
 */
public class EventManagerRole extends RolePrototype {
    private static final EventManagerRole INSTANCE = new EventManagerRole();

    EventManagerRole() {
        super("event_manager", "3d1fc58f-5afc-4823-b71d-049ab2e8a83d",
                WildcardPermission.builder().withTypes(SecuredSecurityTypes.SERVER).withActions(ServerActions.CREATE_OBJECT).build(),
                WildcardPermission.builder().withTypes(SecuredDomainType.EVENT).withActions(EventActions.UPLOAD_MEDIA).build());
    }

    public static EventManagerRole getInstance() {
        return INSTANCE;
    }
}
