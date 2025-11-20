package com.sap.sailing.gwt.common.authentication;

import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.security.ui.authentication.generic.sapheader.BrandedHeaderWithAuthentication;

public class SailingHeaderWithAuthentication extends BrandedHeaderWithAuthentication {
    
    public static final String SAP_SAILING_URL = "http://www.sapsailing.com";
    public static final String SAP_SAILING_APP_NAME = StringMessages.INSTANCE.sailingAnalytics();


    public SailingHeaderWithAuthentication() {
        super(SAP_SAILING_APP_NAME, SAP_SAILING_URL);
    }
    
    public SailingHeaderWithAuthentication(String headerText) {
        super(SAP_SAILING_APP_NAME, SAP_SAILING_URL, headerText);
    }

}
