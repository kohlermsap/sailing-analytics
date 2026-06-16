package com.sap.sailing.mongodb.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.persistence.FieldNames;
import com.sap.sailing.domain.persistence.impl.CollectionNames;
import com.sap.sailing.domain.tracking.Maneuver;

/**
 * Reproduces the pre-bug-6226-fix behaviour of {@code MongoObjectFactoryImpl.storeManeuvers()}:
 * all competitors' maneuvers were packed into a single MongoDB document, keyed only by
 * (EVENT_NAME, RACE_NAME). For races with many competitors this document easily exceeds MongoDB's
 * 16 MB BSON limit, causing a {@code MongoWriteException}.
 *
 * This class exists solely to let {@link ManeuverStorageLimitTest} assert that the old code path
 * fails with the expected exception. It must never be used in production.
 */
class OldMongoObjectFactoryForBug6226Test {
    private final MongoDatabase database;

    OldMongoObjectFactoryForBug6226Test(final MongoDatabase database) {
        this.database = database;
    }

    void storeManeuvers(final RaceIdentifier raceIdentifier, final ManeuverRaceFingerprint fingerprint,
            final Course course, final Map<Competitor, List<Maneuver>> maneuvers) {
        final MongoCollection<Document> maneuverCollection = database.getCollection(CollectionNames.MANEUVERS.name());
        final Document query = new Document();
        query.put(FieldNames.EVENT_NAME.name(), raceIdentifier.getRegattaName());
        query.put(FieldNames.RACE_NAME.name(), raceIdentifier.getRaceName());
        final Document result = new Document();
        final Document fingerprintDoc = Document.parse(fingerprint.toJson().toString());
        result.put(FieldNames.MANEUVER_FINGERPRINT.name(), fingerprintDoc);
        result.put(FieldNames.EVENT_NAME.name(), raceIdentifier.getRegattaName());
        result.put(FieldNames.RACE_NAME.name(), raceIdentifier.getRaceName());
        final List<Document> allCompetitorDocs = buildAllCompetitorManeuverDocs(maneuvers);
        result.put(FieldNames.MANEUVERS.name(), allCompetitorDocs);
        maneuverCollection.replaceOne(query, result, new ReplaceOptions().upsert(true));
    }

    private List<Document> buildAllCompetitorManeuverDocs(final Map<Competitor, List<Maneuver>> maneuvers) {
        final List<Document> result = new ArrayList<>();
        for (final Entry<Competitor, List<Maneuver>> entry : maneuvers.entrySet()) {
            final Document competitorDoc = new Document();
            competitorDoc.put(FieldNames.COMPETITOR_ID.name(), entry.getKey().getId());
            final List<Document> maneuverList = new ArrayList<>();
            if (entry.getValue() != null) {
                for (final Maneuver maneuver : entry.getValue()) {
                    maneuverList.add(buildManeuverDoc(maneuver));
                }
            }
            competitorDoc.put(FieldNames.MANEUVERS.name(), maneuverList);
            result.add(competitorDoc);
        }
        return result;
    }

    private Document buildManeuverDoc(final Maneuver maneuver) {
        final Document doc = new Document();
        doc.put(FieldNames.SIMPLE_CLASS_NAME.name(), maneuver.getClass().getSimpleName());
        doc.put(FieldNames.TYPE.name(), maneuver.getType().name());
        doc.put(FieldNames.TACK.name(), maneuver.getNewTack() == null ? null : maneuver.getNewTack().name());
        doc.put(FieldNames.POSITION_LAT_RAD.name(), maneuver.getPosition().getLatRad());
        doc.put(FieldNames.POSITION_LNG_RAD.name(), maneuver.getPosition().getLngRad());
        doc.put(FieldNames.TIMEPOINT.name(), maneuver.getTimePoint().asMillis());
        doc.put(FieldNames.MAIN_CURVE_BOUNDARIES.name(), buildCurveBoundariesDoc(maneuver.getMainCurveBoundaries()));
        doc.put(FieldNames.MANEUVER_CURVE_WITH_STABLE_SPEED_AND_COURSE_BOUNDERIES.name(),
                buildCurveBoundariesDoc(maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries()));
        doc.put(FieldNames.MAX_TURNING_RATE_IN_DEGREE_PER_SECOUND.name(), maneuver.getMaxTurningRateInDegreesPerSecond());
        doc.put(FieldNames.INDEX_OF_PASSED_WAYPOINT.name(), -1);
        doc.put(FieldNames.TIME_AS_MILLIS.name(), maneuver.getDuration().asMillis());
        doc.put(FieldNames.MANEUVER_LOSS.name(), null);
        return doc;
    }

    private Document buildCurveBoundariesDoc(final com.sap.sailing.domain.tracking.ManeuverCurveBoundaries f) {
        final Document d = new Document();
        d.put(FieldNames.MANEUVER_TIMEPOINT_BEFORE.name(), f.getTimePointBefore().asMillis());
        d.put(FieldNames.MANEUVER_TIMEPOINT_AFTER.name(), f.getTimePointAfter().asMillis());
        d.put(FieldNames.MANEUVER_SPEED_WITH_BEARING_BEFORE_DEGREES.name(), f.getSpeedWithBearingBefore().getBearing().getDegrees());
        d.put(FieldNames.MANEUVER_SPEED_WITH_BEARING_BEFORE_SPEED.name(), f.getSpeedWithBearingBefore().getKnots());
        d.put(FieldNames.MANEUVER_SPEED_WITH_BEARING_AFTER_DEGREES.name(), f.getSpeedWithBearingAfter().getBearing().getDegrees());
        d.put(FieldNames.MANEUVER_SPEED_WITH_BEARING_AFTER_SPEED_IN_KNOTS.name(), f.getSpeedWithBearingAfter().getKnots());
        d.put(FieldNames.MANEUVER_DIRECTION_CHANGE_IN_DEGREES.name(), f.getDirectionChangeInDegrees());
        d.put(FieldNames.MANEUVER_LOWEST_SPEED_IN_KNOTS.name(), f.getLowestSpeed().getKnots());
        d.put(FieldNames.MANEUVER_HIGHEST_SPEED_IN_KNOTS.name(), f.getHighestSpeed().getKnots());
        return d;
    }
}
