package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.base.Size;
import com.google.gwt.maps.client.overlays.MapCanvasProjection;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegType;
import com.sap.sailing.gwt.ui.shared.racemap.BoatClassVectorGraphics;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;
import com.sap.sse.common.Color;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MeterDistance;

/**
 * A google map overlay based on a HTML5 canvas for drawing boats (images)
 * The boats will be zoomed/scaled according to the current map state and rotated according to the bearing of the boat.
 */
public class BoatOverlay extends CanvasOverlayV3 {

    /** 
     * The boat class
     */
    private final BoatClassDTO boatClass;
    
    /**
     * The current GPS fix used to draw the boat.
     */
    private GPSFixDTOWithSpeedWindTackAndLegType boatFix;

    // MapCanvasProjection exposes points but no local transform, so nearby probes approximate its affine basis.
    // Keep this distance short: long probes sample perspective curvature and visibly distort boat proportions.
    private static final Distance SCREEN_HEADING_PROBE_DISTANCE = new MeterDistance(1);
    // shortcut: models above 8192px are culled; use clipped vector geometry if closer views must render them.
    private static final int MAX_PROJECTED_EDGE = 8192;
    private static final int MAX_BACKING_EDGE = 1024;

    private int canvasWidth;
    private int canvasHeight;

    private Color color; 

    private final BoatClassVectorGraphics boatVectorGraphics;

    private LegType lastLegType;
    private Tack lastTack;
    private Boolean lastSelected;
    private Integer lastWidth;
    private Integer lastHeight;
    private Size lastScale;
    private Color lastColor;
    private DisplayMode lastDisplayMode;
    
    public static enum DisplayMode { DEFAULT, SELECTED, NOT_SELECTED };
    private DisplayMode displayMode;

    public BoatOverlay(final MapWidget map, int zIndex, final BoatDTO boatDTO, Color color, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        this.boatClass = boatDTO.getBoatClass();
        this.color = color;
        getCanvas().getElement().setAttribute("data-map-oriented", "true");
        boatVectorGraphics = BoatClassVectorGraphicsResolver.resolveBoatClassVectorGraphics(boatClass.getName());
    }
    
    @Override
    protected void draw() {
        final MapCanvasProjection projection = getMapProjection();
        if (projection != null && boatFix != null) {
            final LatLng position = coordinateSystem.toLatLng(boatFix.position);
            final Point screenCenter = projection.fromLatLngToContainerPixel(position);
            final int viewportWidth = map.getDiv().getClientWidth();
            final int viewportHeight = map.getDiv().getClientHeight();
            final boolean nearViewport = Double.isFinite(screenCenter.getX()) && Double.isFinite(screenCenter.getY())
                    && screenCenter.getX() >= -MAX_PROJECTED_EDGE && screenCenter.getY() >= -MAX_PROJECTED_EDGE
                    && screenCenter.getX() <= viewportWidth + MAX_PROJECTED_EDGE && screenCenter.getY() <= viewportHeight + MAX_PROJECTED_EDGE;
            if (!nearViewport || !drawProjectedBoat(projection, position, screenCenter, viewportWidth, viewportHeight)) {
                clearBoatCanvas();
            }
        }
    }

