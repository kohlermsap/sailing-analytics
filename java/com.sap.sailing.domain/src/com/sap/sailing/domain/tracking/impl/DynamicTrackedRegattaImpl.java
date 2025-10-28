package com.sap.sailing.domain.tracking.impl;

//import java.util.Optional;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
//import com.sap.sailing.domain.base.Sideline;
//import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
//import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
//import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
//import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
//import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;


public class DynamicTrackedRegattaImpl extends TrackedRegattaImpl implements DynamicTrackedRegatta {
    private static final long serialVersionUID = -90155868534737120L;

    public DynamicTrackedRegattaImpl(Regatta regatta) {
        super(regatta);
    }

    @Override
    public DynamicTrackedRace getTrackedRace(RaceDefinition race) {
        return (DynamicTrackedRace) super.getTrackedRace(race);
    }

    @SuppressWarnings("unchecked") // the tracked races of a dynamic tracked regatta are always DynamicTrackedRace objects; see also getTrackedRace(RaceDefinition)
    @Override
    public Iterable<DynamicTrackedRace> getTrackedRaces() {
        return (Iterable<DynamicTrackedRace>) super.getTrackedRaces();
    }

    @Override
    public DynamicTrackedRace getExistingTrackedRace(RaceDefinition race) {
        return (DynamicTrackedRace) super.getExistingTrackedRace(race);
    }

}
