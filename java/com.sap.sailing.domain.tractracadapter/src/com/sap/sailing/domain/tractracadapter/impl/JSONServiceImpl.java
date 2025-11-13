package com.sap.sailing.domain.tractracadapter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sap.sailing.domain.tractracadapter.JSONService;
import com.sap.sailing.domain.tractracadapter.RaceRecord;
import com.sap.sse.common.Util;
import com.sap.sse.util.HttpUrlConnectionHelper;

public class JSONServiceImpl implements JSONService {
    private final String regattaName;
    private final List<RaceRecord> raceRecords;
    
    public JSONServiceImpl(URL jsonURL, boolean loadLiveAndStoredURI, String tracTracApiToken) throws IOException, ParseException, org.json.simple.parser.ParseException, URISyntaxException {
        this(jsonURL, /* race ID == null means load all race records */ null, loadLiveAndStoredURI, tracTracApiToken);
    }

    private RaceRecord createRaceRecord(URL jsonURL, boolean loadLiveAndStoredURI, JSONObject jsonRaceEntry, String defaultUpdateURI, String tracTracApiToken)
            throws URISyntaxException, IOException {
        RaceRecord raceRecord = new RaceRecord(jsonURL, regattaName,
                (String) jsonRaceEntry.get("name"), (String) jsonRaceEntry.get("url_html"),
                (String) jsonRaceEntry.get("params_url"),
                (String) jsonRaceEntry.get("id"),
                (String) jsonRaceEntry.get("tracking_starttime"),
                (String) jsonRaceEntry.get("tracking_endtime"),
                (String) jsonRaceEntry.get("race_starttime"),
                (String) jsonRaceEntry.get("classes"),
                (String) jsonRaceEntry.get("status"), 
                (String) jsonRaceEntry.get("visibility"), 
                Boolean.valueOf((Boolean) jsonRaceEntry.get("has_replay")),
                /*loadLiveAndStoreURI*/ loadLiveAndStoredURI, defaultUpdateURI, tracTracApiToken);
        return raceRecord;
    }
    
    /**
     * @param raceEntryId
     *            if {@code null}, add all races found to the {@link #raceRecords}; otherwise, add only the race whose
     *            ID matches
     */
    public JSONServiceImpl(URL jsonURL, String raceEntryId, boolean loadLiveAndStoredURI, String tracTracApiToken) throws IOException, ParseException, org.json.simple.parser.ParseException, URISyntaxException {
        final URLConnection connection = jsonURL.openConnection();
        if (Util.hasLength(tracTracApiToken)) {
            connection.setRequestProperty("Authorization", "Bearer "+tracTracApiToken);
        }
        final Charset charset = HttpUrlConnectionHelper.getCharsetFromConnectionOrDefault(connection, "UTF-8");
        JSONObject jsonObject = parseJSONObject(connection.getInputStream(), charset);
        raceRecords = new ArrayList<RaceRecord>();
        regattaName = (String) ((JSONObject) jsonObject.get("event")).get("name");
        final String defaultUpdateURI = (String) ((JSONObject) jsonObject.get("event")).get("server_update_uri");
        for (Object raceEntry : (JSONArray) jsonObject.get("races")) {
            JSONObject jsonRaceEntry = (JSONObject) raceEntry;
            if (raceEntryId == null || jsonRaceEntry.get("id").equals(raceEntryId)) {
                RaceRecord raceRecord = createRaceRecord(jsonURL, loadLiveAndStoredURI, jsonRaceEntry, defaultUpdateURI, tracTracApiToken);
                raceRecords.add(raceRecord);
            }
        }
    }

    @Override
    public String getEventName() {
        return regattaName;
    }
    
    @Override
    public List<RaceRecord> getRaceRecords() {
        return Collections.unmodifiableList(raceRecords);
    }

    private JSONObject parseJSONObject(InputStream is, Charset charset) throws IOException, ParseException, org.json.simple.parser.ParseException {
        JSONParser parser = new JSONParser();
        Object result = parser.parse(new InputStreamReader(is));
        return (JSONObject) result;
    }
}
