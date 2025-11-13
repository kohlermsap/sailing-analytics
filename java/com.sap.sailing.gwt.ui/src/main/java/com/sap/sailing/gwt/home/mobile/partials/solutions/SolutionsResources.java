package com.sap.sailing.gwt.home.mobile.partials.solutions;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface SolutionsResources extends ClientBundle {
    public static final SolutionsResources INSTANCE = GWT.create(SolutionsResources.class);

    @Source("solutions-sailing-insight.png")
    ImageResource sailingInsightImage();
    @Source("solutions-sailing-buoy-pinger.png")
    ImageResource buoyPingerImage();
    @Source("solutions-race.png")
    ImageResource raceManagerImage();
    @Source("solutions-simulator-trimmed.png")
    ImageResource simulatorImage();
    @Source("solutions-trimmed.png")
    ImageResource solutionsImage();
}
