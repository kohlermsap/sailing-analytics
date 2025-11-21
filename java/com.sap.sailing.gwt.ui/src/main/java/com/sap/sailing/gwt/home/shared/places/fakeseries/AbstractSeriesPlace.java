package com.sap.sailing.gwt.home.shared.places.fakeseries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.gwt.place.shared.Place;
import com.sap.sailing.gwt.common.client.AbstractMapTokenizer;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.shared.ClientConfiguration;

public abstract class AbstractSeriesPlace extends Place {
    private final SeriesContext ctx;

    protected AbstractSeriesPlace(SeriesContext ctx) {
        this.ctx = ctx;
    }

    public SeriesContext getCtx() {
        return ctx;
    }

    public String getTitle(String eventName) {
        return (ClientConfiguration.getInstance().isBrandingActive() 
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing()) + " - " + eventName;
    }

    public String getSeriesUuidAsString() {
        return ctx.getSeriesId().toString();
    }
    
    public static abstract class Tokenizer<PLACE extends AbstractSeriesPlace> extends AbstractMapTokenizer<PLACE> {
        private final static String PARAM_EVENTID = "seriesId";
	private final static String PARAM_LEADERBOARD_GROUP_UUID = "leaderboardGroupId";
       
        protected PLACE getPlaceFromParameters(Map<String, Set<String>> parameters) {
            Set<String> leaderBoardSet = parameters.get(PARAM_LEADERBOARD_GROUP_UUID);
            SeriesContext ctx = null;
            if (leaderBoardSet != null) {
                String leaderboardGroupIdRaw = Util.first(leaderBoardSet);
                ctx = SeriesContext.createWithLeaderboardGroupId(UUID.fromString(leaderboardGroupIdRaw));
            } else {
                Set<String> eventIdSet = parameters.get(PARAM_EVENTID);
                if (eventIdSet != null) {
                    String eventIdRaw = Util.first(eventIdSet);
                    ctx = SeriesContext.createWithSeriesId(UUID.fromString(eventIdRaw));
                } else {
                    // trigger error handling by setting neither
                    ctx = SeriesContext.createErrorContext();
                }
            }
            return getRealPlace(ctx);
        }
        
        protected Map<String, Set<String>> getParameters(PLACE place) {
            Map<String, Set<String>> parameters = new HashMap<>();
            SeriesContext context = place.getCtx();
            if(context.getLeaderboardGroupId() != null) {
               Util.addToValueSet(parameters, PARAM_LEADERBOARD_GROUP_UUID, context.getLeaderboardGroupId().toString());
            }
            //fallback only generate old urls if not possible otherwise!
            if(context.getLeaderboardGroupId() == null && context.getSeriesId() != null) {
                Util.addToValueSet(parameters, PARAM_EVENTID, context.getSeriesId().toString());
            }
            return parameters;
        }
        
        protected abstract PLACE getRealPlace(SeriesContext context);
    }
}
