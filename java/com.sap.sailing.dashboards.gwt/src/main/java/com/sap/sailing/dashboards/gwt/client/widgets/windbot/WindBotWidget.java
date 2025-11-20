package com.sap.sailing.dashboards.gwt.client.widgets.windbot;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.dashboards.gwt.client.DashboardClientFactory;
import com.sap.sailing.dashboards.gwt.client.dataretriever.WindBotDataRetrieverListener;
import com.sap.sailing.dashboards.gwt.client.widgets.header.DashboardWidgetHeaderAndNoDataMessage;
import com.sap.sailing.dashboards.gwt.client.widgets.startlineadvantage.util.LiveAverageComponent;
import com.sap.sailing.dashboards.gwt.client.widgets.windbot.charts.VerticalWindChart;
import com.sap.sailing.dashboards.gwt.client.widgets.windbot.compass.LocationPointerCompass;
import com.sap.sailing.dashboards.gwt.shared.MovingAverage;
import com.sap.sailing.dashboards.gwt.shared.WindType;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sse.gwt.dispatch.client.system.batching.SplitScheduler;
import com.sap.sse.gwt.shared.ClientConfiguration;

/**
 * The class is an actual widget on the dashboard and shows the measured data of wind bot in a race. It contains a
 * {@link LiveAverageComponent} and a {@link VerticalWindChart} for each the measured true wind speed and the true wind
 * direction. The class implements the {@link RibDashboardDataRetrieverListener} and registers as listener to receive
 * wind data updates. Also it contains a {@link LocationPointerCompass} that indicates the direction and distance from
 * the users device to the wind bot!!!
 * 
 * @author Alexander Ries (D062114)
 *
 */
public class WindBotWidget extends Composite implements HasWidgets, WindBotDataRetrieverListener {

    private MovingAverage movingAverageSpeed;
    private MovingAverage movingAverageDirection;

    private static WindBotWidgetUiBinder uiBinder = GWT.create(WindBotWidgetUiBinder.class);
    private static final Logger logger = Logger.getLogger(WindBotWidget.class.getName());

    interface WindBotWidgetUiBinder extends UiBinder<Widget, WindBotWidget> {
    }
    
    @UiField
    HTMLPanel contentContainer;
    
    @UiField(provided = true)
    DashboardWidgetHeaderAndNoDataMessage dashboardWidgetHeaderAndNoDataMessage;
    
    @UiField
    DivElement totalWindSpeedHeader;
    
    @UiField
    DivElement totalWindDirectionHeader;

    /**
     * One of two {@link LiveAverageComponent}s that display in big font the live and the average value of the measured
     * wind speed.
     * */
    @UiField(provided = true)
    public LiveAverageComponent trueWindSpeedLiveAverageComponent;

    /**
     * One of two {@link LiveAverageComponent}s that display in big font the live and the average value of the measured
     * wind direction.
     * */
    @UiField(provided = true)
    public LiveAverageComponent trueWindDirectionLiveAverageComponent;

    /**
     * One of two {@link VerticalWindChart}s that shows the wind fixes speed measured by the wind bot in a chart
     * vertically.
     * */
    @UiField(provided = true)
    public VerticalWindChart trueWindSpeedVerticalWindChart;

    /**
     * One of two {@link VerticalWindChart}s that shows the wind fixes direction measured by the wind bot in a chart
     * vertically.
     * */
    @UiField(provided = true)
    public VerticalWindChart trueWindDirectionVerticalWindChart;

    /**
     * Compass Needle that shows the direction where the wind bot is located to the device. It shows also the distance
     * to the wind bot from the users position.
     * */
    @UiField
    public LocationPointerCompass locationPointerCompass;

    public DashboardClientFactory dashboardClientFactory;
    private String windBotId;
    private StringMessages stringConstants;

