package com.sap.sailing.gwt.ui.simulator;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.TextAlign;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.overlays.Circle;
import com.google.gwt.maps.client.overlays.CircleOptions;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.RGBColor;

public class RegattaAreaCanvasOverlay extends FullCanvasOverlay {

    private SimulatorMap simulatorMap;
    private RaceCourseCanvasOverlay raceCourseCanvasOverlay;
    private VenueDescriptor venue; 
    private CourseAreaDescriptor currentCourseArea = null;
    
    private boolean initial;
    private double raceBearing = 0.0;
    private double diffBearing = 0.0;
    private final List<Circle> areaCircles = new ArrayList<>();
    private int lastStrokeWeight = -1;

    public RegattaAreaCanvasOverlay(MapWidget map, int zIndex, char event, final SimulatorMap simulatorMap, CoordinateSystem coordinateSystem) {

    	super(map, zIndex, coordinateSystem);

    	this.simulatorMap = simulatorMap;
        this.venue = VenueDescriptorFactory.createVenue(event, coordinateSystem);
        this.currentCourseArea = this.venue.getDefaultCourseArea();        

    }

    @Override
    public void addToMap() {
        super.addToMap();
        
        if (areaCircles.isEmpty()) {
            int counter = 0;
            final int background = simulatorMap.getWindParams().isShowStreamlets() ? 80 : 222;
            for (final CourseAreaDescriptor courseArea : venue.getCourseAreas()) {
                final Circle circle = simulatorMap.drawCircleFromRadius(counter++, courseArea);
                final RGBColor color = new RGBColor(courseArea.getColor());
                final CircleOptions options = CircleOptions.newInstance();
                options.setFillColor(blendWithBackground(color, background, /* opacity */ 0.5));
                options.setFillOpacity(1);
                options.setStrokeColor(blendWithBackground(color, background, /* opacity */ 0.8));
                options.setStrokeOpacity(1);
                circle.setOptions(options);
                areaCircles.add(circle);
            }
        } else {
            for (final Circle circle : areaCircles) {
                circle.setMap(map);
            }
        }
        
        map.panTo(venue.getCenterPos());
        this.initial = true;
    }

    @Override
    protected void draw() {
    	
        super.draw();

        if(getMapProjection() != null) {

            clearCanvas();
            drawRegattaAreas();

            if (this.initial) {
            	updateRaceCourse(0, 0);
            	this.initial = false;
            }
            
        }
    }

