package com.sap.sailing.domain.polars;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.confidence.BearingWithConfidence;

/**
 * Public Facade interface granting clients access to the polar sheets of {@link BoatClass}es. A boat's "polar sheet"
 * (sometimes also referred to as a "VPP" (velocity prediction program)) makes a prediction how fast the boat will
 * sail at a given true wind angle and a given true wind speed.<p>
 * 
 * This service uses a {@link MovingAverageProcessor} for more advanced analysis. Its methods are facaded in this interface for
 * central access.<p>
 * The interesting methods for a client are {@link #getSpeed(BoatClass, Speed, Bearing, boolean)} if data for a specific angle is
 * needed and {@link #getAverageSpeedWithBearing(BoatClass, Speed, LegType, Tack, boolean)} 
 * which also returns the average angle for the parameters provided.
 * 
 * @author Frederik Petersen (D054528)
 * @author Axel Uhl (D043530)
 * 
 */
public interface PolarDataService {

    /**
     * 
     * @param boatClass
     * @param windSpeed
     * @param trueWindAngle
     *            Boat's direction relative to the wind. either in -180 -> +180 or 0 -> 359 degrees interval. The true wind!
     * @return The speed the boat is moving at for the specified wind and bearing according to the polar diagram.
     * @throws NotEnoughDataHasBeenAddedException
     */
    SpeedWithConfidence<Void> getSpeed(BoatClass boatClass, Speed windSpeed, Bearing trueWindAngle)
            throws NotEnoughDataHasBeenAddedException;

    /**
     * 
     * @return The {@link BoatClass}es for which there are polar sheets available via
     *         {@link PolarDataService#getPolarSheetForBoatClass(BoatClass)}
     */
    Set<BoatClass> getAllBoatClassesWithPolarSheetsAvailable();

    /**
     * To be called in an appropriate listener. 
     * Starting point for fixes entering the backend polar data mining pipeline.
     * 
     * @param fix
     * @param competitor
     * @param createdTrackedRace
     */
    void competitorPositionChanged(GPSFixMoving fix, Competitor competitor, TrackedRace createdTrackedRace);

    /**
     * From a boat's speed over ground and assuming values for <code>boatClass</code>, the <code>tack</code> the boat is
     * currently sailing on, and the <code>legType</code>, this method estimates the true wind speed candidates at which
     * the boat may most likely have been sailing under these conditions.
     * 
     * The confidence of the returned candidates is derived from the average confidence of the underlying fixes,
     * distance measures and the amount of underlying data.
     * 
     * Multiple candidates are possible, because we cannot guarantee a reversible function (boatspeed over windspeed).
     * 
     * @return set of wind candidates with confidence, empty set if no were found (due to insufficient underlying data)
     */
    Set<SpeedWithBearingWithConfidence<Void>> getAverageTrueWindSpeedAndAngleCandidates(BoatClass boatClass, Speed speedOverGround, LegType legType, Tack tack);

    /**
     * @param intoTackSpeed speed before the tack maneuver
     * @param intoJibeSpeed speed before the jibe maneuver
     * @param boatClass the boat class for which to calculate the confidence
     * @return the likelihood of the ratio between tack and jibe speed, measured at the beginning
     * of the maneuver; for example, if the tack speed is much faster than the jibe speed, this seems
     * suspicious and will for most polar sheets for most boat classes result in a very low confidence.
     * However, if the tack/jibe speed ratio matches up well with what the polar diagram says, a high
     * probability will result. The resulting value will be between 0..1 (inclusive).
     */
    double getConfidenceForTackJibeSpeedRatio(Speed intoTackSpeed, Speed intoJibeSpeed, BoatClass boatClass);

    /**
     * Assuming a boat of the <code>boatClass</code> sailed at <code>speedOverGround</code> and during the maneuver
     * changed course by <code>courseChange</code>, how likely was that a maneuver of type <code>maneuverType</code>?
     * 
     * @return a probability between 0..1 (inclusive) in the {@link Pair#getA() first} component, and the true wind
     *         speed and true wind angle in the {@link Pair#getB() second} component.
     */
    Pair<Double, SpeedWithBearingWithConfidence<Void>> getManeuverLikelihoodAndTwsTwa(BoatClass boatClass, Speed speedOverGround, double courseChangeDeg, ManeuverType maneuverType);

