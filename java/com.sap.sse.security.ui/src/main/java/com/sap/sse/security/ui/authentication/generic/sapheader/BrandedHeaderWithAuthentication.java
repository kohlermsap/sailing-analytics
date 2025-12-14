package com.sap.sse.security.ui.authentication.generic.sapheader;

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.gwt.client.controls.languageselect.LanguageSelector;
import com.sap.sse.gwt.client.sapheader.BrandedSailingHeader;
import com.sap.sse.security.ui.authentication.generic.GenericAuthentication;
import com.sap.sse.security.ui.authentication.view.AuthenticationMenuView;
import com.sap.sse.security.ui.authentication.view.AuthenticationMenuViewImpl;

/**
 * {@link BrandedSailingHeader} which is decorated by a authentication control on the right side. This is typically used to
 * integrate with {@link GenericAuthentication}.
 */
public class BrandedHeaderWithAuthentication extends BrandedSailingHeader {
    private static final HeaderWithAuthenticationResources res = HeaderWithAuthenticationResources.INSTANCE;
    
    private final SimplePanel rightWrapper;

    private AuthenticationMenuView authenticationMenuView;

    public BrandedHeaderWithAuthentication(String applicationName, String applicationBaseUrl, String headerTitle) {
        this(applicationName, applicationBaseUrl);
        setHeaderTitle(headerTitle);
    }

    public BrandedHeaderWithAuthentication(String applicationName, String applicationBaseUrl) {
        super(applicationName, applicationBaseUrl);
        res.css().ensureInjected();
        FlowPanel rightWithAuthentication = new FlowPanel();
        rightWithAuthentication.addStyleName(res.css().header_right_wrapper());
        final LanguageSelector languageSelector = new LanguageSelector();
        languageSelector.addStyleName(res.css().languageSelector());
        rightWithAuthentication.add(languageSelector);
        Anchor authenticationMenu = new Anchor();
        authenticationMenu.addStyleName(res.css().usermanagement_icon());
        rightWithAuthentication.add(authenticationMenu);
        authenticationMenuView = new AuthenticationMenuViewImpl(authenticationMenu, res.css().usermanagement_loggedin(),
                res.css().usermanagement_open(), res.css().user_menu_premium());
        rightWrapper = new SimplePanel();
        rightWrapper.addStyleName(res.css().header_right_extension());
        rightWithAuthentication.add(rightWrapper);
        super.addWidgetToRightSide(rightWithAuthentication);
    }

    @Override
    public void addWidgetToRightSide(Widget widget) {
        rightWrapper.add(widget);
    }

    /**
     * @return the {@link AuthenticationMenuView} associated with the authentication control on the right side of the
     *         header.
     */
    public AuthenticationMenuView getAuthenticationMenuView() {
        return authenticationMenuView;
    }
}
