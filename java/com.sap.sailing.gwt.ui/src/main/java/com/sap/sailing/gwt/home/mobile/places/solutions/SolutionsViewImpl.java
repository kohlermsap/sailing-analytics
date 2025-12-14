package com.sap.sailing.gwt.home.mobile.places.solutions;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sailing.gwt.home.mobile.partials.solutions.Solutions;

public class SolutionsViewImpl extends Composite implements SolutionsView {
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, SolutionsViewImpl> {
    }

    @UiField(provided = true) Solutions solutionsUi;
    
    public SolutionsViewImpl(Presenter presenter, MobilePlacesNavigator placesNavigator) {
        this.solutionsUi = new Solutions(placesNavigator);
        initWidget(uiBinder.createAndBindUi(this));
    }
    
    public void setContentHtml(SafeHtml html) {
        solutionsUi.setInSailingContentHtml(html);
    }

    public void clearContent() {
        solutionsUi.clearInSailingContent();
    }    
}
