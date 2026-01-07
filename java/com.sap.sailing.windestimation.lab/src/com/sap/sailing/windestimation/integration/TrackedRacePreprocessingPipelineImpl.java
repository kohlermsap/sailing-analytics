package com.sap.sailing.windestimation.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.TrackTimeInfo;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverDetectorImpl;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverDetectorWithEstimationDataSupportDecoratorImpl;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.CompleteManeuverCurve;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.data.WindQuality;
import com.sap.sailing.windestimation.data.transformer.CompleteManeuverCurveWithEstimationDataToManeuverForEstimationTransformer;
import com.sap.sailing.windestimation.preprocessing.PreprocessingPipeline;
import com.sap.sailing.windestimation.preprocessing.RaceElementsFilteringPreprocessingPipelineImpl;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Util;

/**
 * @author Vladislav Chumak (D069712)
 * @deprecated This implementation is inefficient in terms of CPU performance. Use
 *             {@link InteractiveMstHmmWindEstimationForTrackedRace} instead.
 *
 */
@Deprecated
public class TrackedRacePreprocessingPipelineImpl
        implements PreprocessingPipeline<TrackedRace, RaceWithEstimationData<ManeuverForEstimation>> {

    private final PolarDataService polarDataService;
    private final RaceElementsFilteringPreprocessingPipelineImpl raceElementsFilteringPreprocessingPipeline = new RaceElementsFilteringPreprocessingPipelineImpl(
            new CompleteManeuverCurveWithEstimationDataToManeuverForEstimationTransformer());

    @Deprecated
    public TrackedRacePreprocessingPipelineImpl(PolarDataService polarDataService) {
        this.polarDataService = polarDataService;
    }

    @Deprecated
    @Override
    public RaceWithEstimationData<ManeuverForEstimation> preprocessInput(TrackedRace element) {
        List<CompetitorTrackWithEstimationData<CompleteManeuverCurveWithEstimationData>> competitorTracks = getCompetitorTracksWithManeuverEstimationData(
                element);
        RaceWithEstimationData<CompleteManeuverCurveWithEstimationData> race = new RaceWithEstimationData<>(
                element.getTrackedRegatta().getRegatta().getName(), element.getRace().getName(), WindQuality.LOW,
                competitorTracks);
        RaceWithEstimationData<ManeuverForEstimation> preprocessedRace = raceElementsFilteringPreprocessingPipeline
                .preprocessInput(race);
        return preprocessedRace;
    }

    @Deprecated
    public List<CompetitorTrackWithEstimationData<CompleteManeuverCurveWithEstimationData>> getCompetitorTracksWithManeuverEstimationData(
            TrackedRace trackedRace) {
        Iterable<Competitor> competitors = trackedRace.getRace().getCompetitors();
        ManeuverDetectorWithEstimationDataSupportDecoratorImpl maneuverDetector = new ManeuverDetectorWithEstimationDataSupportDecoratorImpl(
                new ManeuverDetectorImpl(), polarDataService);
        List<CompetitorTrackWithEstimationData<CompleteManeuverCurveWithEstimationData>> competitorTracks = new ArrayList<>();
        for (Competitor competitor : competitors) {
            TrackTimeInfo trackTimeInfo = maneuverDetector.getTrackTimeInfo();
            if (trackTimeInfo != null) {
                Iterable<Maneuver> maneuvers = trackedRace.getManeuvers(competitor, false);
                List<CompleteManeuverCurve> completeManeuverCurves = maneuverDetector
                        .getCompleteManeuverCurves(maneuvers);
                List<CompleteManeuverCurveWithEstimationData> completeManeuverCurvesWithEstimationData = maneuverDetector
                        .getCompleteManeuverCurvesWithEstimationData(completeManeuverCurves);
                CompetitorTrackWithEstimationData<CompleteManeuverCurveWithEstimationData> competitorTrackWithEstimationData = createCompetitorTrack(
                        trackedRace, competitor, trackTimeInfo, completeManeuverCurvesWithEstimationData);
                competitorTracks.add(competitorTrackWithEstimationData);
            }

        }
        return competitorTracks;
    }

    private <T> CompetitorTrackWithEstimationData<T> createCompetitorTrack(TrackedRace trackedRace,
            Competitor competitor, TrackTimeInfo trackTimeInfo, List<T> completeManeuverCurvesWithEstimationData) {
        BoatClass boatClass = trackedRace.getBoatOfCompetitor(competitor).getBoatClass();
        GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
        Distance distanceTravelled = track.getDistanceTraveled(trackTimeInfo.getTrackStartTimePoint(),
                trackTimeInfo.getTrackEndTimePoint());
        String regattaName = trackedRace.getTrackedRegatta().getRegatta().getName();
        String raceName = trackedRace.getRace().getName();
        double avgIntervalBetweenFixesInSeconds = track.getAverageIntervalBetweenFixes().asSeconds();
        CompetitorTrackWithEstimationData<T> competitorTrackWithEstimationData = new CompetitorTrackWithEstimationData<>(
                regattaName, raceName, competitor.getName(), boatClass, completeManeuverCurvesWithEstimationData,
                avgIntervalBetweenFixesInSeconds, distanceTravelled, trackTimeInfo.getTrackStartTimePoint(),
                trackTimeInfo.getTrackEndTimePoint(), getMarkPassingsCount(trackedRace, competitor),
                getWaypointsCount(trackedRace));
        return competitorTrackWithEstimationData;
    }

    private int getMarkPassingsCount(TrackedRace trackedRace, Competitor competitor) {
        int markPassingsCount = 0;
        NavigableSet<MarkPassing> markPassings = trackedRace.getMarkPassings(competitor, false);
        trackedRace.lockForRead(markPassings);
        try {
            markPassingsCount = Util.size(markPassings);
        } finally {
            trackedRace.unlockAfterRead(markPassings);
        }
        return markPassingsCount;
    }

    private int getWaypointsCount(TrackedRace trackedRace) {
        Iterable<Waypoint> waypoints = trackedRace.getRace().getCourse().getWaypoints();
        return Util.size(waypoints);
    }

}
