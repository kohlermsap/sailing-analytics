package com.sap.sailing.domain.common.subscription;

import java.util.UUID;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.security.shared.RolePrototype;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

/**
 * Specifies a role that when associated to a user gives access to several premium features.
 */
public class PremiumRole extends RolePrototype {
    private static final UUID ROLE_ID = UUID.fromString("7021e7a2-569a-11ec-bf63-0242ac130002");
    private static final PremiumRole INSTANCE = new PremiumRole();

    PremiumRole() {
        super("premium", ROLE_ID.toString(),
                WildcardPermission.builder().withTypes(SecuredDomainType.TRACKED_RACE)
                        .withActions(SecuredDomainType.TrackedRaceActions.VIEWSTREAMLETS).build(),
                WildcardPermission.builder().withTypes(SecuredDomainType.TRACKED_RACE)
                        .withActions(SecuredDomainType.TrackedRaceActions.SIMULATOR).build(),
                WildcardPermission.builder().withTypes(SecuredDomainType.SIMULATOR)
                        .withActions(DefaultActions.READ).build(),
                WildcardPermission.builder().withTypes(SecuredSecurityTypes.USER)
                        .withActions(SecuredSecurityTypes.UserActions.BE_PREMIUM).build(),
                WildcardPermission.builder().withTypes(SecuredDomainType.TRACKED_RACE)
                        .withActions(SecuredDomainType.TrackedRaceActions.VIEWANALYSISCHARTS).build(),
                WildcardPermission.builder().withTypes(SecuredDomainType.TRACKED_RACE)
                        .withActions(SecuredDomainType.TrackedRaceActions.COLORED_TAILS).build(),
                WildcardPermission.builder().withTypes(SecuredDomainType.LEADERBOARD)
                        .withActions(SecuredDomainType.LeaderboardActions.PREMIUM_LEADERBOARD_INFORMATION).build());
    }

    public static PremiumRole getInstance() {
        return INSTANCE;
    }

    public static UUID getRoleId() {
        return ROLE_ID;
    }
}
