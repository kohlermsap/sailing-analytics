package com.sap.sailing.domain.maneuverhash.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

//import java.util.logging.Logger;

import com.sap.sailing.domain.base.CPUMeteringType;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.maneuverdetection.ManeuverDetector;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintFactory;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.util.ManeuverCache;
import com.sap.sse.util.SmartFutureCache;
import com.sap.sse.util.SmartFutureCache.AbstractCacheUpdater;
import com.sap.sse.util.SmartFutureCache.EmptyUpdateInterval;


public class ManeuverCacheDelegate implements ManeuverCache<Competitor, List<Maneuver>, EmptyUpdateInterval> {


    private final TrackedRaceImpl race;
   // private static final Logger logger = Logger.getLogger(ManeuverCacheDelegate.class.getName());
    private final ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry;
//    private ManeuverCache<Competitor, List<Maneuver>, EmptyUpdateInterval> maneuverCache;
    private ManeuverFromDatabase cache;
    private SmartFutureCache<Competitor, List<Maneuver>, EmptyUpdateInterval> smartFutureCache; 
    Map<Competitor, List<Maneuver>> maneuvers;
//    // flag suspended / resume 
    private boolean cachesSuspended;
    private boolean triggerManeuverCacheInvalidationForAllCompetitors;

    public ManeuverCacheDelegate(TrackedRaceImpl race,
            ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) {
        super();
        this.race = race;
        this.maneuverRaceFingerprintRegistry = maneuverRaceFingerprintRegistry;
        this.cache = new ManeuverFromDatabase( false, (DynamicTrackedRaceImpl) race, maneuverRaceFingerprintRegistry);
        this.smartFutureCache = new SmartFutureCache<Competitor, List<Maneuver>, EmptyUpdateInterval>(
                            new AbstractCacheUpdater<Competitor, List<Maneuver>, EmptyUpdateInterval>() {
                                @Override
                                public List<Maneuver> computeCacheUpdate(Competitor competitor, EmptyUpdateInterval updateInterval)
                                        throws NoWindException {
                                    return race.getTrackedRegatta().callWithCPUMeterWithException(()->{
                                        Duration averageIntervalBetweenRawFixes = race.getTrack(competitor).getAverageIntervalBetweenRawFixes();
                                        if (averageIntervalBetweenRawFixes != null) {
                                            ManeuverDetector maneuverDetector;
                                            // FIXME The LowGPSSamplingRateManeuverDetectorImpl doesn't work very well; it recognizes many tacks only as bear-away and doesn't seem to have any noticeable benefits... See ORC Worlds 2019 ORC A Long Offshore
                //                            if (averageIntervalBetweenRawFixes.asSeconds() >= 30) {
                //                                maneuverDetector = new LowGPSSamplingRateManeuverDetectorImpl(TrackedRaceImpl.this, competitor);
                //                            } else {
                                                maneuverDetector = race.getManeuverDetectorPerCompetitorCache().getValue(competitor);
                                               
                //                            }
                                            List<Maneuver> maneuvers = race.computeManeuvers(competitor, maneuverDetector);
                                            return maneuvers;
                                        } else {
                                            return Collections.emptyList();
                                        }
                                    }, CPUMeteringType.MANEUVER_DETECTION.name());
                                }
                            }, /* nameForLocks */ "Maneuver cache for race " + race.getRace().getName());
    }    
    
