package com.sap.sailing.gwt.home.desktop.places.whatsnew;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.HyperlinkImpl;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace.WhatsNewNavigationTabs;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;

public class TabletAndDesktopWhatsNewView extends Composite implements WhatsNewView {
    private static SailingAnalyticsPageViewUiBinder uiBinder = GWT.create(SailingAnalyticsPageViewUiBinder.class);

    interface SailingAnalyticsPageViewUiBinder extends UiBinder<Widget, TabletAndDesktopWhatsNewView> {
    }

    private static final HyperlinkImpl HYPERLINK_IMPL = GWT.create(HyperlinkImpl.class);

    @UiField StringMessages i18n;
    @UiField HTML sailingAnalyticsNotes;
    @UiField HTML sailingSimulatorNotes;
    @UiField HTML raceCommitteeAppNotes;
    @UiField HTML inSightAppNotes;
    @UiField HTML buoyPingerAppNotes;
    
    @UiField Anchor sailingAnalyticsNotesAnchor;
    @UiField Anchor sailingSimulatorNotesAnchor;
    @UiField Anchor raceCommitteeAppNotesAnchor;
    @UiField Anchor inSightAppNotesAnchor;
    @UiField Anchor buoyPingerAppNotesAnchor;

    private final PlaceNavigation<WhatsNewPlace> sailingAnalyticNotesNavigation; 
    private final PlaceNavigation<WhatsNewPlace> sailingSimulatorNoteNavigation; 
    private final PlaceNavigation<WhatsNewPlace> raceCommitteeAppNotesNavigation; 
    private final PlaceNavigation<WhatsNewPlace> inSightAppNotesNavigation;
    private final PlaceNavigation<WhatsNewPlace> buoyPingerAppNotesNavigation;
    private final List<Anchor> links;
    private final List<HTML> contentWidgets;
    
    public TabletAndDesktopWhatsNewView(WhatsNewNavigationTabs navigationTab, DesktopPlacesNavigator placesNavigator) {
        super();
        WhatsNewResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        // set notes texts
        sailingAnalyticsNotes.setHTML(WhatsNewResources.INSTANCE.getSailingAnalyticsNotesHtml().getText());
        sailingSimulatorNotes.setHTML(WhatsNewResources.INSTANCE.getSailingSimulatorNotesHtml().getText());
        raceCommitteeAppNotes.setHTML(WhatsNewResources.INSTANCE.getRaceCommitteeAppNotesHtml().getText());
        inSightAppNotes.setHTML(WhatsNewResources.INSTANCE.getInSightAppNotesHtml().getText());
        buoyPingerAppNotes.setHTML(WhatsNewResources.INSTANCE.getBuoyPingerAppNotesHtml().getText());
        if (ClientConfiguration.getInstance().isBrandingActive()) {
            setBrandedElementTextById(raceCommitteeAppNotes, "raceManagerHeadline", "What's New - {0} Sailing Race Manager");
            setBrandedElementTextById(sailingAnalyticsNotes, "sailingAnalyticsHeadline", "What's New - {0} Sailing Analytics");
            setBrandedElementTextById(inSightAppNotes, "inSightHeadline", "What's New - Sail Insight powered by {0}");
            setBrandedElementTextById(buoyPingerAppNotes, "buoyPingerHeadline", "What's New - {0} Sailing Buoy Pinger");
        }
        // set notes navigation
        sailingAnalyticNotesNavigation = placesNavigator.getWhatsNewNavigation(WhatsNewNavigationTabs.SailingAnalytics); 
        sailingSimulatorNoteNavigation = placesNavigator.getWhatsNewNavigation(WhatsNewNavigationTabs.SailingSimulator); 
        raceCommitteeAppNotesNavigation = placesNavigator.getWhatsNewNavigation(WhatsNewNavigationTabs.RaceManagerApp);
        inSightAppNotesNavigation = placesNavigator.getWhatsNewNavigation(WhatsNewNavigationTabs.InSightApp);
        buoyPingerAppNotesNavigation = placesNavigator.getWhatsNewNavigation(WhatsNewNavigationTabs.BuoyPingerApp);
        // set notes URLs
        sailingAnalyticsNotesAnchor.setHref(sailingAnalyticNotesNavigation.getTargetUrl());
        sailingSimulatorNotesAnchor.setHref(sailingSimulatorNoteNavigation.getTargetUrl());
        raceCommitteeAppNotesAnchor.setHref(raceCommitteeAppNotesNavigation.getTargetUrl());
        inSightAppNotesAnchor.setHref(inSightAppNotesNavigation.getTargetUrl());
        buoyPingerAppNotesAnchor.setHref(buoyPingerAppNotesNavigation.getTargetUrl());
        final String brandName = ClientConfiguration.getInstance().getBrandTitle(Optional.empty());
        sailingSimulatorNotesAnchor.setText(i18n.strategySimulator());
        if (ClientConfiguration.getInstance().isBrandingActive()) {
            sailingAnalyticsNotesAnchor.setText(i18n.solutionsAnalyticsHeadline(brandName));
            raceCommitteeAppNotesAnchor.setText(i18n.solutionsRaceHeadline(brandName));
            inSightAppNotesAnchor.setText(i18n.solutionsInSightHeadline(brandName));
            buoyPingerAppNotesAnchor.setText(i18n.solutionsBuoyPingerHeadline(brandName));
        } else {
            sailingAnalyticsNotesAnchor.setText(i18n.solutionsAnalyticsHeadline(""));
            raceCommitteeAppNotesAnchor.setText(i18n.solutionsRaceHeadline(""));
            inSightAppNotesAnchor.setText(i18n.sailInSightName());
            buoyPingerAppNotesAnchor.setText(i18n.solutionsBuoyPingerHeadline(""));
        }
        links = Arrays.asList(new Anchor[] { sailingAnalyticsNotesAnchor, sailingSimulatorNotesAnchor, raceCommitteeAppNotesAnchor, inSightAppNotesAnchor, buoyPingerAppNotesAnchor });
        contentWidgets = Arrays.asList(new HTML[] { sailingAnalyticsNotes, sailingSimulatorNotes, raceCommitteeAppNotes, inSightAppNotes, buoyPingerAppNotes });
        switch(navigationTab) {
            case BuoyPingerApp:
                setActiveContent(buoyPingerAppNotes, buoyPingerAppNotesAnchor);
                break;
            case InSightApp:
                setActiveContent(inSightAppNotes, inSightAppNotesAnchor);
                break;
            case RaceManagerApp:
                setActiveContent(raceCommitteeAppNotes, raceCommitteeAppNotesAnchor);
                break;
            case SailingAnalytics:
                setActiveContent(sailingAnalyticsNotes, sailingAnalyticsNotesAnchor);
                break;
            case SailingSimulator:
                setActiveContent(sailingSimulatorNotes, sailingSimulatorNotesAnchor);
                break;
            case TrainingDiary:
                break;
        }
    }

