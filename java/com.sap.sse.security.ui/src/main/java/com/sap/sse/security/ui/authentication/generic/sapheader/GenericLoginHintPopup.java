package com.sap.sse.security.ui.authentication.generic.sapheader;

import com.google.gwt.user.client.Window;
import com.sap.sse.security.ui.authentication.AuthenticationManager;
import com.sap.sse.security.ui.authentication.generic.GenericAuthentication;
import com.sap.sse.security.ui.authentication.generic.GenericAuthenticationLinkFactory;
import com.sap.sse.security.ui.authentication.login.LoginHintPopup;

/**
 * Extended version of {@link LoginHintPopup} to be used with {@link GenericAuthentication}.
 */
public class GenericLoginHintPopup extends LoginHintPopup {

    public GenericLoginHintPopup(AuthenticationManager authenticationManager, GenericAuthenticationLinkFactory linkFactory) {
        super(authenticationManager, () -> Window.open(linkFactory.createMoreInfoAboutLoginLink(), "_blank", ""), null);
        this.addStyleName(HeaderWithAuthenticationResources.INSTANCE.css().usermanagement_view());
    }
}