    @Override
    public void resume() {
        // richtigen Ort bestimmen
        if (triggerManeuverCacheInvalidationForAllCompetitors) {
            triggerManeuverCacheRecalculationForAllCompetitors();
        }
        
        ManeuverRaceFingerprint fingerprint;
        race.getRace().getCourse().lockForRead(); 
        try {
            synchronized (this) {
                if (maneuverRaceFingerprintRegistry != null) {
                    fingerprint = maneuverRaceFingerprintRegistry.getManeuverRaceFingerprint(race.getRaceIdentifier());
                } else {
                    fingerprint = null;
                }
                if (fingerprint != null && fingerprint.matches(race)) {
                    cache.resume();
                } else {
                    new Thread(()->{
                        smartFutureCache.resume();
                        for(Competitor competitor : race.getRace().getCompetitors()) {
                            maneuvers.put(competitor, (List<Maneuver>) smartFutureCache.get(competitor, true));
                        }
                        maneuverRaceFingerprintRegistry.storeManeuvers(race.getRaceIdentifier(), ManeuverRaceFingerprintFactory.INSTANCE.createFingerprint(race), maneuvers, race.getRace().getCourse());
                    }, "Waiting for mark passings for "+race.getName()+" after having resumed to store the results in registry")
                    .start();
                }
            }
        } finally {
            race.getRace().getCourse().unlockAfterRead();
        }
    }

    @Override
    public List<Maneuver> get(Competitor competitor, boolean waitForLatest) {
        ManeuverRaceFingerprint fingerprint;
        race.getRace().getCourse().lockForRead(); 
        try {
            synchronized (this) {
                if (maneuverRaceFingerprintRegistry != null) {
                    fingerprint = maneuverRaceFingerprintRegistry.getManeuverRaceFingerprint(race.getRaceIdentifier());
                } else {
                    fingerprint = null;
                }
                if (fingerprint != null && fingerprint.matches(race)) {
                    return cache.get(competitor, waitForLatest);
                } else {
                   return smartFutureCache.get(competitor, waitForLatest);
                }
            }
        } finally {
            race.getRace().getCourse().unlockAfterRead();
        }
    }

    @Override
    public void suspend() {
        ManeuverRaceFingerprint fingerprint;
        race.getRace().getCourse().lockForRead(); 
        try {
            synchronized (this) {
                if (maneuverRaceFingerprintRegistry != null) {
                    fingerprint = maneuverRaceFingerprintRegistry.getManeuverRaceFingerprint(race.getRaceIdentifier());
                } else {
                    fingerprint = null;
                }
                if (fingerprint != null && fingerprint.matches(race)) {
                     cache.suspend();
                } else {
                   smartFutureCache.suspend();
                }
            }
        } finally {
            race.getRace().getCourse().unlockAfterRead();
        }    
    }

    @Override
    public void triggerUpdate(Competitor competitor, EmptyUpdateInterval updateInterval) {
        ManeuverRaceFingerprint fingerprint;
        race.getRace().getCourse().lockForRead(); 
        try {
            synchronized (this) {
                if (maneuverRaceFingerprintRegistry != null) {
                    fingerprint = maneuverRaceFingerprintRegistry.getManeuverRaceFingerprint(race.getRaceIdentifier());
                } else {
                    fingerprint = null;
                }
                if (fingerprint != null && fingerprint.matches(race)) {
                     cache.triggerUpdate(competitor, updateInterval);
                } else {
                   smartFutureCache.triggerUpdate(competitor, updateInterval);;
                }
            }
        } finally {
            race.getRace().getCourse().unlockAfterRead();
        }
    }
    
    
    public void triggerManeuverCacheRecalculationForAllCompetitors() {
        if (cachesSuspended) {
            triggerManeuverCacheInvalidationForAllCompetitors = true;
        } else {
            final List<Competitor> shuffledCompetitors = new ArrayList<>();
            for (Competitor competitor : (race.getRace().getCompetitors())) {
                shuffledCompetitors.add(competitor);
            }
            Collections.shuffle(shuffledCompetitors);
            for (Competitor competitor : shuffledCompetitors) {
                triggerManeuverCacheRecalculation(competitor);
            }
        }
    }

    public void triggerManeuverCacheRecalculation(final Competitor competitor) {
        if (cachesSuspended) {
            triggerManeuverCacheInvalidationForAllCompetitors = true;
        } else {
            triggerUpdate(competitor, /* updateInterval */null);
        }
    }
}
