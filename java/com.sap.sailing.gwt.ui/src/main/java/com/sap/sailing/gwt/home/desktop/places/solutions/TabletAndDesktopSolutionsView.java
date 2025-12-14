package com.sap.sailing.gwt.home.desktop.places.solutions;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.partials.solutions.Solutions;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace.SolutionsNavigationTabs;

public class TabletAndDesktopSolutionsView extends Composite implements SolutionsView {
    private static SolutionsPageViewUiBinder uiBinder = GWT.create(SolutionsPageViewUiBinder.class);

    @UiField(provided=true) Solutions solutions;
    
    interface SolutionsPageViewUiBinder extends UiBinder<Widget, TabletAndDesktopSolutionsView> {
    }

    public TabletAndDesktopSolutionsView(SolutionsNavigationTabs navigationTab, DesktopPlacesNavigator placesNavigator) {
        super();
        solutions = new Solutions(navigationTab, placesNavigator);
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void setContentHtml(SafeHtml html) {
        solutions.setInSailingContentHtml(html);
    }

    public void clearContent() {
        solutions.clearInSailingContent();
    }
}
