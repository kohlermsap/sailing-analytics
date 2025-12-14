package com.sap.sse.security.ui.authentication.generic.sapheader;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.sap.sse.security.ui.authentication.generic.resource.AuthenticationResources;

public interface HeaderWithAuthenticationResources extends AuthenticationResources {
    public static final HeaderWithAuthenticationResources INSTANCE = GWT
            .create(HeaderWithAuthenticationResources.class);

    @Source("header-with-authentication.gss")
    HeaderWithAuthenticationCss css();

    public interface HeaderWithAuthenticationCss extends CssResource {
        String header_right_wrapper();
        String header_right_extension();
        String fixed();
        String usermanagement_icon();
        String usermanagement_loggedin();
        String usermanagement_view();
        String usermanagement_open();
        String user_menu_premium();
        String languageSelector();
    }
}