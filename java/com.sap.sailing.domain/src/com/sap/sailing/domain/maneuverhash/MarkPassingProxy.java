package com.sap.sailing.domain.maneuverhash;

import java.io.Serializable;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class MarkPassingProxy implements MarkPassing {
    
    private static final long serialVersionUID = -1038446741597082803L;
    private final int waypointIndex;// Waypoint Index
    private final Serializable competitorId; // CompetitorID 
    private final TrackedRace trackedRace;
    private MarkPassing markPassing;
    private final TimePoint timePoint;
    
    public MarkPassingProxy(TimePoint timePoint, int waypointIndex, Serializable  competitorId, TrackedRace trackedRace ) {
        super();
        this.timePoint = timePoint;
        this.waypointIndex = waypointIndex;
        this.competitorId = competitorId;
        this.trackedRace = trackedRace;
    }
    
//    public MarkPassingProxy(MarkPassing markPassing, RaceIdentifier raceIdentifier) {
//        this.timePoint = markPassing.getTimePoint();
//        this.waypointIndex = (int) markPassing.getWaypoint();
//        this.competitorId =  markPassing.getCompetitor().getId();
//        this.trackedRace = (TrackedRace) raceIdentifier.getExistingTrackedRace(null);
//    }
    
    @Override
    public TimePoint getTimePoint() {
//        isMarkPassing();
        return timePoint;   
    }

    @Override
    public Waypoint getWaypoint() {
       Iterable<Waypoint>  waypoints = trackedRace.getRace().getCourse().getWaypoints();
//       for (Waypoint w : waypoints ) {
//           if( (int) w.getId() == waypointIndex) {
//               return w;
//           }
            return Util.get(waypoints, waypointIndex);
//       }
//       return null;
       // Util.get(course.getWaypoints(), waypointIndex); -> ersetzen möglich?
    }

    @Override
    public Competitor getCompetitor() {
        Iterable<Competitor> competitors = trackedRace.getRace().getCompetitors();
        for (Competitor c : competitors ) {
            if( c.getId() == competitorId) {
                return c;
            }
        }
        return null;
    }

    @Override
    public MarkPassing getOriginal() {
        isMarkPassing();
        return markPassing.getOriginal();
    }

    private void isMarkPassing() {
        if (markPassing == null) {
            markPassing = trackedRace.getMarkPassing(getCompetitor(), getWaypoint()); 
        }
    }
}
