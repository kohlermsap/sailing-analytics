package com.sap.sailing.gwt.ui.simulator;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.TextAlign;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;

public class RegattaAreaCanvasOverlay extends FullCanvasOverlay {

    private SimulatorMap simulatorMap;
    private RaceCourseCanvasOverlay raceCourseCanvasOverlay;
    private VenueDescriptor venue; 
    private CourseAreaDescriptor currentCourseArea = null;
    
    private boolean initial;
    private double raceBearing = 0.0;
    private double diffBearing = 0.0;

    public RegattaAreaCanvasOverlay(MapWidget map, int zIndex, char event, final SimulatorMap simulatorMap, CoordinateSystem coordinateSystem) {

    	super(map, zIndex, coordinateSystem);

    	this.simulatorMap = simulatorMap;
        this.venue = VenueDescriptorFactory.createVenue(event, coordinateSystem);
        this.currentCourseArea = this.venue.getDefaultCourseArea();        

    }

    @Override
    public void addToMap() {
        super.addToMap();
        
        int counter = 0;
        for (CourseAreaDescriptor courseArea : venue.getCourseAreas()) {
            simulatorMap.drawCircleFromRadius(counter++, courseArea);
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

    protected void drawRegattaAreas() {
        final double pxStroke = Math.pow(2.0, (getMap().getZoom() - 10.0) / 2.0);
        final Context2d context2d = canvas.getContext2d();
        context2d.setLineWidth(3);
        context2d.setStrokeStyle("Black");
        for (final CourseAreaDescriptor courseArea : venue.getCourseAreas()) {
            drawCourseArea(courseArea, context2d, pxStroke);
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
    
    protected void drawCourseArea(final CourseAreaDescriptor courseArea, final Context2d context2d, final double pxStroke) {
        final Point centerPoint = getMapProjection().fromLatLngToDivPixel(courseArea.getCenterPos());
        final String bgColor;
        if (simulatorMap.getWindParams().isShowStreamlets()) {
            bgColor = "#505050";
        } else {
            bgColor = "#DEDEDE";
        }
        // Match the geographic circle's hit region rather than drawing a screen-space circle under tilt.
        context2d.beginPath();
        for (int step = 0; step < 64; step++) {
            final LatLng boundary = getDistantPoint(courseArea.getCenterPos(), courseArea.getRadius(), step * 360.0 / 64);
            final Point pixel = getMapProjection().fromLatLngToDivPixel(boundary);
            final double x = pixel.getX() - getWidgetPosLeft();
            final double y = pixel.getY() - getWidgetPosTop();
            if (step == 0) {
                context2d.moveTo(x, y);
            } else {
                context2d.lineTo(x, y);
            }
        }
        context2d.closePath();
        context2d.setGlobalAlpha(1.0f);
        context2d.setFillStyle(bgColor);
        context2d.fill();
        context2d.setGlobalAlpha(0.5f);
        context2d.setFillStyle(courseArea.getColor());
        context2d.fill();
        context2d.setGlobalAlpha(1.0f);
        context2d.setLineWidth(pxStroke);
        context2d.setStrokeStyle(bgColor);
        context2d.stroke();
        context2d.setGlobalAlpha(0.8f);
        context2d.setStrokeStyle(courseArea.getColor());
        context2d.stroke();

        if (getMap().getZoom() >= 11) {
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
