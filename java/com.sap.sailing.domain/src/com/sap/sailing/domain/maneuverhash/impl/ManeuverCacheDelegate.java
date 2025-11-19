package com.sap.sailing.domain.maneuverhash.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.maneuverhash.ManeuverCache;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintFactory;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sse.util.SmartFutureCache.EmptyUpdateInterval;

public class ManeuverCacheDelegate implements ManeuverCache<Competitor, List<Maneuver>, EmptyUpdateInterval> {
    private final TrackedRaceImpl race;
    private static final Logger logger = Logger.getLogger(ManeuverCacheDelegate.class.getName());
    private final ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry;
    private volatile ManeuverCache<Competitor, List<Maneuver>, EmptyUpdateInterval> cacheToUse;
    
    public ManeuverCacheDelegate(TrackedRaceImpl race,
            ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) {
        super();
        this.race = race;
        this.maneuverRaceFingerprintRegistry = maneuverRaceFingerprintRegistry;
        this.cacheToUse = new ManeuversFromSmartFutureCache((DynamicTrackedRaceImpl) race);
    }    
    
    @Override
    public void resume() {
        final ManeuverRaceFingerprint fingerprint;
        if (maneuverRaceFingerprintRegistry != null) {
            logger.info("Compare maneuver fingerprints for race "+race.getRaceIdentifier());
            race.waitForAllRaceLogsAttached();
            fingerprint = maneuverRaceFingerprintRegistry.getManeuverRaceFingerprint(race.getRaceIdentifier());
        } else {
            fingerprint = null;
        }
        if (fingerprint != null && fingerprint.matches(race)) {
            logger.info("Maneuver fingerprints match for race "+race.getRaceIdentifier()+"; loading from DB instead of computing");
            cacheToUse = new ManeuversFromDatabase(maneuverRaceFingerprintRegistry.loadManeuvers(race, race.getRace().getCourse()));
        } else {
            new Thread(()->{
                logger.info("Maneuver fingerprints do not match for race "+race.getRaceIdentifier()+"; NOT loading from DB");
                cacheToUse.resume();
                if (maneuverRaceFingerprintRegistry != null) {
                    // wait for maneuvers to be computed by the default cache implementation (SmartFutureCache),
                    // then store persistently in registry
                    final Map<Competitor, List<Maneuver>> maneuvers = new HashMap<>();
                    for (final Competitor competitor : race.getRace().getCompetitors()) {
                        maneuvers.put(competitor, (List<Maneuver>) cacheToUse.get(competitor, /* waitForLatest */ true));
                    }
                    maneuverRaceFingerprintRegistry.storeManeuvers(race.getRaceIdentifier(), ManeuverRaceFingerprintFactory.INSTANCE.createFingerprint(race), maneuvers, race.getRace().getCourse());
                }
            }, "Waiting for mark passings for "+race.getName()+" after having resumed to store the results in registry")
            .start();
        }
    }

    @Override
    public List<Maneuver> get(Competitor competitor, boolean waitForLatest) {
        return cacheToUse.get(competitor, waitForLatest);
    }

    @Override
    public void suspend() {
        cacheToUse.suspend();
    }

    @Override
    public void triggerUpdate(Competitor competitor, EmptyUpdateInterval updateInterval) {
        cacheToUse.triggerUpdate(competitor, updateInterval);
    }
}