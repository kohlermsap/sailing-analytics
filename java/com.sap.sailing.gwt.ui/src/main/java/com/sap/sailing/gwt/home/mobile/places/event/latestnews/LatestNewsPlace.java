package com.sap.sailing.gwt.home.mobile.places.event.latestnews;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.google.gwt.place.shared.Prefix;
import com.sap.sailing.gwt.common.client.navigation.PlaceTokenPrefixes;
import com.sap.sailing.gwt.home.communication.event.news.NewsEntryDTO;
import com.sap.sailing.gwt.home.shared.app.HasMobileVersion;
import com.sap.sailing.gwt.home.shared.places.event.AbstractEventPlace;
import com.sap.sailing.gwt.home.shared.places.event.EventContext;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class LatestNewsPlace extends AbstractEventPlace implements HasMobileVersion {
    private List<NewsEntryDTO> news = new LinkedList<NewsEntryDTO>();

    public LatestNewsPlace(String eventUuidAsString) {
        super(eventUuidAsString);
    }

    public LatestNewsPlace(EventContext ctx) {
        super(ctx);
    }

    public LatestNewsPlace(EventContext ctx, Collection<NewsEntryDTO> newsEntries) {
        super(ctx);
        news.addAll(newsEntries);
    }

    public List<NewsEntryDTO> getNews() {
        return news;
    }

    public String getTitle() {
        return (ClientConfiguration.getInstance().isBrandingActive() 
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing());
    }

    @Override
    public String getLocationTitle() {
        return StringMessages.INSTANCE.headerLogo();
    }

    @Prefix(PlaceTokenPrefixes.EventNews)
    public static class Tokenizer extends AbstractEventPlace.Tokenizer<LatestNewsPlace> {
        @Override
        protected LatestNewsPlace getRealPlace(EventContext context) {
            return new LatestNewsPlace(context);
        }
    }
}