    public WindBotWidget(DashboardClientFactory dashboardClientFactory) {
        WindBotWidgetResources.INSTANCE.gss().ensureInjected();
        this.dashboardClientFactory = dashboardClientFactory;
        dashboardWidgetHeaderAndNoDataMessage = new DashboardWidgetHeaderAndNoDataMessage();
        stringConstants = StringMessages.INSTANCE;
        movingAverageSpeed = new MovingAverage(500);
        movingAverageDirection = new MovingAverage(500);
        trueWindSpeedLiveAverageComponent = new LiveAverageComponent(stringConstants.dashboardTrueWindSpeedUnit());
        trueWindDirectionLiveAverageComponent = new LiveAverageComponent(stringConstants.degreesUnit());
        trueWindSpeedVerticalWindChart = new VerticalWindChart("#008FFF", "#6ADBFF");
        trueWindDirectionVerticalWindChart = new VerticalWindChart("#008FFF", "#6ADBFF");
        initWidget(uiBinder.createAndBindUi(this));
        hideContentContainer();
        dashboardWidgetHeaderAndNoDataMessage.setHeaderText(stringConstants.dashboardWindBot());
        if (ClientConfiguration.getInstance().isBrandingActive()) {
            dashboardWidgetHeaderAndNoDataMessage.showNoDataMessageWithHeaderAndMessage(stringConstants.dashboardNoWindBotAvailableHeader(), stringConstants.dashboardNoWindBotAvailableMessage(ClientConfiguration.getInstance().getBrandTitle(Optional.empty())) + " ");
        } else {
            dashboardWidgetHeaderAndNoDataMessage.showNoDataMessageWithHeaderAndMessage(stringConstants.dashboardNoWindBotAvailableHeader(), stringConstants.dashboardNoWindBotAvailableMessage(""));
        }
        totalWindSpeedHeader.setInnerHTML(stringConstants.dashboardTrueWindSpeed());
        totalWindDirectionHeader.setInnerHTML(stringConstants.dashboardTrueWindDirection());
        trueWindSpeedVerticalWindChart.addVerticalWindChartClickListener(trueWindSpeedLiveAverageComponent);
        trueWindDirectionVerticalWindChart.addVerticalWindChartClickListener(trueWindDirectionLiveAverageComponent);
    }
    
    public void setWindBotId(String windBotId) {
        this.windBotId = windBotId;
        dashboardWidgetHeaderAndNoDataMessage.setHeaderText(stringConstants.dashboardWindBot() + " " + windBotId);
    }
    
    private void showContentContainer(){
        contentContainer.getElement().getStyle().setOpacity(1.0);
    }
    
    private void hideContentContainer(){
        contentContainer.getElement().getStyle().setOpacity(0.0);
    }
    
    private WindTrackInfoDTO getWindTrackInfoDTOFromAndWindBotID(WindInfoForRaceDTO windInfoForRaceDTO, String id) {
        WindTrackInfoDTO windTrackInfo = null;
        for (WindSource windSource : windInfoForRaceDTO.windTrackInfoByWindSource.keySet()) {
            if (windSource.getType().equals(WindSourceType.EXPEDITION) && windSource.getId() != null) {
                if (windSource.getId().toString().equals(id))
                    windTrackInfo = windInfoForRaceDTO.windTrackInfoByWindSource.get(windSource);
            }
        }
        return windTrackInfo;
    }

