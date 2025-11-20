package com.sap.sse.gwt.client.sapheader;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface BrandedHeaderResources extends ClientBundle {
    public static final BrandedHeaderResources INSTANCE = GWT.create(BrandedHeaderResources.class);

    @Source("BrandedSailingHeader.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String siteheader();
        String siteheaderLeft();
        String siteHeaderTitle();
        String siteHeaderSubTitle();
        String siteheader2ndCol();
        String siteheaderRight();
        String logo();
        String logotitle();
        String pagetitle();
    }
    
    @Source("logo-small@2x.png")
    ImageResource logo();
}