    private boolean drawProjectedBoat(final MapCanvasProjection projection, final LatLng position,
            final Point screenCenter, final int viewportWidth, final int viewportHeight) {
        final Point center = projection.fromLatLngToDivPixel(position);
        final double trueHeadingInDegrees = boatFix.optionalTrueHeading != null ? boatFix.optionalTrueHeading.getDegrees()
                : (boatFix.speedWithBearing == null ? 0 : boatFix.speedWithBearing.bearingInDegrees);
        final Point forward = projection.fromLatLngToDivPixel(coordinateSystem.toLatLng(
                boatFix.position.translateRhumb(new DegreeBearingImpl(trueHeadingInDegrees), SCREEN_HEADING_PROBE_DISTANCE)));
        final Point starboard = projection.fromLatLngToDivPixel(coordinateSystem.toLatLng(
                boatFix.position.translateRhumb(new DegreeBearingImpl(trueHeadingInDegrees + 90), SCREEN_HEADING_PROBE_DISTANCE)));
        final double forwardX = forward.getX() - center.getX(), forwardY = forward.getY() - center.getY();
        final double starboardX = starboard.getX() - center.getX(), starboardY = starboard.getY() - center.getY();
        final double sum = forwardX * forwardX + forwardY * forwardY + starboardX * starboardX + starboardY * starboardY;
        final double determinant = forwardX * starboardY - forwardY * starboardX;
        final double normalization = Math.sqrt((sum + Math.sqrt(Math.max(0,
                sum * sum - 4 * determinant * determinant))) / 2);
        boolean rendered = false;
        if (Double.isFinite(normalization) && normalization > 0.000001 && determinant > 0) {
            final Util.Pair<Size, Size> scaleAndSize = getBoatScaleAndSize(boatClass, normalization);
            final Size logicalScale = scaleAndSize.getA();
            final double logicalWidth = scaleAndSize.getB().getWidth(), logicalHeight = scaleAndSize.getB().getHeight();
            final boolean validSize = Double.isFinite(logicalWidth) && Double.isFinite(logicalHeight)
                    && logicalWidth > 0 && logicalHeight > 0 && logicalWidth <= MAX_PROJECTED_EDGE && logicalHeight <= MAX_PROJECTED_EDGE
                    && Double.isFinite(logicalScale.getWidth()) && Double.isFinite(logicalScale.getHeight());
            if (validSize) {
                final double extentX = (Math.abs(forwardX / normalization) * logicalWidth
                        + Math.abs(starboardX / normalization) * logicalHeight) / 2;
                final double extentY = (Math.abs(forwardY / normalization) * logicalWidth
                        + Math.abs(starboardY / normalization) * logicalHeight) / 2;
                if (screenCenter.getX() + extentX >= 0 && screenCenter.getY() + extentY >= 0
                        && screenCenter.getX() - extentX <= viewportWidth && screenCenter.getY() - extentY <= viewportHeight) {
                    // Cap raster memory, not the visible geometry. CSS restores the same logical size and pitch.
                    final double rasterScale = Math.min(1, Math.min(MAX_BACKING_EDGE / logicalWidth, MAX_BACKING_EDGE / logicalHeight));
                    final Size scale = rasterScale == 1 ? logicalScale : Size.newInstance(
                            logicalScale.getWidth() * rasterScale, logicalScale.getHeight() * rasterScale);
                    canvasWidth = Math.min(MAX_BACKING_EDGE, (int) (logicalWidth * rasterScale));
                    canvasHeight = Math.min(MAX_BACKING_EDGE, (int) (logicalHeight * rasterScale));
                    getCanvas().getElement().setPropertyDouble(/* name */ "__boatLogicalWidth", logicalWidth);
                    getCanvas().getElement().setPropertyDouble(/* name */ "__boatLogicalHeight", logicalHeight);
                    if (lastWidth == null || canvasWidth != lastWidth || lastHeight == null || canvasHeight != lastHeight) {
                        setCanvasSize(canvasWidth, canvasHeight);
                    }
                    if (needToDraw(boatFix.legType, boatFix.tack, isSelected(), canvasWidth, canvasHeight, scale, color, displayMode)) {
                        // A failed paint must not leave a cache claiming the cleared bitmap is still valid.
                        lastWidth = null;
                        lastHeight = null;
                        boatVectorGraphics.drawBoatToCanvas(getCanvas().getContext2d(), boatFix.legType, boatFix.tack,
                                getDisplayMode(), canvasWidth, canvasHeight, scale, color);
                        lastLegType = boatFix.legType;
                        lastTack = boatFix.tack;
                        lastSelected = isSelected();
                        lastWidth = canvasWidth;
                        lastHeight = canvasHeight;
                        lastScale = scale;
                        lastColor = color;
                        lastDisplayMode = displayMode;
                    }
                    updateDrawingMatrixAndSetCanvasTransform(forwardX / normalization / rasterScale,
                            forwardY / normalization / rasterScale, starboardX / normalization / rasterScale,
                            starboardY / normalization / rasterScale);
                    setCanvasPosition(center.getX() - getCanvas().getCoordinateSpaceWidth() / 2,
                            center.getY() - getCanvas().getCoordinateSpaceHeight() / 2);
                    rendered = true;
                }
            }
        }
        return rendered;
    }

    private void clearBoatCanvas() {
        if (getCanvas().getCoordinateSpaceWidth() != 0 || getCanvas().getCoordinateSpaceHeight() != 0) {
            setCanvasSize(/* newWidthInPx */ 0, /* newHeightInPx */ 0);
        }
        canvasWidth = 0;
        canvasHeight = 0;
        lastWidth = null;
        lastHeight = null;
        lastScale = null;
    }

    /**
     * Compares the drawing parameters to {@link #lastLegType} and the other <code>last...</code>. If anything has
     * changed, the result is <code>true</code>.
     */
    private boolean needToDraw(LegType legType, Tack tack, boolean isSelected, double width, double height,
            Size scaleFactor, Color color, DisplayMode displayMode) {
        return lastLegType == null || lastLegType != legType || lastTack == null || lastTack != tack
                || lastSelected == null || lastSelected != isSelected || lastWidth == null || lastWidth != width
                || lastHeight == null || lastHeight != height || lastScale == null || !lastScale.equals(scaleFactor)
                || lastColor == null || !lastColor.equals(color) || lastDisplayMode == null
                || !lastDisplayMode.equals(displayMode);
    }

    public void setBoatFix(GPSFixDTOWithSpeedWindTackAndLegType boatFix, long timeForPositionTransitionMillis) {
        updateTransition(timeForPositionTransitionMillis);
        this.boatFix = boatFix;
    }

    public Util.Pair<Size, Size> getBoatScaleAndSize(BoatClassDTO boatClass, double pixelsPerMeter) {
        final double naturalHullLengthInPixels = boatClass.getHullLength().getMeters() * pixelsPerMeter;
        final double naturalBeamInPixels = boatClass.getHullBeam().getMeters() * pixelsPerMeter;
        final double minimumMultiplier = Math.max(1.0, Math.max(
                boatVectorGraphics.getMinHullLengthInPx() / naturalHullLengthInPixels,
                boatVectorGraphics.getMinBeamInPx() / naturalBeamInPixels));
        final double boatHullScaleFactor = naturalHullLengthInPixels * minimumMultiplier /
                boatVectorGraphics.getHullLengthInPx();
        final double boatBeamScaleFactor = naturalBeamInPixels * minimumMultiplier /
                boatVectorGraphics.getBeamInPx();
        final double scaledWidthSize = boatVectorGraphics.getOverallLengthInPx() * boatHullScaleFactor;
        final double scaledBeamSize = boatVectorGraphics.getOverallLengthInPx() * boatBeamScaleFactor;
        return new Util.Pair<Size, Size>(Size.newInstance(boatHullScaleFactor, boatBeamScaleFactor),
                Size.newInstance(scaledWidthSize * 1.5, scaledBeamSize * 1.5));
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

}
