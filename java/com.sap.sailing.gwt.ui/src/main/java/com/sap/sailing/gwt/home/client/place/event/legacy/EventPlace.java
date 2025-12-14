package com.sap.sailing.gwt.home.client.place.event.legacy;

import java.util.Optional;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.sap.sse.common.Util;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.AbstractBasePlace;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class EventPlace extends AbstractBasePlace {
    private final String eventUuidAsString;
    private final String leaderboardIdAsNameString;
    private final EventNavigationTabs navigationTab;
    
    private final static String PARAM_EVENTID = "eventId"; 
    private final static String PARAM_LEADEROARD_NAME = "leaderboardName"; 
    private final static String PARAM_NAVIGATION_TAB = "navigationTab"; 

    public enum EventNavigationTabs { Overview, Regattas, Regatta, Media, Schedule };
    
    public EventPlace(String url) {
        super(url);
        eventUuidAsString = getParameter(PARAM_EVENTID);
        leaderboardIdAsNameString = getParameter(PARAM_LEADEROARD_NAME);
        String paramNavTab = getParameter(PARAM_NAVIGATION_TAB);
        if(paramNavTab != null) {
            navigationTab = EventNavigationTabs.valueOf(paramNavTab);
        } else {
            navigationTab = EventNavigationTabs.Regattas;
        }
    }

    public EventPlace(String eventUuidAsString, EventNavigationTabs navigationTab, String leaderboardIdAsNameString) {
        super(Util.<String, String>mapBuilder()
                .put(PARAM_EVENTID, eventUuidAsString)
                .put(PARAM_NAVIGATION_TAB, navigationTab.name())
                .put(PARAM_LEADEROARD_NAME, leaderboardIdAsNameString)
                .build());
        this.eventUuidAsString = eventUuidAsString;
        this.navigationTab = navigationTab;
        this.leaderboardIdAsNameString = leaderboardIdAsNameString;
    }

    public String getTitle(String eventName) {
        return (ClientConfiguration.getInstance().isBrandingActive()
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing()) + " - " + eventName;
    }
    
    public String getEventUuidAsString() {
        return eventUuidAsString;
    }

    public String getLeaderboardIdAsNameString() {
        return leaderboardIdAsNameString;
    }

    public EventNavigationTabs getNavigationTab() {
        return navigationTab;
    }

    public static class Tokenizer implements PlaceTokenizer<EventPlace> {
        @Override
        public String getToken(EventPlace place) {
            return place.getParametersAsToken();
        }

        @Override
        public EventPlace getPlace(String url) {
            return new EventPlace(url);
        }
    }
}
