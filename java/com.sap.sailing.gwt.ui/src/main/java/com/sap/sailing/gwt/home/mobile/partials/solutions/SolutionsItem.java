package com.sap.sailing.gwt.home.mobile.partials.solutions;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiConstructor;
import com.sap.sailing.gwt.home.mobile.partials.accordion.AccordionItem;

public class SolutionsItem extends AccordionItem {

    @UiConstructor
    public SolutionsItem(String title, ImageResource image, boolean showInitial) {
        super(title, image, title, showInitial);
    }
}