    @UiHandler("sailingAnalyticsNotesAnchor")
    void overviewClicked(ClickEvent event) {
        setActiveContent(sailingAnalyticsNotes, sailingAnalyticsNotesAnchor);
        handleClickEventWithLocalNavigation(event, sailingAnalyticNotesNavigation);
    }

    @UiHandler("sailingSimulatorNotesAnchor")
    void featuresClicked(ClickEvent event) {
        setActiveContent(sailingSimulatorNotes, sailingSimulatorNotesAnchor);
        handleClickEventWithLocalNavigation(event, sailingSimulatorNoteNavigation);
    }

    @UiHandler("raceCommitteeAppNotesAnchor")
    void releaseNotesClicked(ClickEvent event) {
        setActiveContent(raceCommitteeAppNotes, raceCommitteeAppNotesAnchor);
        handleClickEventWithLocalNavigation(event, raceCommitteeAppNotesNavigation);
    }

    @UiHandler("buoyPingerAppNotesAnchor")
    void buoyPingerClicked(ClickEvent event) {
        setActiveContent(buoyPingerAppNotes, buoyPingerAppNotesAnchor);
        handleClickEventWithLocalNavigation(event, buoyPingerAppNotesNavigation);
    }

    @UiHandler("inSightAppNotesAnchor")
    void inSightAppClicked(ClickEvent event) {
        setActiveContent(inSightAppNotes, inSightAppNotesAnchor);
        handleClickEventWithLocalNavigation(event, inSightAppNotesNavigation);
    }

    private void setActiveContent(HTML activeHTML, Anchor activeLink) {
        for (HTML html : contentWidgets) {
            html.setVisible(html == activeHTML);
        }
        for (Anchor link : links) {
            if (link == activeLink) {
                link.addStyleName(WhatsNewResources.INSTANCE.css().whatsnew_nav_linkactive());
            } else {
                link.removeStyleName(WhatsNewResources.INSTANCE.css().whatsnew_nav_linkactive());
            }
        }
        Window.scrollTo (0 ,0);
    }

    private void handleClickEventWithLocalNavigation(ClickEvent e, PlaceNavigation<?> placeNavigation) {
        if (HYPERLINK_IMPL.handleAsClick((Event) e.getNativeEvent())) {
            // don't use the placecontroller for navigation here as we want to avoid a page reload
            History.newItem(placeNavigation.getHistoryUrl(), false);
            e.preventDefault();
         }
    }

    private void setBrandedElementTextById(HTML contentWidget, String elementId, String templateWithBrandPlaceholder) {
        String brandName = ClientConfiguration.getInstance().getBrandTitle(Optional.empty());
        Element container = contentWidget.getElement();
        Element target = findElementById(container, elementId);
        if (target == null) {
            return;
        }
        String text = templateWithBrandPlaceholder.replace("{0}", brandName);
        target.setInnerText(text);
    }

    private Element findElementById(Element root, String elementId) {
        if (elementId.equals(root.getId())) {
            return root;
        }
        NodeList<Element> all = root.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element el = all.getItem(i);
            if (elementId.equals(el.getId())) {
                return el;
            }
        }
        return null;
    }

}
