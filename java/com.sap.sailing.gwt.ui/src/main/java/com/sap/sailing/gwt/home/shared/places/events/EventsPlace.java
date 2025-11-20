package com.sap.sailing.gwt.home.shared.places.events;

import java.util.Optional;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.sap.sailing.gwt.home.shared.app.HasLocationTitle;
import com.sap.sailing.gwt.home.shared.app.HasMobileVersion;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class EventsPlace extends Place implements HasLocationTitle, HasMobileVersion {
    public String getTitle() {
        return (ClientConfiguration.getInstance().isBrandingActive()
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing()) + " - " + StringMessages.INSTANCE.events();
    }
    
    public static class Tokenizer implements PlaceTokenizer<EventsPlace> {
        @Override
        public String getToken(EventsPlace place) {
            return "";
        }

        @Override
        public EventsPlace getPlace(String token) {
            return new EventsPlace();
        }
    }

    @Override
    public String getLocationTitle() {
        return StringMessages.INSTANCE.events();
    }

}
