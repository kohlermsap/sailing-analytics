package com.sap.sse.security.ui.authentication.generic.sapheader;

import com.sap.sse.gwt.common.CommonSharedResources;
import com.sap.sse.security.ui.authentication.view.AbstractFlyoutAuthenticationView;
import com.sap.sse.security.ui.authentication.view.FlyoutAuthenticationView;

/**
 * {@link FlyoutAuthenticationView} styled to work with {@link BrandedHeaderWithAuthentication}.
 *
 */
public class GenericFlyoutAuthenticationView extends AbstractFlyoutAuthenticationView {
    protected static final HeaderWithAuthenticationResources res = HeaderWithAuthenticationResources.INSTANCE;

    public GenericFlyoutAuthenticationView(CommonSharedResources resources) {
        super(resources);
        res.css().ensureInjected();
        
        popupPanel.setStyleName(res.css().usermanagement_view());
    }
    
    public void show() {
        popupPanel.show();
        getPresenter().onVisibilityChanged(true);
    }
}
