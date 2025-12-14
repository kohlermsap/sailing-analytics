package com.sap.sailing.gwt.home.desktop.partials.solutions;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Shared;
import com.google.gwt.resources.client.ImageResource;

public interface SolutionsResources extends ClientBundle {
    public static final SolutionsResources INSTANCE = GWT.create(SolutionsResources.class);

    @Source("Solutions.gss")
    LocalCss css();
    
    @Source("solutions-sailing-insight.png")
    ImageResource sailingInsightImage();
    @Source("solutions-sailing-buoy-pinger.png")
    ImageResource buoyPingerImage();
    @Source("solutions-sailing-race-manager.png")
    ImageResource raceManagerImage();
    @Source("solutions-simulator.png")
    ImageResource simulatorImage();
    @Source("solutions.png")
    ImageResource solutionsImage();
    
    @Shared
    public interface LocalCss extends CssResource {
        String solutions();
        String solutions_nav();
        String solutions_nav_link();
        String solutions_nav_linkactive();
        String solutions_content();
        String parallax();
        String gridalternator();
        String solutions_contentsapinsailing();
        String solutions_contentsapinsailing_body();
        String solutions_contentsap();
        String solutions_contentsapsailingracemanager();
        String solutions_contentsapsailingracemanager_body();
        String solutions_contentsapsailinsight();
        String solutions_contentsapsailingbuoypinger();
        String solutions_contentsapsailingbuoypinger_body();
        String solutions_contentsimulator();
        String solutions_content_linkappstore();
    }
}
