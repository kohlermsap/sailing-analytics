package com.sap.sailing.domain.maneuverhash.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.maneuverhash.ManeuverCache;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintFactory;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
import com.sap.sailing.domain.maneuverhash.SerializableManeuverCache;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;

public class ManeuverCacheDelegate implements SerializableManeuverCache {
    private static final long serialVersionUID = 19872309587435L;
    private final TrackedRaceImpl race;
    private static final Logger logger = Logger.getLogger(ManeuverCacheDelegate.class.getName());
    private transient ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry;
    private volatile transient ManeuverCache cacheToUse;
    
    public ManeuverCacheDelegate(TrackedRaceImpl race,
            ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) {
        super();
        this.race = race;
        this.maneuverRaceFingerprintRegistry = maneuverRaceFingerprintRegistry;
        this.cacheToUse = createUpdatableManeuverCache();
    }    
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        this.cacheToUse = (ManeuversFromDatabase) ois.readObject();
    }
    
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(new ManeuversFromDatabase(getAllKnownManeuvers()));
    }
    
    @Override
    public void ensureFilled() {
        if (cacheToUse.canBeUpdated()) {
            for (final Competitor competitor : race.getShuffledCompetitors()) {
                cacheToUse.triggerUpdate(competitor);
            }
        }
    }

    @Override
    public void setManeuverRaceFingerprintRegistry(ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) {
        this.maneuverRaceFingerprintRegistry = maneuverRaceFingerprintRegistry;
    }

    private Map<Competitor, List<Maneuver>> getAllKnownManeuvers() {
        final Map<Competitor, List<Maneuver>> result = new HashMap<>();
        for (final Competitor competitor : race.getRace().getCompetitors()) {
            final List<Maneuver> maneuversForCompetitor = get(competitor, /* waitForLatest */ false);
            if (maneuversForCompetitor != null) {
                result.put(competitor, maneuversForCompetitor);
            }
        }
        return result;
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
                if (!cacheToUse.canBeUpdated()) {
                    cacheToUse = createUpdatableManeuverCache();
                }
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
            }, "Waiting for maneuvers for "+race.getName()+" after having resumed to store the results in registry")
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
    public void recalculate(Competitor competitor) {
        cacheToUse.recalculate(competitor);
    }

    @Override
    public void triggerUpdate(Competitor competitor) {
        if (!cacheToUse.canBeUpdated()) {
            logger.warning("Received a maneuver cache update trigger for competitor "+competitor.getName()+" but current cache cannot be updated; switching to an updatable cache");
            cacheToUse = createUpdatableManeuverCache();
        }
        cacheToUse.triggerUpdate(competitor);
    }

    private ManeuverCache createUpdatableManeuverCache() {
        return new ManeuversFromSmartFutureCache((DynamicTrackedRaceImpl) race);
    }

    @Override
    public boolean canBeUpdated() {
        return true;
    }
}
