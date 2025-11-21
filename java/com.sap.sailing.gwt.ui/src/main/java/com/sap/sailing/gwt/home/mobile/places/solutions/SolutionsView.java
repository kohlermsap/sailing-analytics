package com.sap.sailing.gwt.home.mobile.places.solutions;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Widget;

public interface SolutionsView {

    Widget asWidget();

    public interface Presenter {
    }
    
    void clearContent();
    void setContentHtml(SafeHtml html);
}

