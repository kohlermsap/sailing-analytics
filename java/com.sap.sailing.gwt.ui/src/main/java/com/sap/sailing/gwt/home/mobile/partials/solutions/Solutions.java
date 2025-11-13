package com.sap.sailing.gwt.home.mobile.partials.solutions;

import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace.WhatsNewNavigationTabs;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class Solutions extends Composite {

    interface MyUiBinder extends UiBinder<Widget, Solutions> {
    }
    
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField StringMessages i18n;

    @UiField SolutionsItem brandInSailingItem;
    @UiField SolutionsItem sailingAnalyticsItem;
    @UiField SolutionsItem sailingRaceManager;
    @UiField SolutionsItem sailInSight;
    @UiField SolutionsItem sailingBuoyPinger;
    @UiField SolutionsItem strategySimulator;
    @UiField AnchorElement sailingAnalyticsDetailsAnchor;
    @UiField AnchorElement raceManagerAppDetailsAnchor;
    @UiField AnchorElement sailInSightAppDetailsAnchor;
    @UiField AnchorElement buoyPingerAppDetailsAnchor;
    @UiField AnchorElement simulatorAppDetailsAnchor;

    @UiField ParagraphElement contentSailingAnalytics1;
    @UiField ParagraphElement contentSailingAnalytics2;
    @UiField ParagraphElement contentSailInSight;
    @UiField ParagraphElement contentSailingRaceManager;
    @UiField ParagraphElement contentSailingBuoyPinger;
    @UiField DivElement inSailingContentDiv;

    
    public Solutions(MobilePlacesNavigator placesNavigator) {
        initWidget(uiBinder.createAndBindUi(this));
        if (ClientConfiguration.getInstance().isBrandingActive()) {
            String brandName = ClientConfiguration.getInstance().getBrandTitle(Optional.empty());
            brandInSailingItem.setHeaderText(i18n.inSailing(brandName));
            sailingAnalyticsItem.setHeaderText(i18n.sailingAnalyticsTitle(brandName));
            sailingRaceManager.setHeaderText(i18n.sailingRaceManager(brandName));
            sailInSight.setHeaderText(i18n.sailInSight(brandName));
            sailingBuoyPinger.setHeaderText(i18n.sailingBuoyPinger(brandName));

            contentSailingAnalytics1.setInnerText(i18n.contentSailingAnalytics1(brandName));
            contentSailingAnalytics2.setInnerText(i18n.contentSailingAnalytics2(brandName));
            contentSailingRaceManager.setInnerText(i18n.contentSailingRaceManager(brandName));
            contentSailInSight.setInnerText(i18n.contentSailInSight(i18n.solutionsInSightHeadline(brandName)));
            contentSailingBuoyPinger.setInnerText(i18n.contentSailingBuoyPinger(brandName));
            
            brandInSailingItem.setImageUrl(ClientConfiguration.getInstance().getSolutionsInSailingImageURL());
            sailingAnalyticsItem.setImageUrl(ClientConfiguration.getInstance().getSoutionsInSailingTrimmedImageURL());
            sailingRaceManager.setImageUrl(ClientConfiguration.getInstance().getSailingRaceManagerAppTrimmedImageURL());
            sailInSight.setImageUrl(ClientConfiguration.getInstance().getSailInSightAppImageURL());
            sailingBuoyPinger.setImageUrl(ClientConfiguration.getInstance().getBuoyPingerAppImageURL());
            strategySimulator.setImageUrl(ClientConfiguration.getInstance().getSailingSimulatorTrimmedImageURL());
            sailingAnalyticsDetailsAnchor.setInnerText(ClientConfiguration.getInstance().getSailingAnalyticsReadMoreText(Optional.empty()));
        } else {
            brandInSailingItem.setVisible(false);
            sailingAnalyticsItem.setHeaderText(i18n.sailingAnalyticsTitle(""));
            sailingRaceManager.setHeaderText(i18n.sailingRaceManager(""));
            sailInSight.setHeaderText(i18n.sailInSightName());
            sailingBuoyPinger.setHeaderText(i18n.sailingBuoyPinger(""));

            contentSailingAnalytics1.setInnerText(i18n.contentSailingAnalytics1(""));
            contentSailingAnalytics2.setInnerText(i18n.contentSailingAnalytics2(""));
            contentSailingRaceManager.setInnerText(i18n.contentSailingRaceManager(""));
            contentSailInSight.setInnerText(i18n.contentSailInSight(i18n.sailInSightName()));
            contentSailingBuoyPinger.setInnerText(i18n.contentSailingBuoyPinger(""));
        }
        initWhatsNewLink(placesNavigator, WhatsNewNavigationTabs.SailingAnalytics, sailingAnalyticsDetailsAnchor);
        initWhatsNewLink(placesNavigator, WhatsNewNavigationTabs.RaceManagerApp, raceManagerAppDetailsAnchor);
        initWhatsNewLink(placesNavigator, WhatsNewNavigationTabs.InSightApp, sailInSightAppDetailsAnchor);
        initWhatsNewLink(placesNavigator, WhatsNewNavigationTabs.BuoyPingerApp, buoyPingerAppDetailsAnchor);
        initWhatsNewLink(placesNavigator, WhatsNewNavigationTabs.SailingSimulator, simulatorAppDetailsAnchor);
    }
    
    private void initWhatsNewLink(MobilePlacesNavigator placesNavigator, WhatsNewNavigationTabs tab, AnchorElement anchor) {
        placesNavigator.getWhatsNewNavigation(tab).configureAnchorElement(anchor);
    }
    
    public void setInSailingContentHtml(SafeHtml html) {
        inSailingContentDiv.setInnerHTML(html.asString());
    }
    
    public void clearInSailingContent() {
        inSailingContentDiv.setInnerHTML("");
    }
}
