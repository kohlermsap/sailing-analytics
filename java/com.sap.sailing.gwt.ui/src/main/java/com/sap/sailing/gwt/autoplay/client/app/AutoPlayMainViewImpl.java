package com.sap.sailing.gwt.autoplay.client.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayHeaderEvent;
import com.sap.sailing.gwt.common.authentication.SailingHeaderWithAuthentication;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.DefaultErrorReporter;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.sapheader.BrandedSailingHeader;

public class AutoPlayMainViewImpl extends ResizeComposite
        implements ApplicationTopLevelView, AcceptsOneWidget {
    public static final int SAP_HEADER_IN_PX = 75;

    private static SixtyInchViewImplUiBinder uiBinder = GWT.create(SixtyInchViewImplUiBinder.class);

    @UiField
    protected LayoutPanel mainPanel;

    protected AnimationPanel animationPanel = new AnimationPanel();

    protected BrandedSailingHeader sapHeader = new BrandedSailingHeader(SailingHeaderWithAuthentication.SAP_SAILING_APP_NAME,
            SailingHeaderWithAuthentication.SAP_SAILING_URL);

    private static ErrorReporter errorReporter = new DefaultErrorReporter<StringMessages>(StringMessages.INSTANCE);

    interface SixtyInchViewImplUiBinder extends UiBinder<Widget, AutoPlayMainViewImpl> {
    }

    public AutoPlayMainViewImpl(EventBus eventBus) {
        initWidget(uiBinder.createAndBindUi(this));
        sapHeader.setHeaderTitle("Initializing");
        mainPanel.add(sapHeader);
        mainPanel.setWidgetTopHeight(sapHeader, 0, Unit.PX, SAP_HEADER_IN_PX, Unit.PX);
        eventBus.addHandler(AutoPlayHeaderEvent.TYPE, new AutoPlayHeaderEvent.Handler() {

            private Image eventLogoImage;

            @Override
            public void onHeaderChanged(AutoPlayHeaderEvent event) {
                sapHeader.setHeaderTitle(event.getHeaderText());
                sapHeader.setHeaderSubTitle(event.getHeaderSubText());
                if (event.getHeaderLogoUrl() != null && !event.getHeaderLogoUrl().isEmpty()) {
                    if (eventLogoImage == null) {
                        eventLogoImage = new Image(event.getHeaderLogoUrl());
                        eventLogoImage.getElement().getStyle().setHeight(50, Unit.PX);
                        sapHeader.addWidgetToRightSide(eventLogoImage);
                    } else {
                        if (!eventLogoImage.getUrl().equals(event.getHeaderLogoUrl())) {
                            eventLogoImage.setUrl(event.getHeaderLogoUrl());
                        }
                    }
                }
            }
        });
        mainPanel.add(animationPanel);
        mainPanel.setWidgetTopBottom(animationPanel, 75, Unit.PX, 0, Unit.PX);
    }

    @Override
    public void setWidget(IsWidget widgetToShow) {
        animationPanel.add(widgetToShow);
    }

    @Override
    public AcceptsOneWidget getContent() {
        return this;
    }

    @Override
    public void showLoading(boolean visible) {
    }

    @Override
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }
}
