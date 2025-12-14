package com.sap.sailing.gwt.home.mobile.places.morelogininformation;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.gwt.home.mobile.partials.accordion.AccordionItem;

public class MoreLoginInformationContentMobile extends AccordionItem {
    
    private final Label contentLabel;
    private String titleText;


    @UiConstructor
    public MoreLoginInformationContentMobile(String title, String content, ImageResource image) {
        super(title, image, title, true);
        this.contentLabel = new Label(content);
        this.titleText = title;
        addContent(contentLabel);
    }
    
    public void configureImage(String url) {
        if (url != null) {
            setImageUrl(url);
        }
    }
    
    public void setContent(String content) {
        contentLabel.setText(content != null ? content : "");
    }
    
    public String getTitle() {
        return titleText;
    }
    
    public void setTitle(String title) {
        this.titleText = title != null ? title : "";
        setHeaderText(this.titleText); 
    }
}