    /**
     * Updates the classes {@link LiveAverageComponent}s and {@link VerticalWindChart}s with data AND updates the
     * {@link #locationPointerCompass} with a new wind bot position !!!
     * */
    @Override
    public void updateWindBotUI(WindInfoForRaceDTO windInfoForRaceDTO) {
        logger.log(Level.INFO, "WindBotComponent with id " + windBotId + " got notified about new WindInfoForRaceDTO");
        if (windInfoForRaceDTO != null) {
            final WindTrackInfoDTO windTrackInfoDTO = getWindTrackInfoDTOFromAndWindBotID(windInfoForRaceDTO, windBotId);
            if (windTrackInfoDTO != null) {
                logger.log(Level.INFO, "WindInfoForRaceDTO contains WindTrackInfoDTO for Windbot id " + windBotId);
                if (windTrackInfoDTO.windFixes != null) {
                    if (windTrackInfoDTO.windFixes.size() > 0) {
                        dashboardWidgetHeaderAndNoDataMessage.hideNoDataMessage();
                        logger.log(Level.INFO, "Upating UI with Wind Fixes for WindBot id " + windBotId);
                        SplitScheduler.get().schedule(new ScheduledCommand() {

                            @Override
                            public void execute() {
                                updateMovingAverages(windTrackInfoDTO.windFixes);
                            }
                        });
                        SplitScheduler.get().schedule(new ScheduledCommand() {

                            @Override
                            public void execute() {
                                updateWindSpeedLabelsAndChart(windTrackInfoDTO);
                            }
                        });
                        SplitScheduler.get().schedule(new ScheduledCommand() {

                            @Override
                            public void execute() {
                                updateWindDirectionLabelsAndChart(windTrackInfoDTO);
                            }
                        });
                        SplitScheduler.get().schedule(new ScheduledCommand() {

                            @Override
                            public void execute() {
                                updateLocationPointerCompass(windTrackInfoDTO);
                            }
                        });
                        showContentContainer();
                    } else {
                        logger.log(Level.INFO, "WindTrackInfoDTO.windFixes is empty");
                    }
                } else {
                    logger.log(Level.INFO, "WindTrackInfoDTO.windFixes is null");
                }
            } else {
                logger.log(Level.INFO, "WindInfoForRaceDTO does not contains WindTrackInfoDTO for Windbot id "
                        + windBotId);
            }
        } else {
            logger.log(Level.INFO, "WindInfoForRaceDTO is null");
        }
    }
    
    private void updateWindSpeedLabelsAndChart(final WindTrackInfoDTO windTrackInfoDTO) {
        int size = windTrackInfoDTO.windFixes.size();
        trueWindSpeedLiveAverageComponent.setLiveValue(""+NumberFormat.getFormat("#0.0").format(windTrackInfoDTO.windFixes.get(size-1).trueWindSpeedInKnots));
        trueWindSpeedVerticalWindChart.addPointsToSeriesWithAverageAndWindType(windTrackInfoDTO.windFixes, movingAverageSpeed.getAverage(), WindType.SPEED);
        trueWindSpeedLiveAverageComponent.setAverageValue(""+NumberFormat.getFormat("#0.0").format(movingAverageSpeed.getAverage()));
    }
    
    private void updateWindDirectionLabelsAndChart(final WindTrackInfoDTO windTrackInfoDTO) {
        int size = windTrackInfoDTO.windFixes.size();
        trueWindDirectionLiveAverageComponent.setLiveValue(""+NumberFormat.getFormat("#0.0").format(windTrackInfoDTO.windFixes.get(size-1).trueWindFromDeg));
        trueWindDirectionVerticalWindChart.addPointsToSeriesWithAverageAndWindType(windTrackInfoDTO.windFixes, movingAverageDirection.getAverage(), WindType.DIRECTION);
        trueWindDirectionLiveAverageComponent.setAverageValue(""+NumberFormat.getFormat("#0.0").format(movingAverageDirection.getAverage()));
    }
    
    private void updateLocationPointerCompass(final WindTrackInfoDTO windTrackInfoDTO) {
        locationPointerCompass.windBotPositionChanged(windTrackInfoDTO.windFixes.get(windTrackInfoDTO.windFixes.size() - 1).position);
    }
    
    private void updateMovingAverages(List<WindDTO> windFixes) {
        for(WindDTO windDTO : windFixes) {
            movingAverageSpeed.add(windDTO.trueWindSpeedInKnots);
            movingAverageDirection.add(windDTO.trueWindFromDeg);
        }
    }
    
    @Override
    public void add(Widget w) {
        throw new UnsupportedOperationException("The method add(Widget w) is not supported.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("The method clear() is not supported.");
    }

    @Override
    public Iterator<Widget> iterator() {
        return null;
    }

    @Override
    public boolean remove(Widget w) {
        return false;
    }
}