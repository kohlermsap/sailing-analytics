package com.sap.sailing.gwt.home.client.place.event.legacy;

import java.util.Optional;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.AbstractBasePlace;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class SeriesPlace extends AbstractBasePlace {
    private final String eventUuidAsString;
    private final String leaderboardIdAsNameString;
    private final Boolean showRaceDetails;
    private final Boolean showSettings;
    private final SeriesNavigationTabs navigationTab;
    
    private final static String PARAM_EVENTID = "eventId";
    private final static String PARAM_LEADERBOARD_NAME = "leaderboardName";
    private final static String PARAM_SHOW_RACE_DETAILS = "showRaceDetails";
    private final static String PARAM_SHOW_SETTINGS = "showSettings";
    private final static String PARAM_NAVIGATION_TAB = "navigationTab"; 

    public enum SeriesNavigationTabs { OverallLeaderboard, RegattaLeaderboards, CompetitorAnalytics };
    
    public SeriesPlace(String url) {
        super(url);
        eventUuidAsString = getParameter(PARAM_EVENTID);
        leaderboardIdAsNameString = getParameter(PARAM_LEADERBOARD_NAME);
        showRaceDetails = Boolean.valueOf(getParameter(PARAM_SHOW_RACE_DETAILS));
        showSettings = Boolean.valueOf(getParameter(PARAM_SHOW_SETTINGS));
        String paramNavTab = getParameter(PARAM_NAVIGATION_TAB);
        if(paramNavTab != null) {
            navigationTab = SeriesNavigationTabs.valueOf(paramNavTab);
        } else {
            navigationTab = SeriesNavigationTabs.OverallLeaderboard;
        }
    }

    public SeriesPlace(String eventUuidAsString, SeriesNavigationTabs navigationTab, String leaderboardIdAsNameString, Boolean showRaceDetails, Boolean showSettings) {
        super(Util.<String, String>mapBuilder()
                .put(PARAM_EVENTID, eventUuidAsString)
                .put(PARAM_NAVIGATION_TAB, navigationTab.name())
                .put(PARAM_LEADERBOARD_NAME, leaderboardIdAsNameString)
                .put(PARAM_SHOW_RACE_DETAILS, String.valueOf(showRaceDetails))
                .put(PARAM_SHOW_SETTINGS,  String.valueOf(showSettings))
                .build());
        this.eventUuidAsString = eventUuidAsString;
        this.navigationTab = navigationTab;
        this.leaderboardIdAsNameString = leaderboardIdAsNameString;
        this.showRaceDetails = showRaceDetails;
        this.showSettings = showSettings;
    }

    public String getTitle(String eventName, String leaderboardName) {
        return (ClientConfiguration.getInstance().isBrandingActive() 
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing()) + StringMessages.INSTANCE.leaderboard() + ": " + leaderboardName;
    }
    
    public String getEventUuidAsString() {
        return eventUuidAsString;
    }

    public SeriesNavigationTabs getNavigationTab() {
        return navigationTab;
    }

    public String getLeaderboardIdAsNameString() {
        return leaderboardIdAsNameString;
    }

    public Boolean getShowRaceDetails() {
        return showRaceDetails;
    }

    public Boolean getShowSettings() {
        return showSettings;
    }

    public static class Tokenizer implements PlaceTokenizer<SeriesPlace> {
        @Override
        public String getToken(SeriesPlace place) {
            return place.getParametersAsToken();
        }

        @Override
        public SeriesPlace getPlace(String url) {
            return new SeriesPlace(url);
        }
    }
}
