package com.sap.sailing.gwt.home.shared.partials.checkboxtile;

import java.util.function.BiConsumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

/**
 * @param onToggle
 *            if null, toggle will be disabled
 */
public final class CheckBoxTile extends Composite {
    private static CheckBoxTileUiBinder uiBinder = GWT.create(CheckBoxTileUiBinder.class);

    interface CheckBoxTileUiBinder extends UiBinder<Widget, CheckBoxTile> {
    }

    @UiField
    SharedResources res;
    @UiField
    Label labelUi;
    @UiField
    CheckBox toggleButtonUi;

    public CheckBoxTile(final String label, final boolean initialState,
            final BiConsumer<Boolean, AsyncCallback<VoidResult>> onToggle) {
        super();
        initWidget(uiBinder.createAndBindUi(this));
        labelUi.setText(label);
        toggleButtonUi.setValue(initialState);
        if (onToggle == null) {
            toggleButtonUi.setEnabled(false);
        }
        toggleButtonUi.addValueChangeHandler(value -> {
            final Boolean newlyToggledValue = value.getValue();
            onToggle.accept(newlyToggledValue, callback());
        });
    }

    public void showLoadingUi() {
    }

    public void hideLoadingUi() {
    }

    public void setValue(final boolean b) {
        toggleButtonUi.setValue(b);
    }

    public void setEnabled(final boolean b) {
        toggleButtonUi.setEnabled(b);
    }
    
    public AsyncCallback<VoidResult> callback() {
        return new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {
                toggleButtonUi.setValue(!toggleButtonUi.getValue(), false); // undo failed toggle
                hideLoadingUi();
                toggleButtonUi.setEnabled(true);
            }

            @Override
            public void onSuccess(VoidResult result) {
                hideLoadingUi();
                toggleButtonUi.setEnabled(true);
            }
        };
    }
}
