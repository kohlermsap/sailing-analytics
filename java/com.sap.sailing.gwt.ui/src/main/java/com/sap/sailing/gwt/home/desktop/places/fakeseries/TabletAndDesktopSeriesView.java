package com.sap.sailing.gwt.home.desktop.places.fakeseries;

import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabPanel;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabPanelPlaceSelectionEvent;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabView;
import com.sap.sailing.gwt.home.desktop.partials.seriesheader.SeriesHeader;
import com.sap.sailing.gwt.home.shared.app.ApplicationHistoryMapper;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class TabletAndDesktopSeriesView extends Composite implements SeriesView<AbstractSeriesTabPlace, SeriesView.Presenter> {
    private static final ApplicationHistoryMapper historyMapper = GWT.<ApplicationHistoryMapper> create(ApplicationHistoryMapper.class);

    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    private SeriesView.Presenter currentPresenter;

    interface MyBinder extends UiBinder<Widget, TabletAndDesktopSeriesView> {
    }

    @UiField StringMessages i18n;
    
    @UiField(provided = true)
    TabPanel<AbstractSeriesTabPlace, SeriesView.Presenter, SeriesTabView<AbstractSeriesTabPlace>> tabPanelUi;
    
    @UiField(provided = true)
    SeriesHeader seriesHeader;

    public TabletAndDesktopSeriesView() {
    }

    @Override
    public void registerPresenter(final SeriesView.Presenter currentPresenter) {
        this.currentPresenter = currentPresenter;
        tabPanelUi = new TabPanel<>(currentPresenter, historyMapper);
        
        seriesHeader = new SeriesHeader(currentPresenter);
        
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void navigateTabsTo(AbstractSeriesTabPlace place) {
        tabPanelUi.activatePlace(place);
        StringBuilder titleBuilder = new StringBuilder(ClientConfiguration.getInstance().isBrandingActive() 
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing()).append(" - ");

        titleBuilder.append(currentPresenter.getSeriesDTO().getDisplayName());

        String currentTabTitle = tabPanelUi.getCurrentTabTitle();
        if (currentTabTitle != null && !currentTabTitle.isEmpty()) {
            titleBuilder.append(" - ").append(currentTabTitle);
        }
        Window.setTitle(titleBuilder.toString());
    }

    @SuppressWarnings("unchecked")
    @UiHandler("tabPanelUi")
    public void onTabSelection(TabPanelPlaceSelectionEvent e) {
        currentPresenter.handleTabPlaceSelection((TabView<?, SeriesView.Presenter>) e.getSelectedActivity());
    }
    
    @Override
    public void showErrorInCurrentTab(IsWidget errorView) {
        tabPanelUi.overrideCurrentContentInTab(errorView);
    }
}
