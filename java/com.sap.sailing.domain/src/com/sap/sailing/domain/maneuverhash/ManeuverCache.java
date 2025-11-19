package com.sap.sailing.domain.maneuverhash;

import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.tracking.Maneuver;

public interface ManeuverCache {
    void resume();

    List<Maneuver> get(Competitor key, boolean waitForLatest);

    void suspend();

    void triggerUpdate(Competitor key);
}