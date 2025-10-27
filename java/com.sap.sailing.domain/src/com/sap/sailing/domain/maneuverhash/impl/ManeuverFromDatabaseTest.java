package com.sap.sailing.domain.maneuverhash.impl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.impl.TimedComparator;
import com.sap.sse.util.SmartFutureCache;

public class ManeuverFromDatabaseTest extends SmartFutureCache{
    private final DynamicTrackedRace race;
    private final ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry;

    public ManeuverFromDatabaseTest(CacheUpdater cacheUpdateComputer, String nameForLocks,DynamicTrackedRace race, ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry ) {
        super(cacheUpdateComputer, nameForLocks);
        this.race = race;
        this.maneuverRaceFingerprintRegistry = maneuverRaceFingerprintRegistry;
        // TODO Auto-generated constructor stub
    }
    
    
    public void resume() {
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
                    
                }
                
            }
            
        } finally {
            race.getRace().getCourse().unlockAfterRead();
        }
    }
    
    private void updateManeuversFromRegistry() {
        Map<Competitor, List<Maneuver>> maneuvers = maneuverRaceFingerprintRegistry.loadManeuvers(
                race, race.getRace().getCourse());
        for (final  Competitor e : maneuvers.keySet()) {
            List<Maneuver> competitorManeuvers = maneuvers.get(e);

                race.updateManeuvers(e, competitorManeuvers.stream().sorted(TimedComparator.INSTANCE).collect(Collectors.toList()));

            
        }
    }
    

}
