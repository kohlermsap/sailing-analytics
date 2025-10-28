package com.sap.sse.gwt.client.useragent;

import java.util.HashMap;

import com.sap.sse.gwt.client.useragent.UserAgentDetails.AgentTypes;

public class UserAgentCheckerImpl implements UserAgentChecker {

    /**
     * Version numbers indicate minimum required browser (20 = at least this version)
     */
    private static final HashMap<AgentTypes, Integer> MINIMUM_SUPPORTED_AGENTS = new HashMap<AgentTypes, Integer>() {
        private static final long serialVersionUID = -3972648919899828625L;
        {
            put(AgentTypes.MSIE, 9);
            put(AgentTypes.SAFARI, 5);
            put(AgentTypes.OPERA, 10);
            put(AgentTypes.FIREFOX, 10);
            put(AgentTypes.CHROME, 20);
        }
    };

    @Override
    public boolean isUserAgentSupported(UserAgentDetails details) {
        if (MINIMUM_SUPPORTED_AGENTS.containsKey(details.getType())) {
            if (details.getVersion()[0] < MINIMUM_SUPPORTED_AGENTS.get(details.getType())) {
                return false;
            }
        }
        /* returning true for entries not listed */
        return true;
    }

}
