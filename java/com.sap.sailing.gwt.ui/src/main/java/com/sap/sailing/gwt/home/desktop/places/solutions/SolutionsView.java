package com.sap.sailing.gwt.home.desktop.places.solutions;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.IsWidget;

public interface SolutionsView extends IsWidget {
    interface Presenter {}
    void clearContent();
    void setContentHtml(SafeHtml html);
}
