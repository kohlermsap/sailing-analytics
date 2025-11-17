package com.sap.sailing.domain.maneuverhash.impl;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.maneuverhash.ManeuverCache;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sse.util.SmartFutureCache.EmptyUpdateInterval;

public class ManeuversFromDatabase implements ManeuverCache<Competitor, List<Maneuver>, EmptyUpdateInterval> {
    
    public ManeuversFromDatabase(
             Map<Competitor, List<Maneuver>> maneuvers) {
        super();
        this.maneuvers = maneuvers;
    }

    boolean suspended;
    private static final Logger logger = Logger.getLogger(ManeuversFromDatabase.class.getName());
    Map<Competitor, List<Maneuver>> maneuvers;

    public void resume() {
        logger.log(Level.WARNING, "Method should never be called");
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
      // TODO change to smartFutureCache in Delegate
    }
}