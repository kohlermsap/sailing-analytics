package com.sap.sailing.gwt.home.desktop.places.event.regatta;

import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.TextTransform;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.windfinder.SpotDTO;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabPanel;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabPanelPlaceSelectionEvent;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabView;
import com.sap.sailing.gwt.home.desktop.partials.eventheader.EventHeader;
import com.sap.sailing.gwt.home.desktop.partials.sailorinfo.SailorInfo;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.overviewtab.RegattaOverviewTabView;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.racestab.RegattaRacesTabView;
import com.sap.sailing.gwt.home.shared.app.ApplicationHistoryMapper;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.windfinder.WindfinderControl;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesDefaultPlace;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.LinkUtil;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class TabletAndDesktopRegattaEventView extends Composite implements EventRegattaView {
    
    private static final ApplicationHistoryMapper historyMapper = GWT.<ApplicationHistoryMapper> create(ApplicationHistoryMapper.class);

    interface MyBinder extends UiBinder<Widget, TabletAndDesktopRegattaEventView> {
    }
    
    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    @UiField StringMessages i18n;

    @UiField(provided = true) TabPanel<AbstractEventRegattaPlace, EventRegattaView.Presenter, RegattaTabView<AbstractEventRegattaPlace>> tabPanelUi;
    @UiField(provided = true) EventHeader eventHeader;
    @UiField(provided = true) RegattaRacesTabView racesTabUi;
    @UiField(provided = true) RegattaOverviewTabView regattaOverviewTabUi;
    
    private Presenter currentPresenter;

    private final FlagImageResolver flagImageResolver;

    public TabletAndDesktopRegattaEventView(FlagImageResolver flagImageResolver) {
        this.flagImageResolver = flagImageResolver;
    }

    @Override
    public void registerPresenter(final Presenter currentPresenter) {
        this.currentPresenter = currentPresenter;
        tabPanelUi = new TabPanel<>(currentPresenter, historyMapper);
        eventHeader = new EventHeader(currentPresenter);
        racesTabUi = new RegattaRacesTabView(flagImageResolver);
        regattaOverviewTabUi = new RegattaOverviewTabView(flagImageResolver);
        initWidget(uiBinder.createAndBindUi(this));

        String sailorsInfoURL = currentPresenter.getEventDTO().getSailorsInfoWebsiteURL();
        if (sailorsInfoURL != null && !sailorsInfoURL.isEmpty()) {
            tabPanelUi.addTabExtension(new SailorInfo(sailorsInfoURL));
        }

        final PlaceNavigation<SeriesDefaultPlace> currentEventSeriesNavigation = currentPresenter.getCurrentEventSeriesNavigation();
        if (currentEventSeriesNavigation != null) {
            Anchor seriesAnchor = new Anchor(i18n.overallLeaderboardSelection());
            seriesAnchor.setHref(currentEventSeriesNavigation.getTargetUrl());
            seriesAnchor.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    if (LinkUtil.handleLinkClick(event.getNativeEvent().<Event>cast())) {
                        event.preventDefault();
                        currentEventSeriesNavigation.goToPlace();
                    }
                }
            });
            seriesAnchor.setStyleName(SharedResources.INSTANCE.mainCss().button());
            seriesAnchor.addStyleName(SharedResources.INSTANCE.mainCss().buttonprimary());
            Style style = seriesAnchor.getElement().getStyle();
            style.setPaddingLeft(0.75, Unit.EM);
            style.setPaddingRight(0.75, Unit.EM);
            style.setTextTransform(TextTransform.UPPERCASE);
            tabPanelUi.addTabExtension(seriesAnchor);
        }

        final Iterable<SpotDTO> spots = currentPresenter.getEventDTO().getAllWindFinderSpotIdsUsedByEvent();
        if (spots != null && !Util.isEmpty(spots)) {
            tabPanelUi.addTabExtension(new WindfinderControl(spots, SpotDTO::getCurrentlyMostAppropriateUrl));
        }
    }

    @Override
    public void navigateTabsTo(AbstractEventRegattaPlace place) {
        tabPanelUi.activatePlace(place);
        StringBuilder titleBuilder = new StringBuilder(ClientConfiguration.getInstance().isBrandingActive() 
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing()).append(" - ");

        titleBuilder.append(currentPresenter.showRegattaMetadata() ? currentPresenter.getRegattaMetadata()
                .getDisplayName() : currentPresenter.getEventDTO().getDisplayName());
        String currentTabTitle = tabPanelUi.getCurrentTabTitle();
        if (currentTabTitle != null && !currentTabTitle.isEmpty()) {
            titleBuilder.append(" - ").append(currentTabTitle);
        }
        Window.setTitle(titleBuilder.toString());
    }

    @SuppressWarnings("unchecked")
    @UiHandler("tabPanelUi")
    public void onTabSelection(TabPanelPlaceSelectionEvent e) {
        currentPresenter.handleTabPlaceSelection((TabView<?, EventRegattaView.Presenter>) e.getSelectedActivity());
    }

    @Override
    public void showErrorInCurrentTab(IsWidget errorView) {
        tabPanelUi.overrideCurrentContentInTab(errorView);
    }

}
