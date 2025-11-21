package com.sap.sailing.gwt.home.shared.places.morelogininformation;

import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.UIObject;

public class AbstractMoreLoginInformation extends Composite implements MoreLoginInformationView {

    @UiField
    public Element registerControl;

    protected AbstractMoreLoginInformation(Runnable registerCallback) {
    }

    @Override
    public final void setRegisterControlVisible(boolean visible) {
        UIObject.setVisible(registerControl, visible);
    }

}