    private void clearCanvas() {
        canvas.getContext2d().clearRect(0.0 /* canvas.getAbsoluteLeft() */, 0.0/* canvas.getAbsoluteTop() */,
                canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
    }

    @Override
    public void removeFromMap() {
        super.removeFromMap();
        for (final Circle circle : areaCircles) {
            circle.setMap(/* map */ null);
        }
    }

    private String blendWithBackground(final RGBColor color, final int background, final double opacity) {
        final Util.Triple<Integer, Integer, Integer> rgb = color.getAsRGB();
        return new RGBColor((int) Math.round(opacity * rgb.getA() + (1 - opacity) * background),
                (int) Math.round(opacity * rgb.getB() + (1 - opacity) * background),
                (int) Math.round(opacity * rgb.getC() + (1 - opacity) * background)).getAsHtml();
    }

    protected void drawRegattaAreas() {
        final int strokeWeight = Math.max(1, (int) Math.round(Math.pow(2.0, (getMap().getZoom() - 10.0) / 2.0)));
        if (strokeWeight != lastStrokeWeight) {
            final CircleOptions options = CircleOptions.newInstance();
            options.setStrokeWeight(strokeWeight);
            for (final Circle circle : areaCircles) {
                circle.setOptions(options);
            }
            lastStrokeWeight = strokeWeight;
        }
        final Context2d context2d = canvas.getContext2d();
        for (final CourseAreaDescriptor courseArea : venue.getCourseAreas()) {
            drawCourseAreaLabel(courseArea, context2d);
        }
    }

    protected void updateRaceCourse(int type, double bearing) {

        if (type == 1) {
            raceBearing = bearing;
            simulatorMap.getWindNeedleCanvasOverlay().setBearing(raceBearing + 180.0);
        } else if (type == 2) {
            diffBearing = bearing;
        }
        if (currentCourseArea != null) {
            simulatorMap.getMainPanel().setUpdateButtonEnabled(true);
            LatLng startPoint;
            LatLng endPoint;
            if (raceCourseCanvasOverlay.raceCourseDirection == SailingSimulatorConstants.LegTypeDownwind) {
                startPoint = getDistantPoint(currentCourseArea.getCenterPos(), 0.9 * currentCourseArea.getRadius(), 0.0 + raceBearing - diffBearing);
                endPoint = getDistantPoint(currentCourseArea.getCenterPos(), 0.9 * currentCourseArea.getRadius(), 180.0 + raceBearing - diffBearing);
            } else {
                startPoint = getDistantPoint(currentCourseArea.getCenterPos(), 0.9 * currentCourseArea.getRadius(), 180.0 + raceBearing - diffBearing);
                endPoint = getDistantPoint(currentCourseArea.getCenterPos(), 0.9 * currentCourseArea.getRadius(), 0.0 + raceBearing - diffBearing);
            }
            raceCourseCanvasOverlay.setStartEndPoint(startPoint, endPoint);
        } else {
            simulatorMap.getMainPanel().setUpdateButtonEnabled(false);
        }
    }

    public CourseAreaDescriptor getCurrentCourseArea() {
    	return this.currentCourseArea;
    }
    
    public void setCurrentCourseArea(CourseAreaDescriptor courseArea) {
    	this.currentCourseArea = courseArea;
    }
    
    protected void drawCourseAreaLabel(final CourseAreaDescriptor courseArea, final Context2d context2d) {
        // Native circles clip geographic fills at the camera plane; this canvas contains only upright labels.
        final Point centerPoint = getMapProjection().fromLatLngToDivPixel(courseArea.getCenterPos());
        if (getMap().getZoom() >= 11 && Double.isFinite(centerPoint.getX()) && Double.isFinite(centerPoint.getY())) {
            context2d.setGlobalAlpha(0.8f);
            context2d.setFillStyle(courseArea.getColorText());

            final double fontsize = 14.0 + (32.0 - 14.0) * (getMap().getZoom() - 11.0) / (14.0 - 11.0);
            context2d.setFont("normal " + fontsize + "px Calibri");
            // TextMetrics txtmet = context2d.measureText(name);
            context2d.setTextAlign(TextAlign.CENTER);
            context2d.fillText(courseArea.getName(), centerPoint.getX() - this.getWidgetPosLeft(), centerPoint.getY() + 0.32 * fontsize
                    - this.getWidgetPosTop());

        }
        
        context2d.setGlobalAlpha(1.0f);
    }

    protected LatLng getDistantPoint(LatLng pos, double dist, double degBear) {

        double lat1 = pos.getLatitude() / 180. * Math.PI;
        double lon1 = pos.getLongitude() / 180. * Math.PI;

        double brng = degBear * Math.PI / 180;

        double R = 6371;
        double d = 1.852 * dist;
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R) + Math.cos(lat1) * Math.sin(d / R) * Math.cos(brng));
        double lon2 = lon1
                + Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(lat1),
                        Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));
        lon2 = (lon2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI; // normalize to -180° ... +180°*/

        double lat2deg = lat2 / Math.PI * 180;
        double lon2deg = lon2 / Math.PI * 180;

        LatLng result = LatLng.newInstance(lat2deg, lon2deg);

        return result;
    }

    public void setRaceCourseCanvas(RaceCourseCanvasOverlay rcCanvas) {
        this.raceCourseCanvasOverlay = rcCanvas;
    }

    public VenueDescriptor getVenue() {
    	return this.venue;
    }
    
}