    /**
     * This method is not intended to be used directly apart from debugging purposes. If you intend to use the polar service please 
     * use the {@link #getAverageSpeedWithBearing(BoatClass, Speed, LegType, Tack, boolean)} method.
     * 
     * @param boatClass
     * @param legType
     * @return The estimating function for the tack and legtype combination estimating boatspeed over windspeed for the
     *         given boat class. All values in kn.
     * @throws NotEnoughDataHasBeenAddedException 
     */
    PolynomialFunction getSpeedRegressionFunction(BoatClass boatClass, LegType legType) throws NotEnoughDataHasBeenAddedException;
    
    /**
     * This method is not intended to be used directly apart from debugging purposes. If you intend to use the polar service please 
     * use the {@link #getAverageSpeedWithBearing(BoatClass, Speed, LegType, Tack, boolean)} method.
     * 
     * @param boatClass
     * @param legType
     * @return The estimating function for the tack and legtype combination estimating true wind angle over windspeed for the
     *         given boat class. TWA in degrees and windspeeds in knots.
     * @throws NotEnoughDataHasBeenAddedException 
     */
    PolynomialFunction getAngleRegressionFunction(BoatClass boatClass, LegType legType) throws NotEnoughDataHasBeenAddedException;

    /**
     * This method is not intended to be used directly apart from debugging purposes. If you intend to use the polar service please 
     * use {@link #getSpeed(BoatClass, Speed, Bearing)}.
     * 
     * @param boatClass
     * @param trueWindAngle
     * @return The estimating function for the true wind angle estimating boatspeed over windspeed for the
     *         given boat class. All values in kn.
     * @throws NotEnoughDataHasBeenAddedException
     */
    PolynomialFunction getSpeedRegressionFunction(BoatClass boatClass, double trueWindAngle) throws NotEnoughDataHasBeenAddedException;
    
    /**
     * For Upwind and Downwind returns the typical angle needed for a maneuver.
     * 
     * @param boatClass
     * @param maneuverType
     *            needs to be Tack or Jibe
     * @param windSpeed
     *            Since the maneuver angle is windSpeed depended, supply it here
     * @return angle with confidence as supplied by polars. Angle is always >= 0
     * @throws NotEnoughDataHasBeenAddedException
     */
    BearingWithConfidence<Void> getManeuverAngle(BoatClass boatClass, ManeuverType maneuverType, Speed windSpeed)
            throws NotEnoughDataHasBeenAddedException;
    
    void raceFinishedLoading(TrackedRace race);

    /**
     * See {@link #getAverageSpeedWithBearing(BoatClass, Speed, LegType, Tack, boolean)}
     * Always use regression
     */
    SpeedWithBearingWithConfidence<Void> getAverageSpeedWithTrueWindAngle(BoatClass boatClass, Speed windSpeed,
            LegType legType, Tack tack) throws NotEnoughDataHasBeenAddedException;

    void insertExistingFixes(TrackedRace trackedRace);
    
    /**
     * @param boatClass When polars of this boat class change, the listener will be notified. May not be null.
     * @param listener may not be null
     */
    void registerListener(BoatClass boatClass, PolarsChangedListener listener);
    
    void unregisterListener(BoatClass boatClass, PolarsChangedListener listener);

    /**
     * Announces a base {@link DomainFactory} to this polar data service that it now can start using. Calling this
     * method with a non-{@code null} parameter will unblock all {@link #runWithDomainFactory} calls.
     */
    void registerDomainFactory(DomainFactory domainFactory);
    
    /**
     * When called, the method blocks until a {@link DomainFactory} has been {@link #registerDomainFactory(DomainFactory) registered}
     * with this service, then lets the {@code consumer} accept that domain factory.
     */
    void runWithDomainFactory(Consumer<DomainFactory> consumer) throws InterruptedException;

    Map<BoatClass, Long> getFixCountPerBoatClass();

    SpeedWithBearingWithConfidence<Void> getClosestTwaTws(ManeuverType type, Speed speedAtManeuverStart,
            double courseChangeDeg, BoatClass boatClass);

    double getManeuverAngleInDegreesFromTwa(ManeuverType maneuverType, Bearing twa);

    Pair<List<Speed>, Double> estimateWindSpeeds(BoatClass boatClass, Speed boatSpeed, Bearing trueWindAngle)
            throws NotEnoughDataHasBeenAddedException;
}
