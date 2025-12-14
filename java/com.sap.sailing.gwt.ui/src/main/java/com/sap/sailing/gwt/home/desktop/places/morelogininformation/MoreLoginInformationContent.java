package com.sap.sailing.gwt.home.desktop.places.morelogininformation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;

public class MoreLoginInformationContent extends Widget {

    private static MoreLoginInformationContentUiBinder uiBinder = GWT.create(MoreLoginInformationContentUiBinder.class);

    interface MoreLoginInformationContentUiBinder extends UiBinder<Element, MoreLoginInformationContent> {
    }

    interface Styles extends CssResource {
        String left();

        String right();
    }

    @UiField
    HeadingElement titleUi;
    @UiField
    DivElement textUi;
    @UiField
    ImageElement imageUi;
    @UiField
    Styles style;

    @UiConstructor
    public MoreLoginInformationContent(String title, String content, ImageResource image, boolean imageOnLeft) {
        setElement(uiBinder.createAndBindUi(this));
        titleUi.setInnerText(title);
        textUi.setInnerText(content);
        if (image != null) {
            imageUi.setSrc(image.getSafeUri().asString());
        }
        imageUi.addClassName(imageOnLeft ? style.left() : style.right());
    }
    
    public void configureImage(String url) {
        if (url != null) {
            imageUi.setSrc(url);
        }
    }
    
    public void setContent(String content) {
        textUi.setInnerText(content);
    }
    
    public String getTitle() {
        return titleUi.getInnerText();
    }
    
    public void setTitle(String title) {
        titleUi.setInnerText(title);
    }

}
