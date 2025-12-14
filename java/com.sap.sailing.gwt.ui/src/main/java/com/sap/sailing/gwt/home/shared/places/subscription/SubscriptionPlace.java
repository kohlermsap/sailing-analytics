package com.sap.sailing.gwt.home.shared.places.subscription;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.Prefix;
import com.sap.sailing.gwt.common.client.AbstractMapTokenizer;
import com.sap.sailing.gwt.common.client.navigation.PlaceTokenPrefixes;
import com.sap.sailing.gwt.home.shared.app.HasLocationTitle;
import com.sap.sailing.gwt.home.shared.app.HasMobileVersion;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class SubscriptionPlace extends Place implements HasLocationTitle, HasMobileVersion {

    private final Set<String> plansToHighlight;

    public SubscriptionPlace() {
        this(Collections.emptySet());
    }

    public SubscriptionPlace(final Set<String> plansToHighlight) {
        super();
        this.plansToHighlight = plansToHighlight;
    }

    public Set<String> getPlansToHighlight() {
        return plansToHighlight;
    }

    @Override
    public String getLocationTitle() {
        return StringMessages.INSTANCE.subscription();
    }

    public String getTitle() {
        return (ClientConfiguration.getInstance().isBrandingActive() 
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing()) + " - " + StringMessages.INSTANCE.subscription();
    }

    @Prefix(PlaceTokenPrefixes.Subscription)
    public static class Tokenizer extends AbstractMapTokenizer<SubscriptionPlace> {

        private static final String PARAM_MAP_KEY = "highlight";

        @Override
        protected SubscriptionPlace getPlaceFromParameters(final Map<String, Set<String>> parameters) {
            return new SubscriptionPlace(parameters.getOrDefault(PARAM_MAP_KEY, Collections.emptySet()));
        }

        @Override
        protected Map<String, Set<String>> getParameters(final SubscriptionPlace place) {
            return Collections.singletonMap(PARAM_MAP_KEY, place.getPlansToHighlight());
        }

    }
}
