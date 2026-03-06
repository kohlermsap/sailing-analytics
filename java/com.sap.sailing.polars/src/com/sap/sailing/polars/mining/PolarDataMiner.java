package com.sap.sailing.polars.mining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.impl.SpeedWithBearingWithConfidenceImpl;
import com.sap.sailing.domain.base.impl.SpeedWithConfidenceImpl;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.PolarSheetGenerationSettings;
import com.sap.sailing.domain.common.PolarSheetsData;
import com.sap.sailing.domain.common.PolarSheetsHistogramData;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.impl.PolarSheetsDataImpl;
import com.sap.sailing.domain.common.impl.PolarSheetsHistogramDataImpl;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.polars.PolarsChangedListener;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.polars.impl.CubicEquation;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.datamining.components.FilterCriterion;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.data.ClusterGroup;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.functions.ParameterProvider;
import com.sap.sse.datamining.functions.ParameterizedFunction;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.impl.components.ParallelFilteringProcessor;
import com.sap.sse.datamining.impl.components.ParallelMultiDimensionsValueNestingGroupingProcessor;
import com.sap.sse.datamining.impl.functions.SimpleParameterizedFunction;
import com.sap.sse.util.ThreadPoolUtil;
import com.sap.sse.util.impl.ThreadFactoryWithPriority;

