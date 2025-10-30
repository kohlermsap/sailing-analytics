package com.sap.sailing.gwt.home.desktop.partials.footer;

import static com.google.gwt.dom.client.Style.Display.NONE;
import static com.sap.sse.gwt.shared.DebugConstants.DEBUG_ID_ATTRIBUTE;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace.WhatsNewNavigationTabs;
import com.sap.sailing.gwt.home.shared.SwitchingEntryPoint;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.controls.languageselect.LanguageSelector;
import com.sap.sse.gwt.shared.ClientConfiguration;
import com.sap.sse.gwt.shared.DebugConstants;

public class Footer extends Composite {
    private static FooterPanelUiBinder uiBinder = GWT.create(FooterPanelUiBinder.class);

    interface FooterPanelUiBinder extends UiBinder<Widget, Footer> {
    }
    
    @UiField AnchorElement whatsNewAnchor;
    @UiField AnchorElement supportAnchor;
    @UiField LanguageSelector languageSelector;
    @UiField DivElement copyrightDiv;
    @UiField AnchorElement imprintAnchorLink;
    @UiField AnchorElement privacyAnchorLink;
    @UiField AnchorElement mobileUi;
    @UiField AnchorElement sapJobsAnchor;
    @UiField(provided = true)
    final PlaceNavigation<WhatsNewPlace> releaseNotesNavigation;

    public Footer(final DesktopPlacesNavigator navigator, EventBus eventBus) {
        FooterResources.INSTANCE.css().ensureInjected();
        releaseNotesNavigation = navigator.getWhatsNewNavigation(WhatsNewNavigationTabs.SailingAnalytics);

        initWidget(uiBinder.createAndBindUi(this));
        navigator.getImprintNavigation().configureAnchorElement(imprintAnchorLink);
        
        DOM.sinkEvents(mobileUi, Event.ONCLICK);
        DOM.setEventListener(mobileUi, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                if (event.getTypeInt() == Event.ONCLICK) {
                    event.preventDefault();
                    SwitchingEntryPoint.switchToMobile();
                }
            }
        });
        if (!ClientConfiguration.getInstance().isBrandingActive()) {
            copyrightDiv.getStyle().setDisplay(NONE);
            languageSelector.setLabelText(StringMessages.INSTANCE.whitelabelFooterLanguage());
            supportAnchor.getStyle().setDisplay(Display.NONE);
            whatsNewAnchor.getStyle().setDisplay(Display.NONE);
            imprintAnchorLink.getStyle().setDisplay(Display.NONE);
            privacyAnchorLink.getStyle().setDisplay(Display.NONE);
            sapJobsAnchor.getStyle().setDisplay(Display.NONE);
        }
        copyrightDiv.setAttribute(DebugConstants.DEBUG_ID_ATTRIBUTE, "copyrightDiv");
        supportAnchor.setAttribute(DEBUG_ID_ATTRIBUTE, "supportAnchor");
        whatsNewAnchor.setAttribute(DEBUG_ID_ATTRIBUTE, "whatsNewAnchor");
        imprintAnchorLink.setAttribute(DEBUG_ID_ATTRIBUTE, "imprintAnchorLink");
        privacyAnchorLink.setAttribute(DEBUG_ID_ATTRIBUTE, "privacyAnchorLink");
        languageSelector.getElement().setAttribute(DEBUG_ID_ATTRIBUTE, "languageSelector");
    }
}
