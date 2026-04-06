package com.sap.sailing.server.gateway.jaxrs.api;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneUUIDIdentifierImpl;
import com.sap.sailing.server.gateway.deserialization.impl.FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

@Path("/v1/gps_fixes")
public class GPSFixesResource extends AbstractSailingServerResource {
    private static final Logger logger = Logger.getLogger(GPSFixesResource.class.getName());
    private final JsonDeserializer<Pair<UUID, List<GPSFixMoving>>> deserializer =
            new FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json;charset=UTF-8")
    public Response postFixes(String json, @QueryParam("returnManeuverUpdate") @DefaultValue("false") Boolean returnManeuverUpdate,
            @QueryParam("returnLiveDelay") @DefaultValue("false") Boolean returnLiveDelay) {
        final TimePoint received = MillisecondsTimePoint.now();
        Pair<UUID, List<GPSFixMoving>> data = null;
        try {
            logger.fine("Post issued to " + this.getClass().getName());
            Object requestBody = JSONValue.parseWithException(json);
            JSONObject requestObject = Helpers.toJSONObjectSafe(requestBody);
            logger.fine("JSON requestObject is: " + requestObject.toString());
            data = deserializer.deserialize(requestObject);
        } catch (ParseException | JsonDeserializationException e) {
            logger.warning(String.format("Exception while parsing post request:\n%s", e.toString()));
            return Response.status(Status.BAD_REQUEST).entity("Invalid JSON body in request").type(MediaType.TEXT_PLAIN).build();
        }
        DeviceIdentifier device = new SmartphoneUUIDIdentifierImpl(data.getA());
        List<GPSFixMoving> fixes = data.getB();
        JSONObject answer = new JSONObject();
        try {
            Iterable<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> racesWithManeuverChangedAndLiveDelay = getService().getSensorFixStore().storeFixes(device, fixes, returnManeuverUpdate, returnLiveDelay);
            if (!Util.isEmpty(racesWithManeuverChangedAndLiveDelay)) {
                JSONArray changed = new JSONArray();
                answer.put("maneuverchanged", changed);
                for (Triple<RegattaAndRaceIdentifier, Boolean, Duration> raceWithManeuverChangedAndLiveDelay : racesWithManeuverChangedAndLiveDelay) {
                    JSONObject singleRaceRegatta = new JSONObject();
                    singleRaceRegatta.put("regattaName", raceWithManeuverChangedAndLiveDelay.getA().getRegattaName());
                    singleRaceRegatta.put("raceName", raceWithManeuverChangedAndLiveDelay.getA().getRaceName());
                    singleRaceRegatta.put("maneuverChanged", raceWithManeuverChangedAndLiveDelay.getB());
                    singleRaceRegatta.put("liveDelayInMillis", raceWithManeuverChangedAndLiveDelay.getC().asMillis());
                    changed.add(singleRaceRegatta);
                }
            }
            logger.log(Level.INFO, "Added " + fixes.size() + " fixes for device " + device.toString()  + " to store."+
                    (fixes.isEmpty()?"":" Delay: "+fixes.iterator().next().getTimePoint().until(received)));
        } catch (NoCorrespondingServiceRegisteredException e) {
            logger.log(Level.WARNING, "Could not store fix for device " + device);
        }
        return Response.ok(streamingOutput(answer)).build();
    }
}