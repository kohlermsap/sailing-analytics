package com.sap.sailing.gwt.home.mobile.partials.accordion;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiChild;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.shared.utils.CollapseAnimation;

public class AccordionItem extends Composite {

    private static final String ACCORDION_COLLAPSED_STYLE = AccordionResources.INSTANCE.css().accordioncollapsed();

    private static AccordionItemUiBinder uiBinder = GWT.create(AccordionItemUiBinder.class);

    interface AccordionItemUiBinder extends UiBinder<Widget, AccordionItem> {
    }
    
    @UiField
    DivElement headerUi;
    
    @UiField
    HeadingElement titleUi;
    
    @UiField
    ImageElement imageUi;
    
    @UiField
    SimplePanel contentUi;
    
    @UiField
    DivElement contentWrapperUi;

    public AccordionItem(String title, String imageUrl, String imageAltText, boolean showInitial) {
        this(title, () -> imageUrl, imageAltText, showInitial);
    }

    public AccordionItem(String title, ImageResource image, String imageAltText, boolean showInitial) {
        this(title, image==null?null:image.getSafeUri(), imageAltText, showInitial);
    }

    public AccordionItem(String title, SafeUri imageUrl, String imageAltText, boolean showInitial) {
        initWidget(uiBinder.createAndBindUi(this));
        titleUi.setInnerText(title);
        if (imageUrl != null) {
            imageUi.setSrc(imageUrl.asString());
        }
        imageUi.setAlt(imageAltText);
        initAnimation(showInitial);
    }
    
    public void setImageUrl(String imageUrl) {
        imageUi.setSrc(imageUrl);
    }

    private void initAnimation(boolean showInitial) {
        final CollapseAnimation animation = new CollapseAnimation(contentWrapperUi, showInitial);
        Element rootElement = getWidget().getElement();
        UIObject.setStyleName(rootElement, ACCORDION_COLLAPSED_STYLE, !showInitial);
        DOM.sinkEvents(headerUi, Event.ONCLICK);
        DOM.setEventListener(headerUi, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                boolean collapsed = rootElement.hasClassName(ACCORDION_COLLAPSED_STYLE);
                UIObject.setStyleName(rootElement, ACCORDION_COLLAPSED_STYLE, !collapsed);
                animation.animate(collapsed);
            }
        });
    }
    
    @UiChild
    public void addContent(Widget content) {
        contentUi.setWidget(content);
    }
    
    public void setHeaderText(String text) {
        if (text == null) text = "";
        titleUi.setInnerText(text);
        getElement().setAttribute("aria-label", text);
        asWidget().setTitle(text);
    }
    
    public void setContent(String content) {
        contentWrapperUi.setInnerText(content);
    }
}