/**
 * Entry point for the aggregation of backend polar data and backend to that data.
 * <p>
 * 
 * Creates a polar data pipeline upon creation and puts incoming GPS fixes into that pipeline. Also holds references to
 * the actual data containers in which the aggregation results lay.
 * <p>
 * 
 * For more information on polars in SAP Sailing Analytics, please see:
 * <a href="https://wiki.sapsailing.com/wiki/howto/misc/polars">https://wiki.sapsailing.com/wiki/howto/misc/polars</a>
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class PolarDataMiner {

    private static final int EXECUTOR_QUEUE_SIZE = 100;
    private static final int THREAD_POOL_SIZE = ThreadPoolUtil.INSTANCE.getReasonableThreadPoolSize();
    private final ThreadPoolExecutor executor = createExecutor();
    private static final ScheduledExecutorService processRacesThatFinishedLoadingExecutor = ThreadPoolUtil.INSTANCE
            .createBackgroundTaskThreadPoolExecutor(1,
                    PolarDataMiner.class.getName() + " processRacesThatFinishedLoadingExecutor");
    private final Map<BoatClass, AtomicInteger> stats = new ConcurrentHashMap<>();

    private final Queue<GPSFixMovingWithOriginInfo> fixQueue = new ConcurrentLinkedQueue<GPSFixMovingWithOriginInfo>();

    private static final Logger logger = Logger.getLogger(PolarDataMiner.class.getSimpleName());

    private final ConcurrentMap<BoatClass, Set<PolarsChangedListener>> listeners = new ConcurrentHashMap<>();

    private ParallelFilteringProcessor<GPSFixMovingWithOriginInfo> preFilteringProcessor;

    private final PolarSheetGenerationSettings backendPolarSheetGenerationSettings;

    /**
     * This processor uses two cubic regressions angle to the true wind over windspeed and boatspeed over windspeed for
     * each course (legtype tack combination)
     */
    private final CubicRegressionPerCourseProcessor cubicRegressionPerCourseProcessor;

    private final SpeedRegressionPerAngleClusterProcessor speedRegressionPerAngleClusterProcessor;
    private final ClusterGroup<Bearing> angleClusterGroup;

    private ThreadPoolExecutor createExecutor() {
        return new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 60l, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(EXECUTOR_QUEUE_SIZE), new ThreadFactoryWithPriority(PolarDataMiner.class.getSimpleName(),
                        Thread.NORM_PRIORITY-1, /* daemon */true)) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                GPSFixMovingWithOriginInfo fix;
                while (this.getQueue().size() < (EXECUTOR_QUEUE_SIZE / 10) && (fix=fixQueue.poll()) != null) {
                    preFilteringProcessor.processElement(fix);
                }
            }
        };
    }

    public PolarDataMiner(PolarSheetGenerationSettings backendPolarSettings,
            CubicRegressionPerCourseProcessor cubicRegressionPerCourseProcessor,
            SpeedRegressionPerAngleClusterProcessor speedRegressionPerAngleClusterProcessor,
            ClusterGroup<Bearing> angleClusterGroup) {
        cubicRegressionPerCourseProcessor.setListeners(listeners);
        speedRegressionPerAngleClusterProcessor.setListeners(listeners);
        backendPolarSheetGenerationSettings = backendPolarSettings;
        this.cubicRegressionPerCourseProcessor = cubicRegressionPerCourseProcessor;
        this.speedRegressionPerAngleClusterProcessor = speedRegressionPerAngleClusterProcessor;
        this.angleClusterGroup = angleClusterGroup;
        try {
            setUpWorkflow();
        } catch (ClassCastException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    public PolarDataMiner filterToBoatClasses(Iterable<BoatClass> boatClasses) {
        return new PolarDataMiner(backendPolarSheetGenerationSettings,
                                  cubicRegressionPerCourseProcessor.filterToBoatClasses(boatClasses),
                                  speedRegressionPerAngleClusterProcessor.filterToBoatClasses(boatClasses),
                                  angleClusterGroup);
    }

    private void setUpWorkflow() throws ClassCastException, NoSuchMethodException, SecurityException {
        Collection<Processor<GroupedDataEntry<GPSFixMovingWithPolarContext>, ?>> regressionPerCourseGrouperResultReceivers = new ArrayList<Processor<GroupedDataEntry<GPSFixMovingWithPolarContext>, ?>>();
        regressionPerCourseGrouperResultReceivers.add(cubicRegressionPerCourseProcessor);
        Collection<ParameterizedFunction<?>> parameterizedDimensionsForCubicRegression = new ArrayList<>();
        for (Function<?> function : PolarDataDimensionCollectionFactory
                .getCubicRegressionPerCourseClusterKeyDimensions()) {
            parameterizedDimensionsForCubicRegression.add(new SimpleParameterizedFunction<>(function,
                    ParameterProvider.NULL));
        }
        Processor<GPSFixMovingWithPolarContext, GroupedDataEntry<GPSFixMovingWithPolarContext>> cubicRegressionPerCourseGroupingProcessor = new ParallelMultiDimensionsValueNestingGroupingProcessor<GPSFixMovingWithPolarContext>(
                GPSFixMovingWithPolarContext.class, executor, regressionPerCourseGrouperResultReceivers,
                parameterizedDimensionsForCubicRegression);
        Collection<Processor<GroupedDataEntry<GPSFixMovingWithPolarContext>, ?>> regressionPerAngleClusterGrouperResultReceivers = new ArrayList<Processor<GroupedDataEntry<GPSFixMovingWithPolarContext>, ?>>();
        regressionPerAngleClusterGrouperResultReceivers.add(speedRegressionPerAngleClusterProcessor);
        Collection<ParameterizedFunction<?>> parameterizedDimensionsForRegressionPerAngleCluster = new ArrayList<>();
        for (Function<?> function : PolarDataDimensionCollectionFactory
                .getSpeedRegressionPerAngleClusterClusterKeyDimensions()) {
            parameterizedDimensionsForRegressionPerAngleCluster.add(new SimpleParameterizedFunction<>(function,
                    ParameterProvider.NULL));
        }
        Processor<GPSFixMovingWithPolarContext, GroupedDataEntry<GPSFixMovingWithPolarContext>> regressionPerAngleClusterGroupingProcessor = new ParallelMultiDimensionsValueNestingGroupingProcessor<GPSFixMovingWithPolarContext>(
                GPSFixMovingWithPolarContext.class, executor, regressionPerAngleClusterGrouperResultReceivers,
                parameterizedDimensionsForRegressionPerAngleCluster);
        Collection<Processor<GPSFixMovingWithPolarContext, ?>> filteringResultReceivers = new ArrayList<>();
        filteringResultReceivers.add(cubicRegressionPerCourseGroupingProcessor);
        filteringResultReceivers.add(regressionPerAngleClusterGroupingProcessor);
        Processor<GPSFixMovingWithPolarContext, GPSFixMovingWithPolarContext> filteringProcessor = new ParallelFilteringProcessor<GPSFixMovingWithPolarContext>(
                GPSFixMovingWithPolarContext.class, executor, filteringResultReceivers, new PolarFixFilterCriteria(
                        backendPolarSheetGenerationSettings.getPctOfLeadingCompetitorsToInclude()));
        Collection<Processor<GPSFixMovingWithPolarContext, ?>> enrichingResultReceivers = Arrays
                .asList(filteringProcessor);
        AbstractEnrichingProcessor<GPSFixMovingWithOriginInfo, GPSFixMovingWithPolarContext> enrichingProcessor = new AbstractEnrichingProcessor<GPSFixMovingWithOriginInfo, GPSFixMovingWithPolarContext>(
                GPSFixMovingWithOriginInfo.class, GPSFixMovingWithPolarContext.class, executor,
                enrichingResultReceivers) {
            @Override
            protected GPSFixMovingWithPolarContext enrich(GPSFixMovingWithOriginInfo element) {
                GPSFixMovingWithPolarContext result = null;
                result = new GPSFixMovingWithPolarContext(element.getFix(), element.getTrackedRace(),
                        element.getCompetitor(), angleClusterGroup);
                return result;
            }
        };
        Collection<Processor<GPSFixMovingWithOriginInfo, ?>> preFilterResultReceivers = Arrays
                .asList(enrichingProcessor);
        preFilteringProcessor = new ParallelFilteringProcessor<GPSFixMovingWithOriginInfo>(
                GPSFixMovingWithOriginInfo.class, executor, preFilterResultReceivers,
                new FilterCriterion<GPSFixMovingWithOriginInfo>() {
                    @Override
                    public boolean matches(GPSFixMovingWithOriginInfo element) {
                        boolean result = false;
                        if (PolarFixFilterCriteria.isInLeadingCompetitors(element.getTrackedRace(),
                                element.getCompetitor(),
                                backendPolarSheetGenerationSettings.getPctOfLeadingCompetitorsToInclude())) {
                            result = true;
                            final BoatClass boatClass = element.getBoat().getBoatClass();
                            AtomicInteger count = stats.get(boatClass);
                            if (count == null) {
                                count = new AtomicInteger(1);
                                stats.put(boatClass, count);
                            } else {
                                count.getAndIncrement();
                            }
                        }
                        return result;
                    }

                    @Override
                    public Class<GPSFixMovingWithOriginInfo> getElementType() {
                        return GPSFixMovingWithOriginInfo.class;
                    }
                });
    }

    public void addFix(GPSFixMoving fix, Competitor competitor, TrackedRace trackedRace) {
        // don't process fixes while LOADING because wind data is loading at the same time, and
        // unpredictable results may occur due to this
        if (trackedRace.getStatus().getStatus() != TrackedRaceStatusEnum.LOADING) {
            GPSFixMovingWithOriginInfo fixWithOriginInfo = new GPSFixMovingWithOriginInfo(fix, trackedRace, competitor);
            processFix(trackedRace, fixWithOriginInfo);
        }
    }

    private void processFix(TrackedRace trackedRace, GPSFixMovingWithOriginInfo fixWithOriginInfo) {
        if (executor.getQueue().size() >= EXECUTOR_QUEUE_SIZE / 10) { // in this case synchronous execution becomes likely, but
            // we are in a synchronous callback and don't want to spend too much time here in this foreground thread; queue it!
            fixQueue.add(fixWithOriginInfo);
        } else {
            preFilteringProcessor.processElement(fixWithOriginInfo);
        }
    }

    public boolean isCurrentlyActiveOrHasQueue() {
        boolean isActive = executor.getActiveCount() > 0;
        boolean hasQueue = executor.getQueue().size() > 0;
        return isActive || hasQueue;
    }

    /**
     * @param boatClass
     * @param windSpeed
     * @param trueWindAngle
     * @param useLinearRegression
     *            if true uses lin. regression in the wind interval, otherwise arithm. mean
     * @return
     * @throws NotEnoughDataHasBeenAddedException
     */
    public SpeedWithConfidence<Void> estimateBoatSpeed(BoatClass boatClass, Speed windSpeed, Bearing trueWindAngle)
            throws NotEnoughDataHasBeenAddedException {
        return speedRegressionPerAngleClusterProcessor.estimateBoatSpeed(boatClass, windSpeed, trueWindAngle);
    }
    
    public Pair<List<Speed>, Double> estimateWindSpeeds(BoatClass boatClass, Speed boatSpeed, Bearing trueWindAngle)
            throws NotEnoughDataHasBeenAddedException {
        LegType legType;
        if (trueWindAngle.getDegrees() < 70) {
            legType = LegType.UPWIND;
        } else if (trueWindAngle.getDegrees() < 120) {
            legType = LegType.REACHING;
        } else {
            legType = LegType.DOWNWIND;
        }
        Set<SpeedWithBearingWithConfidence<Void>> resultSet = cubicRegressionPerCourseProcessor
                .estimateTrueWindSpeedAndAngleCandidates(boatClass, boatSpeed, legType, Tack.STARBOARD);
        double referenceTwsKnots = 10;
        if (!resultSet.isEmpty()) {
            double bestTwsKnots = Double.MAX_VALUE;
            for (SpeedWithBearingWithConfidence<Void> speedWithBearingWithConfidence : resultSet) {
                double twsKnots = speedWithBearingWithConfidence.getObject().getKnots();
                if (twsKnots > 2 && twsKnots < 20 && Math.abs(10 - twsKnots) < Math.abs(10 - bestTwsKnots)) {
                    bestTwsKnots = twsKnots;
                }
            }
            if (bestTwsKnots > 2 && bestTwsKnots < 20) {
                referenceTwsKnots = bestTwsKnots;
            }
        }
        return speedRegressionPerAngleClusterProcessor.estimateWindSpeeds(boatClass, boatSpeed, trueWindAngle,
                referenceTwsKnots);
    }

    public Set<SpeedWithBearingWithConfidence<Void>> estimateTrueWindSpeedAndAngleCandidates(BoatClass boatClass,
            Speed speedOverGround, LegType legType, Tack tack) {
        Set<SpeedWithBearingWithConfidence<Void>> resultSet = cubicRegressionPerCourseProcessor
                .estimateTrueWindSpeedAndAngleCandidates(boatClass, speedOverGround, legType, tack);
        if (resultSet.isEmpty()) {
            // FALLBACK function if no data was available
            resultSet = getAverageTrueWindSpeedAndAngleCandidatesWithFallbackFunction(boatClass, speedOverGround,
                    legType, tack);
        }
        return resultSet;
    }

    private Set<SpeedWithBearingWithConfidence<Void>> getAverageTrueWindSpeedAndAngleCandidatesWithFallbackFunction(
            BoatClass boatClass, Speed speedOverGround, LegType legType, Tack tack) {

        // The following is an estimation function. It only serves as a fallback. It's the same for all boatclasses and
        // returns
        // default maneuver angles.
        // The function is able to return boat speed values for windspeed values between 5kn and 25kn , which are some
        // kind of realistic
        // for sailing boats. They are taken from the 505 polars we gathered in the races until now.

        Set<SpeedWithBearingWithConfidence<Void>> resultSet = new HashSet<>();
        final int tackFactor = (tack.equals(Tack.PORT)) ? -1 : 1;
        if (legType.equals(LegType.UPWIND)) {
            CubicEquation upWindEquation = new CubicEquation(0.0002, -0.0245, 0.7602, -0.0463
                    - speedOverGround.getKnots());
            int angle = 49 * tackFactor;
            solveAndAddResults(resultSet, upWindEquation, angle);
        } else if (legType.equals(LegType.DOWNWIND)) {
            CubicEquation downWindEquation = new CubicEquation(0.0003, -0.0373, 1.5213, -2.1309
                    - speedOverGround.getKnots());
            int angle = 150 * tackFactor;
            solveAndAddResults(resultSet, downWindEquation, angle);
        }
        return resultSet;
        // return polarDataMiner.estimateTrueWindSpeedAndAngleCandidates(boatClass, speedOverGround, legType, tack);
    }

    private void solveAndAddResults(Set<SpeedWithBearingWithConfidence<Void>> result, CubicEquation equation, int angle) {
        double[] windSpeedCandidates = equation.solve();
        for (int i = 0; i < windSpeedCandidates.length; i++) {
            double windSpeedCandidateInKnots = windSpeedCandidates[i] > 0 ? windSpeedCandidates[i] : 0;
            if (windSpeedCandidateInKnots < 40) {
                result.add(new SpeedWithBearingWithConfidenceImpl<Void>(new KnotSpeedWithBearingImpl(
                        windSpeedCandidateInKnots, new DegreeBearingImpl(angle)), 0.00001, null));
            }
        }
    }

    public PolarSheetsData createFullSheetForBoatClass(BoatClass boatClass) {
        double[] defaultWindSpeeds = backendPolarSheetGenerationSettings.getWindSpeedStepping().getRawStepping();
        Number[][] averagedPolarDataByWindSpeed = new Number[defaultWindSpeeds.length][360];

        Map<Integer, Integer[]> dataCountPerAngleForWindspeed = new HashMap<>();
        Map<Integer, Map<Integer, PolarSheetsHistogramData>> histogramDataMap = new HashMap<>();

        int totalDataCount = 0;

        for (int windIndex = 0; windIndex < defaultWindSpeeds.length; windIndex++) {
            Double windSpeed = defaultWindSpeeds[windIndex];
            Integer[] perAngle = new Integer[360];
            Map<Integer, PolarSheetsHistogramData> perWindSpeed = new HashMap<>();
            for (int angle = 0; angle < 360; angle++) {
                SpeedWithConfidence<Void> speedWithConfidence;
                try {
                    int convertedAngle = convertAngleIfNecessary(angle);
                    SpeedWithConfidence<Void> regressionResult = speedRegressionPerAngleClusterProcessor
                            .estimateBoatSpeed(boatClass, new KnotSpeedImpl(windSpeed), new DegreeBearingImpl(
                                    convertedAngle));
                    if (regressionResult.getConfidence() > 0.1) {
                        speedWithConfidence = regressionResult;
                    } else {
                        // Low confidence. So put in 0 speed for chart
                        speedWithConfidence = new SpeedWithConfidenceImpl<Void>(new KnotSpeedImpl(0),
                                regressionResult.getConfidence(), null);
                    }
                } catch (NotEnoughDataHasBeenAddedException e) {
                    // No data so put in a 0 speed with 0 confidence
                    speedWithConfidence = new SpeedWithConfidenceImpl<Void>(new KnotSpeedImpl(0), 0, null);
                }

                averagedPolarDataByWindSpeed[windIndex][angle] = speedWithConfidence.getObject().getKnots();
                int dataCount = 200; /* FIXME */

                totalDataCount = totalDataCount + dataCount;
                // FIXME hard coded
                double coefficiantOfVariation = 0.8;
                double confidenceMeasure = 0.5;

                PolarSheetsHistogramDataImpl polarSheetsHistogramDataImpl = createEmptyHistogramData(perAngle, angle,
                        dataCount, coefficiantOfVariation, confidenceMeasure);
                perWindSpeed.put(angle, polarSheetsHistogramDataImpl);
            }
            histogramDataMap.put(windIndex, perWindSpeed);
            dataCountPerAngleForWindspeed.put(windIndex, perAngle);
        }
        PolarSheetsData data = new PolarSheetsDataImpl(averagedPolarDataByWindSpeed, totalDataCount,
                dataCountPerAngleForWindspeed, backendPolarSheetGenerationSettings.getWindSpeedStepping(),
                histogramDataMap);
        return data;
    }

    private int convertAngleIfNecessary(int angle) {
        int convertedAngle = angle;
        if (angle > 180) {
            convertedAngle = angle - 360;
        }
        return convertedAngle;
    }

    private PolarSheetsHistogramDataImpl createEmptyHistogramData(Integer[] perAngle, int angle, int dataCount,
            double coefficiantOfVariation, double confidenceMeasure) {
        perAngle[angle] = dataCount;
        Number[] xValues = {};
        Number[] yValues = {};
        ;
        Map<String, Integer[]> yValuesByGaugeIds = new HashMap<>();
        Map<String, Integer[]> yValuesByDay = new HashMap<>();
        Map<String, Integer[]> yValuesByDayAndGaugeId = new HashMap<>();
        PolarSheetsHistogramDataImpl polarSheetsHistogramDataImpl = new PolarSheetsHistogramDataImpl(angle, xValues,
                yValues, yValuesByGaugeIds, yValuesByDay, yValuesByDayAndGaugeId, dataCount, coefficiantOfVariation);
        polarSheetsHistogramDataImpl.setConfidenceMeasure(confidenceMeasure);
        return polarSheetsHistogramDataImpl;
    }

    public Set<BoatClass> getAvailableBoatClasses() {
        return speedRegressionPerAngleClusterProcessor.getAvailableBoatClasses();
    }

    public int[] getDataCountsForWindSpeed(BoatClass boatClass, Speed windSpeed, int startAngleInclusive,
            int endAngleExclusive) {
        int[] dataCounts = new int[360];
        for (int angle = 0; angle < 360; angle++) {
            if (angle >= startAngleInclusive && angle < endAngleExclusive) {
                dataCounts[angle] = 0; /* FIXME */
            } else {
                dataCounts[angle] = -1;
            }
        }
        return dataCounts;
    }

    public SpeedWithBearingWithConfidence<Void> getAverageSpeedAndCourseOverGround(BoatClass boatClass,
            Speed windSpeed, LegType legType) throws NotEnoughDataHasBeenAddedException {
        SpeedWithBearingWithConfidence<Void> averageSpeedAndCourseOverGround = null;
        averageSpeedAndCourseOverGround = cubicRegressionPerCourseProcessor.getAverageSpeedAndCourseOverGround(
                boatClass, windSpeed, legType);
        return averageSpeedAndCourseOverGround;
    }

    public PolynomialFunction getSpeedRegressionFunction(BoatClass boatClass, LegType legType)
            throws NotEnoughDataHasBeenAddedException {
        return cubicRegressionPerCourseProcessor.getSpeedRegressionFunction(boatClass, legType);
    }

    public PolynomialFunction getAngleRegressionFunction(BoatClass boatClass, LegType legType)
            throws NotEnoughDataHasBeenAddedException {
        return cubicRegressionPerCourseProcessor.getAngleRegressionFunction(boatClass, legType);
    }

    public PolynomialFunction getSpeedRegressionFunction(BoatClass boatClass, double trueWindAngle)
            throws NotEnoughDataHasBeenAddedException {
        return speedRegressionPerAngleClusterProcessor.getSpeedRegressionFunction(boatClass, trueWindAngle);
    }

    public void raceFinishedTracking(final TrackedRace race) {
        processRacesThatFinishedLoadingExecutor.execute(()->{ // no Subject association necessary here
            logger.info("All queued fixes for newly loaded race will process now. "
                    + (race.getRace() != null ? race.getRace().getName() : race.getRaceIdentifier().getRaceName()));
            for (final Competitor competitor : race.getRace().getCompetitors()) {
                final GPSFixTrack<Competitor, GPSFixMoving> track = race.getTrack(competitor);
                // it is necessary to release the track's lock before calling processElement
                // because processElement will transitively cause obtaining the course lock,
                // and other methods will first obtain the course and then the track lock, leading
                // to a deadlock. See also bug 4297.
                final List<GPSFixMoving> fixes = new ArrayList<>();
                track.lockForRead();
                try {
                    for (final GPSFixMoving fix : track.getFixes()) {
                        fixes.add(fix);
                    }
                } finally {
                    track.unlockAfterRead();
                }
                for (final GPSFixMoving fix : fixes) {
                    preFilteringProcessor.processElement(new GPSFixMovingWithOriginInfo(fix, race, competitor));
                }
            }
            logger.info("Finished injecting fixes for race "
                    + (race.getRace() != null ? race.getRace().getName() : race.getRaceIdentifier().getRaceName())
                    + "; stats: " + stats);
        });
    }

    public void registerListener(BoatClass boatClass, PolarsChangedListener listener) {
        Set<PolarsChangedListener> listenersForBoatClass = listeners.get(boatClass);
        if (listenersForBoatClass == null) {
            Map<PolarsChangedListener, Boolean> mapForConcurrency = new ConcurrentHashMap<>();
            listenersForBoatClass = Collections.newSetFromMap(mapForConcurrency);
            listeners.put(boatClass, listenersForBoatClass);
        }
        listenersForBoatClass.add(listener);
    }

    public void unregisterListener(BoatClass boatClass, PolarsChangedListener listener) {
        Set<PolarsChangedListener> listenersForBoatClass = listeners.get(boatClass);
        if (listenersForBoatClass != null) {
            listenersForBoatClass.remove(listener);
        }
    }

    public CubicRegressionPerCourseProcessor getCubicRegressionPerCourseProcessor() {
        return cubicRegressionPerCourseProcessor;
    }

    public SpeedRegressionPerAngleClusterProcessor getSpeedRegressionPerAngleClusterProcessor() {
        return speedRegressionPerAngleClusterProcessor;
    }

    public PolarSheetGenerationSettings getPolarSheetGenerationSettings() {
        return backendPolarSheetGenerationSettings;
    }
}
