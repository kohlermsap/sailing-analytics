package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.bson.BsonMaximumSizeExceededException;
import com.mongodb.client.MongoClient;import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.impl.ManeuverCurveBoundariesImpl;
import com.sap.sailing.domain.tracking.impl.ManeuverWithMainCurveBoundariesImpl;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.mongodb.MongoDBConfiguration;

/**
 * Regression test for bug 6226: BsonMaximumSizeExceededException when storing maneuvers for
 * large races with many competitors.
 *
 * The old implementation stored all competitors' maneuvers in a single MongoDB document, which
 * exceeded the 16MB BSON limit for races with ~163 competitors. The fix stores one document per
 * competitor instead.
 *
 * This test uses a synthetic dataset (200 competitors x 500 maneuvers each) to trigger the
 * original issue and verify that the new implementation handles it correctly.
 */
public class ManeuverStorageLimitTest {
    private static final int COMPETITOR_COUNT = 200;
    private static final int MANEUVERS_PER_COMPETITOR = 500;

    private MongoDBConfiguration dbConfiguration;
    private MongoDatabase database;

    @BeforeEach
    public void setUp() {
        dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
        final MongoClient mongoClient = MongoClients.create(dbConfiguration.getMongoClientURI());
        database = mongoClient.getDatabase(dbConfiguration.getDatabaseName());
        database.drop();
    }

    /**
     * Verifies that the new per-competitor document storage does not exceed MongoDB's 16MB limit
     * even for a large race (200 competitors, 500 maneuvers each).
     * Verifies document count equals competitor count after storing.
     */
    @Test
    public void testNewImplementationStoresOneDocumentPerCompetitor() {
        final RegattaNameAndRaceName raceIdentifier = new RegattaNameAndRaceName("505 Pre-Worlds 2014 synthetic", "Race 1");
        final Map<Competitor, List<Maneuver>> maneuvers = buildSyntheticManeuvers(COMPETITOR_COUNT, MANEUVERS_PER_COMPETITOR);
        final ManeuverRaceFingerprint fingerprint = buildMockFingerprint();
        final Course course = mock(Course.class);
        new MongoObjectFactoryImpl(database).storeManeuvers(raceIdentifier, fingerprint, course, maneuvers);
        final MongoCollection<Document> collection = database.getCollection("MANEUVERS");
        final long documentCount = collection.countDocuments();
        assertEquals(COMPETITOR_COUNT, documentCount,
                "Expected one document per competitor in the MANEUVERS collection");
    }

    /**
     * Demonstrates that the old single-document implementation fails with MongoWriteException
     * (BsonMaximumSizeExceededException) for the same large dataset.
     *
     * The old implementation called replaceOne with a single document containing all competitors,
     * which exceeds MongoDB's 16MB BSON limit for COMPETITOR_COUNT=200, MANEUVERS_PER_COMPETITOR=500,
     * causing a BsonMaximumSizeExceededException.
     */
    @Test
    public void testOldImplementationFailsWithBsonLimitExceeded() {
        final RegattaNameAndRaceName raceIdentifier = new RegattaNameAndRaceName("505 Pre-Worlds 2014 synthetic", "Race 1");
        final Map<Competitor, List<Maneuver>> maneuvers = buildSyntheticManeuvers(COMPETITOR_COUNT, MANEUVERS_PER_COMPETITOR);
        final ManeuverRaceFingerprint fingerprint = buildMockFingerprint();
        final Course course = mock(Course.class);
        assertThrows(BsonMaximumSizeExceededException.class, () ->
            new OldMongoObjectFactoryForBug6226Test(database).storeManeuvers(raceIdentifier, fingerprint, course, maneuvers),
            "Old implementation must throw BsonMaximumSizeExceededException due to BSON 16MB limit"
        );
    }

    private ManeuverRaceFingerprint buildMockFingerprint() {
        final ManeuverRaceFingerprint fingerprint = mock(ManeuverRaceFingerprint.class);
        final JSONObject fingerprintJson = new JSONObject();
        fingerprintJson.put("DUMMY", "fingerprint");
        when(fingerprint.toJson()).thenReturn(fingerprintJson);
        return fingerprint;
    }

    private Map<Competitor, List<Maneuver>> buildSyntheticManeuvers(final int competitorCount, final int maneuversPerCompetitor) {
        final Map<Competitor, List<Maneuver>> result = new HashMap<>();
        for (int i = 0; i < competitorCount; i++) {
            final Competitor competitor = mock(CompetitorImpl.class);
            when(competitor.getId()).thenReturn("competitor-" + i);
            result.put(competitor, buildManeuverList(maneuversPerCompetitor));
        }
        return result;
    }

    private List<Maneuver> buildManeuverList(final int count) {
        final List<Maneuver> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(buildManeuver(i));
        }
        return result;
    }

    private Maneuver buildManeuver(final int index) {
        final long baseMillis = 1_400_000_000_000L + index * 30_000L;
        final ManeuverCurveBoundaries curveBoundaries = new ManeuverCurveBoundariesImpl(
                new MillisecondsTimePoint(baseMillis),
                new MillisecondsTimePoint(baseMillis + 15_000L),
                new KnotSpeedWithBearingImpl(6.0, new DegreeBearingImpl(45.0)),
                new KnotSpeedWithBearingImpl(5.5, new DegreeBearingImpl(315.0)),
                90.0,
                new KnotSpeedImpl(4.0),
                new KnotSpeedImpl(6.5));
        return new ManeuverWithMainCurveBoundariesImpl(
                ManeuverType.TACK,
                Tack.STARBOARD,
                new DegreePosition(47.5 + index * 0.001, 9.0 + index * 0.001),
                new MillisecondsTimePoint(baseMillis + 7_500L),
                curveBoundaries,
                curveBoundaries,
                5.0,
                null,
                null);
    }
}
