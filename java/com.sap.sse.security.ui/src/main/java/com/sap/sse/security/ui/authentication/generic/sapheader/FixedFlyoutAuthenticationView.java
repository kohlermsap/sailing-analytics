package com.sap.sse.security.ui.authentication.generic.sapheader;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.sap.sse.gwt.common.CommonSharedResources;
import com.sap.sse.security.ui.authentication.view.FlyoutAuthenticationView;

/**
 * {@link FlyoutAuthenticationView} styled to work with {@link BrandedHeaderWithAuthentication}. This is used in cases when
 * the header is positioned as fixed on top of the page.
 */
public class FixedFlyoutAuthenticationView extends GenericFlyoutAuthenticationView {
    
    private HandlerRegistration resizeHandlerRegistration;

    public FixedFlyoutAuthenticationView(CommonSharedResources resources) {
        super(resources);

        popupPanel.addStyleName(res.css().fixed());
        
        popupPanel.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if(event.isAttached()) {
                    if(resizeHandlerRegistration == null) {
                        resizeHandlerRegistration = Window.addResizeHandler(new ResizeHandler() {
                            @Override
                            public void onResize(ResizeEvent event) {
                                updateHeight();
                            }
                        });
                    }
                } else {
                    resizeHandlerRegistration.removeHandler();
                    resizeHandlerRegistration = null;
                }
            }
        });
    }
    
    @Override
    public void show() {
        super.show();
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                updateHeight();
            }
        });
    }
    
    private void updateHeight() {
        int maxHeight = Window.getClientHeight() - popupPanel.getPopupTop()- (popupPanel.getOffsetHeight() - flyoverContentUi.getOffsetHeight()) - 15;
        flyoverContentUi.getStyle().setOverflowY(Overflow.AUTO);
        flyoverContentUi.getStyle().setProperty("maxHeight", maxHeight + "px");
    }
}
