package com.sap.sse.gwt.client.context.impl;

import com.sap.sse.gwt.client.context.data.ClientConfigurationContextDataJSO;

/**
 * <p>
 * Accessor to the custom object structure used for providing static configuration from the server to the GWT client. 
 * An example can be seen here {@link /com.sap.sailing.gwt.ui/Home.html}
 * </p>
 * <p>
 * {@link ClientConfigurationContextDataJSO} provides access to the individual fields.
 * </p>
 * @see com.sap.sse.gwt.shared.ClientConfiguration
 * @author Georg Herdt
 *
 */
public final class ClientConfigurationContextDataFactoryImpl {
    public native ClientConfigurationContextDataJSO getInstance() /*-{
        return $doc.clientConfigurationContext;
    }-*/;
}
