package com.sap.sailing.domain.racelogtracking.impl.fixtracker;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.RaceLogRevokeEvent;
import com.sap.sailing.domain.abstractlog.race.impl.BaseRaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDenoteForTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.analyzing.impl.RaceLogTrackingStateAnalyzer;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogTrackingState;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelogsensortracking.SensorFixMapperFactory;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.TrackingDataLoader;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;

/**
 * This class manages the lifecycle of the {@link FixLoaderAndTracker} by listening to
 * {@link RaceLogDenoteForTrackingEvent}, race log attached and stop tracking race changes.<p>
 * 
 * Once the race is stopped, it notifies its own owner so that all reference to this instance can be cleanly
 * removed to prevent memory leaks.
 */
public class RaceLogFixTrackerManager implements TrackingDataLoader {
    private static final Logger logger = Logger.getLogger(RaceLogFixTrackerManager.class.getName());

    private final DynamicTrackedRace trackedRace;

    private final SensorFixStore sensorFixStore;
    
    private final boolean removeOutliersFromCompetitorTracks;

    private final SensorFixMapperFactory sensorFixMapperFactory;
    
    /**
     * We maintain our own collection that holds the RaceLogs. The known RaceLogs should by in sync with the ones that
     * can be obtained from the TrackedRace. When stopping, there could be a concurrency issue that leads to a listener
     * not being removed. This is prevented by remembering all RaceLogs to which we attached a listener. So we can be
     * sure to not produce a memory leak.
     */
    private final Set<RaceLog> knownRaceLogs = new HashSet<>();
    
    private FixLoaderAndTracker tracker;

    private final RaceChangeListener raceChangeListener = new AbstractRaceChangeListener() {
        public void raceLogAttached(RaceLog raceLog) {
            addRaceLog(raceLog);
        }
        
        public void raceLogDetached(RaceLog raceLog) {
            removeRaceLog(raceLog);
        }
    };

    private final RaceLogEventVisitor raceLogEventVisitor = new BaseRaceLogEventVisitor() {
        @Override
        public void visit(RaceLogDenoteForTrackingEvent event) {
            updateDenotationState();
        }

        @Override
        public void visit(RaceLogRevokeEvent event) {
            updateDenotationState();
        }
    };

    public RaceLogFixTrackerManager(DynamicTrackedRace trackedRace, SensorFixStore sensorFixStore,
            SensorFixMapperFactory sensorFixMapperFactory, boolean removeOutliersFromCompetitorTracks) {
        this.removeOutliersFromCompetitorTracks = removeOutliersFromCompetitorTracks;
        this.trackedRace = trackedRace;
        this.sensorFixStore = sensorFixStore;
        this.sensorFixMapperFactory = sensorFixMapperFactory;
        trackedRace.addListener(raceChangeListener);
        synchronized (knownRaceLogs) {
            for (RaceLog raceLog : trackedRace.getAttachedRaceLogs()) {
                addRaceLogUnlocked(raceLog);
            }
        }
        updateDenotationState();
    }

    private synchronized void updateDenotationState() {
        if (isForTracking()) {
            startTrackerIfNotAlreadyStarted();
        } else {
            stopTrackerIfStillRunning(/* preemptive */ false, /* willBeRemoved */ false);
        }
        this.notifyAll();
    }
    
    private void addRaceLog(RaceLog raceLog) {
        synchronized (knownRaceLogs) {
            addRaceLogUnlocked(raceLog);
        }
        updateDenotationState();
    }
    
    private void removeRaceLog(RaceLog raceLog) {
        synchronized (knownRaceLogs) {
            removeRaceLogUnlocked(raceLog);
        }
        updateDenotationState();
    }
    
    private void addRaceLogUnlocked(RaceLog raceLog) {
        knownRaceLogs.add(raceLog);
        raceLog.addListener(raceLogEventVisitor);
    }
    
    private void removeRaceLogUnlocked(RaceLog raceLog) {
        raceLog.removeListener(raceLogEventVisitor);
        knownRaceLogs.remove(raceLog);
    }
    
    public void stop(boolean preemptive, boolean willBeRemoved) {
        stopTrackerIfStillRunning(preemptive, willBeRemoved);
        trackedRace.removeListener(raceChangeListener);
        synchronized (knownRaceLogs) {
            final Set<RaceLog> tempSet = new HashSet<>(knownRaceLogs);
            for (RaceLog raceLog : tempSet) {
                removeRaceLogUnlocked(raceLog);
            }
        }
    }
    
    private synchronized void startTrackerIfNotAlreadyStarted() {
        if (tracker == null) {
            logger.fine("Starting fix tracker for TrackedRace: " + trackedRace.getRaceIdentifier());
            tracker = new FixLoaderAndTracker(trackedRace, sensorFixStore, sensorFixMapperFactory, removeOutliersFromCompetitorTracks);
        }
    }

    private synchronized void stopTrackerIfStillRunning(boolean preemptive, boolean willBeRemoved) {
        if (tracker != null) {
            logger.fine("Stopping fix tracker for TrackedRace: " + trackedRace.getRaceIdentifier());
            tracker.stop(preemptive, willBeRemoved);
            tracker = null;
        }
    }

    boolean isForTracking() {
        synchronized (knownRaceLogs) {
            for (RaceLog raceLog : knownRaceLogs) {
                RaceLogTrackingState raceLogTrackingState = new RaceLogTrackingStateAnalyzer(raceLog).analyze();
                if (raceLogTrackingState.isForTracking()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public synchronized FixLoaderAndTracker waitForTracker() throws InterruptedException {
        while (tracker == null) {
            this.wait();
        }
        return tracker;
    }
}
