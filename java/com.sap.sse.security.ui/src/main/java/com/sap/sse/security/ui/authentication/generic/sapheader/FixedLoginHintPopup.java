package com.sap.sse.security.ui.authentication.generic.sapheader;

import com.sap.sse.security.ui.authentication.AuthenticationManager;
import com.sap.sse.security.ui.authentication.generic.GenericAuthenticationLinkFactory;

/**
 * Specific version of {@link GenericLoginHintPopup} to be used on pages uses a fixed header that sticks on the top of
 * the viewport.
 */
public class FixedLoginHintPopup extends GenericLoginHintPopup {

    public FixedLoginHintPopup(AuthenticationManager authenticationManager, GenericAuthenticationLinkFactory linkFactory) {
        super(authenticationManager, linkFactory);
        this.addStyleName(HeaderWithAuthenticationResources.INSTANCE.css().fixed());
    }
}
