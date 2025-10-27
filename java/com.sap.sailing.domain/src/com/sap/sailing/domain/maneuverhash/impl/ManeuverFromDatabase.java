package com.sap.sailing.domain.maneuverhash.impl;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sse.util.ManeuverCache;
import com.sap.sse.util.SmartFutureCache.EmptyUpdateInterval;

public class ManeuverFromDatabase implements ManeuverCache<Competitor, List<Maneuver>, EmptyUpdateInterval> {
    
    public ManeuverFromDatabase(boolean suspended, TrackedRaceImpl race,
            ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) {
        super();
        this.suspended = suspended;
        this.race = race;
        this.maneuverRaceFingerprintRegistry = maneuverRaceFingerprintRegistry;
    }

    boolean suspended;
    private TrackedRaceImpl race;
    private ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry;
    private static final Logger logger = Logger.getLogger(ManeuverFromDatabase.class.getName());
    Map<Competitor, List<Maneuver>> maneuvers;

    public void resume() {
       logger.info("Found stored set of maneuvers for race "+race.getName()+" with matching fingerprint; loading instead of computing...");
       updateManeuversFromRegistry();
       suspended = false;
    }

    private void updateManeuversFromRegistry() {
        maneuvers = maneuverRaceFingerprintRegistry.loadManeuvers(race, race.getRace().getCourse());
//        for (final Entry<Competitor,List<Maneuver>> e : maneuverRaceFingerprintRegistry.loadManeuvers(
//                race, race.getRace().getCourse()).entrySet()) {
////            race.updateManeuvers(e.getKey(), e.getValue().stream().sorted(TimedComparator.INSTANCE).collect(Collectors.toList()));
//        }
    }

    public void suspend() {
        synchronized (this) {
            logger.finest("Suspended ManeuverFromDatabase");
            suspended = true;
        }    
    }

    public List<Maneuver> get(Competitor competitor, boolean waitForLatest) {
        return maneuvers.get(competitor);
    }

    @Override
    public void triggerUpdate(Competitor key, EmptyUpdateInterval updateInterval) {
      logger.log(Level.WARNING, "If Fingerprint matches, no Update should be triggered");
    }
}