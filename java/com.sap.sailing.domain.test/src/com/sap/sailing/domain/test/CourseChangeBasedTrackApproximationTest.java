package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.impl.CourseChangeBasedTrackApproximation;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class CourseChangeBasedTrackApproximationTest {
    private DynamicGPSFixTrack<Competitor, GPSFixMoving> track;
    private CourseChangeBasedTrackApproximation approximation;
    private final static BoatClass boatClass = new BoatClassImpl("505", /* typicallyStartsUpwind */true);

    @BeforeEach
    public void setUp() throws Exception {
        final CompetitorWithBoat competitor = TrackBasedTest.createCompetitorWithBoat("Someone");
        track = new DynamicGPSFixMovingTrackImpl<Competitor>(competitor,
                /* millisecondsOverWhichToAverage */5000, /* lossless compaction */true);
        approximation = new CourseChangeBasedTrackApproximation(track, competitor.getBoat().getBoatClass(), /* logFixes */ true);
    }
    
    /**
     * Whitebox test for {@link FixWindow} and its queue: add a few fixes to the queue, then add a few significantly
     * newer fixes to the queue that cause the older fixes to get added to the {@ilnk FixWindow} one after the other.
     * Then, add a fix immediately before the last that causes a direction change that exceeds the threshold and thus
     * produces a maneuver candidate letting only the newest fix in FixWindow. Then, add another fix to the queue,
     * causing the queue's oldest fix to get moved to the {@link FixWindow}, unless when trimming the
     * {@link FixWindow} the queue would have been trimmed with it. If not, a {@link NoSuchElementException}
     * would be thrown.
     */
    @Test
    public void testInsertWithEqualTimePoints() {
        final GPSFixMoving fix1 = fix(10000l, 49, 8, 5, 2);
        final GPSFixMoving fix2 = travel(fix1, 1000l, 5, 4);
        final GPSFixMoving fix3 = travel(fix2, 1000l, 5, 6);
        final double lastCOG = 16;
        final GPSFixMoving fix4 = travel(fix3, 1000l, 5, lastCOG);
        final GPSFixMoving fix5 = travel(fix4, 1000l, 5, lastCOG);
        track.add(fix1);
        track.add(fix2);
        track.add(fix3);
        track.add(fix4);
        track.add(fix5);
        // now add five newer fixes, forcing the five older fixes to advance into the FixWindow:
        final GPSFixMoving fix6 = travel(fix1, track.getMillisecondsOverWhichToAverageSpeed()+1, 5, lastCOG);
        final GPSFixMoving fix7 = travel(fix6, 1000l, 5, lastCOG);
        final GPSFixMoving fix8 = travel(fix7, 1000l, 5, lastCOG);
        final GPSFixMoving fix9 = travel(fix8, 1000l, 5, lastCOG);
        final GPSFixMoving fix10 = travel(fix9, 1000l, 5, lastCOG);
        track.add(fix6);
        track.add(fix7);
        track.add(fix8);
        track.add(fix9);
        track.add(fix10);
        // we expect no maneuver to have been recognized yet
        Iterable<GPSFixMoving> candidates = approximation.approximate(fix1.getTimePoint(), fix5.getTimePoint());
        assertTrue(Util.isEmpty(candidates));
        // now add a fix to the queue that is later than the FixWindow's start (assumed to be at fix1)
        // and before the FixWindow's end (assumed to be fix5 at the moment); the fix won't be added until
        // a fix that is at least track.getMillisecondsOverWhichToAverageSpeed() newer is added to the queue:
        final GPSFixMoving fix3_5 = travel(fix3, 500l, 5, 7);
        track.add(fix3_5);
        // now an even earlier fix that, when added to FixWindow, will trigger a maneuver candidate emission
        // because COG 0deg to COG 16deg is more than the 15deg threshold
        final GPSFixMoving fix2_5 = travel(fix2, 500l, 5, 320); // FIXME bug6222: this leads to the maneuver candidate being recognized at the beginning of the window, turning to port
        track.add(fix2_5);
        // force fix2_5 to get moved to FixWindow:
        final GPSFixMoving fix11 = travel(fix10, 1000l, 5, lastCOG);
        track.add(fix11); // expected to trigger moving oldest queued fix2_5 to get added to the FixWindow
        // we expect one maneuver candidate to have been recognized now
        Iterable<GPSFixMoving> candidates2 = approximation.approximate(fix1.getTimePoint(), fix5.getTimePoint());
        assertEquals(1, Util.size(candidates2));
//        assertSame(fix5, candidates2.iterator().next()); // TODO bug6222: fixes are replaced upon track insertion; use equality
        // and this should have spanned fix1..fix5 with the greatest course change on fix5, so that gets
        // emitted as the maneuver candidate, and all fixes from FixWindow's head up to and including fix5
        // are expected to get removed from FixWindow
        // Now add an ever newer fix to the queue, forcing fix3_5 to get added if it hasn't been cleaned up
        // by the FixWindow trimming:
        final GPSFixMoving fix12 = travel(fix11, 4000l, 5, 2);
        track.add(fix12);
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

    @Test
    public void testDirectionChangeJustAboveThreshold() {
        final Duration samplingInterval = Duration.ONE_SECOND;
        final double aBitOverMinimumManeuverAngleDegrees = boatClass.getManeuverDegreeAngleThreshold() * 1.5;
        final TimePoint start = TimePoint.of(10000l);
        final Speed speed = new KnotSpeedImpl(5.0);
        GPSFixMoving next = fix(start.asMillis(), 0, 0, speed.getKnots(), 0);
        track.add(next);
        // perform aBitOverMinimumManeuverAngleDegrees within five fixes:
        final int NUMBER_OF_FIXES_FOR_MANEUVER = 5;
        double cog = 0.0;
        for (int i=0; i<NUMBER_OF_FIXES_FOR_MANEUVER; i++) {
            cog = ((double) i+1.0)/((double) NUMBER_OF_FIXES_FOR_MANEUVER) * aBitOverMinimumManeuverAngleDegrees;
            next = travel(next, samplingInterval.asMillis(), speed.getKnots(), cog);
            track.add(next);
        }
        // now go straight for the maneuver duration to ensure that the approximation has read buffered fixes up to and including the end of the maneuver:
        for (int i=0; i<boatClass.getApproximateManeuverDurationInMilliseconds()/1000; i++) {
            next = travel(next, Duration.ONE_SECOND.asMillis(), speed.getKnots(), cog);
            track.add(next);
        }
        final Iterable<GPSFixMoving> oneManeuverCandidate = approximation.approximate(start, start.plus(samplingInterval.times(NUMBER_OF_FIXES_FOR_MANEUVER)));
        assertFalse(Util.isEmpty(oneManeuverCandidate));
        assertEquals(1, Util.size(oneManeuverCandidate));
    }

    /**
     * See also https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=6209#c5 which talks about FixWindow contents
     * that describe COG changes in different directions across the FixWindow's duration. This way, a COG change just
     * barely exceeding the threshold wouldn't be recognized/emitted as an approximated fix if it is preceded by a COG
     * change in the other direction that does not by itself pass the threshold.
     */
    @Test
    public void testTurningDirectionChangeInSameWindow() {
        final Duration samplingInterval = Duration.ONE_SECOND;
        final double halfMinimumManeuverAngleDegrees = boatClass.getManeuverDegreeAngleThreshold() / 2.0;
        final TimePoint start = TimePoint.of(10000l);
        final Speed speed = new KnotSpeedImpl(5.0);
        double cog = 0.0;
        GPSFixMoving next = fix(start.asMillis(), 0, 0, speed.getKnots(), cog);
        track.add(next);
        // perform halfMinimumManeuverAngleDegrees within five fixes:
        final int NUMBER_OF_FIXES_FOR_NON_MANEUVER = 5;
        for (int i=0; i<NUMBER_OF_FIXES_FOR_NON_MANEUVER; i++) {
            cog -= 1.0/((double) NUMBER_OF_FIXES_FOR_NON_MANEUVER) * halfMinimumManeuverAngleDegrees;
            next = travel(next, samplingInterval.asMillis(), speed.getKnots(), cog);
            track.add(next);
        }
        final Iterable<GPSFixMoving> emptyManeuverCandidates = approximation.approximate(start, start.plus(samplingInterval.times(NUMBER_OF_FIXES_FOR_NON_MANEUVER)));
        assertTrue(Util.isEmpty(emptyManeuverCandidates));
        final double aBitOverMinimumManeuverAngleDegrees = boatClass.getManeuverDegreeAngleThreshold() * 1.5;
        // perform aBitOverMinimumManeuverAngleDegrees within five fixes:
        final int NUMBER_OF_FIXES_FOR_MANEUVER = 5;
        for (int i=0; i<NUMBER_OF_FIXES_FOR_MANEUVER; i++) {
            cog += 1.0/((double) NUMBER_OF_FIXES_FOR_MANEUVER) * aBitOverMinimumManeuverAngleDegrees;
            next = travel(next, samplingInterval.asMillis(), speed.getKnots(), cog);
            track.add(next);
        }
        // now go straight for the maneuver duration to ensure that the approximation has read buffered fixes up to and including the end of the maneuver:
        for (int i=0; i<boatClass.getApproximateManeuverDurationInMilliseconds()/1000; i++) {
            next = travel(next, Duration.ONE_SECOND.asMillis(), speed.getKnots(), cog);
            track.add(next);
        }
        final Iterable<GPSFixMoving> oneManeuverCandidate = approximation.approximate(start, start.plus(samplingInterval.times(NUMBER_OF_FIXES_FOR_NON_MANEUVER+NUMBER_OF_FIXES_FOR_MANEUVER)));
        assertFalse(Util.isEmpty(oneManeuverCandidate));
        assertEquals(1, Util.size(oneManeuverCandidate));
    }

    private GPSFixMoving fix(long timepoint, double lat, double lon, double speedInKnots, double cogDeg) {
        return new GPSFixMovingImpl(new DegreePosition(lat, lon), new MillisecondsTimePoint(timepoint), new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(cogDeg)), /* optionalTrueHeading */ null);
    }
    
    private GPSFixMoving travel(GPSFixMoving fix, long durationInMillis, double speedInKnots, double cogDeg) {
        return new GPSFixMovingImpl(fix.getPosition().translateGreatCircle(new DegreeBearingImpl(cogDeg), new KnotSpeedImpl(speedInKnots).travel(new MillisecondsDurationImpl(durationInMillis))),
                fix.getTimePoint().plus(durationInMillis), new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(cogDeg)), /* optionalTrueHeading */ null);
    }
}
