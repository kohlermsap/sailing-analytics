package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.impl.CourseChangeBasedTrackApproximation;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class CourseChangeBasedTrackApproximationTest extends OnlineTracTracBasedTest {
    private DynamicGPSFixTrack<Competitor, GPSFixMoving> track;
    private CourseChangeBasedTrackApproximation approximation;

    public CourseChangeBasedTrackApproximationTest() throws MalformedURLException, URISyntaxException {
        super();
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///" + new File("resources/event_20110609_KielerWoch-505_Race_2.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(
                new URL("file:///" + new File("resources/event_20110609_KielerWoch-505_Race_2.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        assertFalse(Util.isEmpty(getTrackedRace().getRace().getCompetitors()));
        final DynamicGPSFixTrack<Competitor, GPSFixMoving> sampleTrack = getTrackedRace().getTrack(getTrackedRace().getRace().getCompetitors().iterator().next());
        sampleTrack.lockForRead();
        try {
            assertFalse(Util.isEmpty(sampleTrack.getRawFixes()));
        } finally {
            sampleTrack.unlockAfterRead();
        }
        final CompetitorWithBoat competitor = TrackBasedTest.createCompetitorWithBoat("Someone");
        track = new DynamicGPSFixMovingTrackImpl<Competitor>(competitor,
                /* millisecondsOverWhichToAverage */5000, /* lossless compaction */true);
        approximation = new CourseChangeBasedTrackApproximation(track, competitor.getBoat().getBoatClass());
    }
    
    /**
     * During the work on bug5959 (https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5959) we identified an issue
     * with early vs. late initialization of the approximation. When initializing the
     * {@link CourseChangeBasedTrackApproximation} objects before adding any fixes to the track, we received different
     * results than when initializing it after adding fixes and first trying to compute maneuvers. This goes against the
     * specification that the approximation should be independent of when it is initialized.<p>
     * 
     * To conduct the test, we take the fixes from a loaded competitor track, create a new {@link DynamicGPSFixMovingTrackImpl},
     * construct a {@link CourseChangeBasedTrackApproximation} based on this track (early initialization), then copy all fixes
     * from the loaded competitor track to the new test track (which adds these fixes to the approximation), then create a second
     * {@link CourseChangeBasedTrackApproximation} which will then add all fixes in one sweep. Then, we compare the results of
     * {@link CourseChangeBasedTrackApproximation#approximate(TimePoint, TimePoint)} for the duration of the entire track,
     * expecting them to be equal.
     */
    @Test
    public void testNoDiffBetweenEarlyAndLateInitialization() {
        final Iterable<Competitor> competitors = getTrackedRace().getRace().getCompetitors();
        final CompetitorWithBoat sampleCompetitor = (CompetitorWithBoat) Util.get(competitors, new Random().nextInt(Util.size(competitors)));
        final DynamicGPSFixTrack<Competitor, GPSFixMoving> sampleTrack = getTrackedRace().getTrack(sampleCompetitor);
        final DynamicGPSFixTrack<Competitor, GPSFixMoving> trackCopy = new DynamicGPSFixMovingTrackImpl<Competitor>(sampleCompetitor, /* millisecondsOverWhichToAverage */ 15000);
        final CourseChangeBasedTrackApproximation earlyInitApproximation = new CourseChangeBasedTrackApproximation(trackCopy, sampleCompetitor.getBoat().getBoatClass());
        final TimePoint from = sampleTrack.getFirstRawFix().getTimePoint();
        final TimePoint to = sampleTrack.getLastRawFix().getTimePoint();
        sampleTrack.lockForRead();
        try {
            for (final GPSFixMoving fix : sampleTrack.getRawFixes()) {
                trackCopy.add(fix);
            }
        } finally {
            sampleTrack.unlockAfterRead();
        }
        final CourseChangeBasedTrackApproximation lateInitApproximation = new CourseChangeBasedTrackApproximation(trackCopy, sampleCompetitor.getBoat().getBoatClass());
        final Iterable<GPSFixMoving> earlyInitResult = earlyInitApproximation.approximate(from, to);
        final Iterable<GPSFixMoving> lateInitResult = lateInitApproximation.approximate(from, to);
        assertEquals(Util.size(earlyInitResult), Util.size(lateInitResult), "Different numbers of approximation points for competitor "+sampleCompetitor.getName());
        assertEquals(Util.asSet(earlyInitResult), Util.asSet(lateInitResult));
    }
    
    @Test
    public void simpleTackRecognition() {
        final GPSFixMoving start = fix(10000l, 0, 0, 5, 0);
        track.add(start);
        GPSFixMoving next = start;
        for (int i=0; i<20; i++) {
            track.add(next = travel(next, 1000 /*ms*/, 5 /* knots */, 0 /*deg COG*/));
        }
        final TimePoint startOfTurn = next.getTimePoint().minus(1000); // the turn will be considered started at the previous fix
        // now turn to port over three fixes:
        track.add(next = travel(next, 1000, 4, 340));
        next = travel(next, 1000, 3, 290);
        track.add(next);
        track.add(next = travel(next, 1000, 3, 270));
        final TimePoint endOfTurn = next.getTimePoint();
        for (int i=0; i<20; i++) {
            track.add(next = travel(next, 1000 /*ms*/, 5 /* knots */, 270 /*deg COG*/));
        }
        Iterable<GPSFixMoving> candidates = approximation.approximate(start.getTimePoint(), next.getTimePoint());
        assertTrue(Util.size(candidates) >= 1);
        for (final GPSFixMoving candidate : candidates) {
            assertTrue(!candidate.getTimePoint().before(startOfTurn) &&
                    !candidate.getTimePoint().after(endOfTurn));
        }
    }

    private GPSFixMoving fix(long timepoint, double lat, double lon, double speedInKnots, double cogDeg) {
        return new GPSFixMovingImpl(new DegreePosition(lat, lon), new MillisecondsTimePoint(timepoint), new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(cogDeg)), /* optionalTrueHeading */ null);
    }
    
    private GPSFixMoving travel(GPSFixMoving fix, long durationInMillis, double speedInKnots, double cogDeg) {
        return new GPSFixMovingImpl(fix.getPosition().translateGreatCircle(new DegreeBearingImpl(cogDeg), new KnotSpeedImpl(speedInKnots).travel(new MillisecondsDurationImpl(durationInMillis))),
                fix.getTimePoint().plus(durationInMillis), new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(cogDeg)), /* optionalTrueHeading */ null);
    }
}
