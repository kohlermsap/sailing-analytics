package com.sap.sailing.gwt.home.shared.places.start;

import java.util.Optional;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.sap.sailing.gwt.home.shared.app.HasLocationTitle;
import com.sap.sailing.gwt.home.shared.app.HasMobileVersion;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class StartPlace extends Place implements HasLocationTitle, HasMobileVersion {
    public String getTitle() {
        return ClientConfiguration.getInstance().isBrandingActive() 
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing();
    }

    public static class Tokenizer implements PlaceTokenizer<StartPlace> {
        @Override
        public String getToken(StartPlace place) {
            return null;
        }

        @Override
        public StartPlace getPlace(String token) {
            return new StartPlace();
        }
    }

    @Override
    public String getLocationTitle() {
        return StringMessages.INSTANCE.headerLogo();
    }
}
