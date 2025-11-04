package com.sap.sailing.gwt.home.desktop.places.solutions;

import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace.SolutionsNavigationTabs;
import com.sap.sailing.gwt.ui.client.SailingClientFactory;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;

public interface SolutionsClientFactory extends SailingClientFactory {
    SolutionsView createSolutionsView(SolutionsNavigationTabs navigationTab);
    SailingServiceAsync getSailingService();
}
