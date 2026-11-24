package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.TextAlign;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewMethods;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewOnAddHandler;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * A google map overlay based on a HTML5 canvas for drawing a course area circle with a center point that has
 * the course area name written next to it.
 */
public class CourseAreaCircleOverlay extends CanvasOverlayV3 {
    private static final double DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH = 3.0;
    private static final CssColor DEFAULT_COURSE_AREA_CIRCLE_COLOR = CssColor.make("rgba(37,158,255,0.75)");
    private static final int CENTER_CROSS_SIZE_IN_PIXELS = 10;
    private static final int BOUNDARY_POINT_COUNT = 128;
    
    /**
     * The course area whose circle to draw
     */
    private final CourseAreaDTO courseArea;
    private final StringMessages stringMessages;
    
    public CourseAreaCircleOverlay(MapWidget map, int zIndex, CourseAreaDTO courseArea, CoordinateSystem coordinateSystem, StringMessages stringMessages) {
        super(map, zIndex, coordinateSystem);
        this.courseArea = courseArea;
        this.stringMessages = stringMessages;
        setCanvasSize(50, 50);
    }
    
    @Override
    protected void draw() {
        if (getMapProjection() != null && courseArea != null && getPosition() != null && courseArea.getRadius() != null) {
            getCanvas().setTitle(getTitle());
            final Point center = getMapProjection().fromLatLngToDivPixel(coordinateSystem.toLatLng(getPosition()));
            final Context2d context2d = getCanvas().getContext2d();
            context2d.setFont("16px arial");
            final double halfWidth = Math.max(CENTER_CROSS_SIZE_IN_PIXELS / 2.0,
                    context2d.measureText(courseArea.getName()).getWidth() / 2);
            double minX = center.getX() - halfWidth;
            double maxX = center.getX() + halfWidth;
            double minY = center.getY() - CENTER_CROSS_SIZE_IN_PIXELS / 2.0;
            // Keep room for the label below the cross, including the font's descenders.
            double maxY = center.getY() + 2 * CENTER_CROSS_SIZE_IN_PIXELS + 4;
            final Point[] boundary = new Point[BOUNDARY_POINT_COUNT];
            for (int index = 0; index < boundary.length; index++) {
                final Position position = getPosition().translateGreatCircle(
                        new DegreeBearingImpl(index * 360.0 / boundary.length), courseArea.getRadius());
                final Point pixel = getMapProjection().fromLatLngToDivPixel(coordinateSystem.toLatLng(position));
                boundary[index] = pixel;
                minX = Math.min(minX, pixel.getX());
                maxX = Math.max(maxX, pixel.getX());
                minY = Math.min(minY, pixel.getY());
                maxY = Math.max(maxY, pixel.getY());
            }
            final double padding = DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH / 2 + 1;
            final double left = Math.floor(minX - padding);
            final double top = Math.floor(minY - padding);
            final double width = Math.ceil(maxX + padding) - left;
            final double height = Math.ceil(maxY + padding) - top;
            // Validate before narrowing to int: extreme projections can overflow integer dimensions or area.
            if (!Double.isFinite(left) || !Double.isFinite(top) || !Double.isFinite(width) || !Double.isFinite(height)
                    || width <= 0 || height <= 0 || width >= 2 << 12 || height >= 2 << 12 || width * height > 1 << 26) {
                GWT.log("Course area circle canvas for " + courseArea.getName() + " has invalid or excessive bounds ("
                        + width + "x" + height + "). Not drawing.");
                setCanvasSize(/* newWidthInPx */ 0, /* newHeightInPx */ 0);
            } else {
                setCanvasSize((int) width, (int) height);
                final double centerX = center.getX() - left;
                final double centerY = center.getY() - top;
                // Project the whole geographic boundary; a screen-space arc cannot follow map perspective.
                context2d.setStrokeStyle(DEFAULT_COURSE_AREA_CIRCLE_COLOR);
                context2d.setFillStyle(DEFAULT_COURSE_AREA_CIRCLE_COLOR);
                context2d.setLineWidth(DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH);
                context2d.beginPath();
                context2d.moveTo(boundary[0].getX() - left, boundary[0].getY() - top);
                for (int index = 1; index < boundary.length; index++) {
                    context2d.lineTo(boundary[index].getX() - left, boundary[index].getY() - top);
                }
                context2d.closePath();
                context2d.stroke();
                context2d.beginPath();
                context2d.setLineWidth(1.0); // draw only a fine 1px center cross
                context2d.moveTo(centerX - CENTER_CROSS_SIZE_IN_PIXELS / 2.0, centerY);
                context2d.lineTo(centerX + CENTER_CROSS_SIZE_IN_PIXELS / 2.0, centerY);
                context2d.moveTo(centerX, centerY - CENTER_CROSS_SIZE_IN_PIXELS / 2.0);
                context2d.lineTo(centerX, centerY + CENTER_CROSS_SIZE_IN_PIXELS / 2.0);
                context2d.stroke();
                context2d.setTextAlign(TextAlign.CENTER);
                context2d.setFont("16px arial");
                context2d.fillText(courseArea.getName(), centerX, centerY + 2 * CENTER_CROSS_SIZE_IN_PIXELS);
                setCanvasPosition(left, top);
            }
        }
    }

    @Override
    protected OverlayViewOnAddHandler getOnAddHandler() {
        return new OverlayViewOnAddHandler() {
            @Override
            public void onAdd(OverlayViewMethods methods) {
                methods.getPanes().getMapPane().appendChild(canvas.getElement());
                CourseAreaCircleOverlay.this.onAttach();
            }
        };
    }

    private String getTitle() {
        return stringMessages.courseArea()+": "+courseArea.getName();
    }
    
    public CourseAreaDTO getCourseArea() {
        return courseArea;
    }

    /**
     * The real-world position where the mark displayed by this overlay is located
     */
    public Position getPosition() {
        return getCourseArea().getCenterPosition();
    }

    /**
     * The {@link LatLng} position where the mark is shown on the map. This is transformed through the
     * {@link CoordinateSystem} in place for the {@link RaceMap}.
     */
    public LatLng getMarkLatLngPosition() {
        return coordinateSystem.toLatLng(getPosition());
    }
}
