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
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;

/**
 * Reproduces the pre-pagination behaviour of {@code MongoObjectFactoryImpl.storeCompetitorManeuvers()}:
 * all maneuvers for a competitor were stored in a single document with no page splitting. For competitors
 * with very large maneuver counts this document exceeds MongoDB's 16MB BSON limit.
 *
 * This class exists solely to let {@link ManeuverStorageLimitTest} assert that the old single-page code
 * path fails with {@code BsonMaximumSizeExceededException} for a competitor with 2500 maneuvers.
 * It must never be used in production.
 */
class OldSinglePageMongoObjectFactoryForBug6226Test {
    private final MongoDatabase database;

    OldSinglePageMongoObjectFactoryForBug6226Test(final MongoDatabase database) {
        this.database = database;
    }

    void storeManeuvers(final RaceIdentifier raceIdentifier, final ManeuverRaceFingerprint fingerprint,
            final Course course, final Map<Competitor, List<Maneuver>> maneuvers) {
        final MongoCollection<Document> maneuverCollection = database.getCollection(CollectionNames.MANEUVERS.name());
        final Document fingerprintDoc = Document.parse(fingerprint.toJson().toString());
        for (final Entry<Competitor, List<Maneuver>> e : maneuvers.entrySet()) {
            storeCompetitorManeuversSinglePage(maneuverCollection, raceIdentifier, fingerprintDoc, course, e.getKey(), e.getValue());
        }
    }

    private void storeCompetitorManeuversSinglePage(final MongoCollection<Document> maneuverCollection,
            final RaceIdentifier raceIdentifier, final Document fingerprintDoc, final Course course,
            final Competitor competitor, final List<Maneuver> competitorManeuvers) {
        final Document query = new Document();
        query.put(FieldNames.EVENT_NAME.name(), raceIdentifier.getRegattaName());
        query.put(FieldNames.RACE_NAME.name(), raceIdentifier.getRaceName());
        query.put(FieldNames.COMPETITOR_ID.name(), competitor.getId());
        final Document result = new Document();
        result.put(FieldNames.MANEUVER_FINGERPRINT.name(), fingerprintDoc);
        result.put(FieldNames.EVENT_NAME.name(), raceIdentifier.getRegattaName());
        result.put(FieldNames.RACE_NAME.name(), raceIdentifier.getRaceName());
        result.put(FieldNames.COMPETITOR_ID.name(), competitor.getId());
        final List<Document> maneuverList = new ArrayList<>();
        if (competitorManeuvers != null) {
            for (final Maneuver maneuver : competitorManeuvers) {
                maneuverList.add(buildManeuverDoc(maneuver));
            }
        }
        result.put(FieldNames.MANEUVERS.name(), maneuverList);
        maneuverCollection.replaceOne(query, result, new ReplaceOptions().upsert(true));
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

    private Document buildCurveBoundariesDoc(final ManeuverCurveBoundaries f) {
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
