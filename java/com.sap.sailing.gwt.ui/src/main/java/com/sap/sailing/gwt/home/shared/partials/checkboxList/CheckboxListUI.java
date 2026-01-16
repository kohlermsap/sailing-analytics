package com.sap.sailing.gwt.home.shared.partials.checkboxList;

import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionNotificationToggle;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;

public class CheckboxListUI extends Composite {
    interface Binder extends UiBinder<Widget, CheckboxListUI> {
    }

    private static final Binder binder = GWT.create(Binder.class);

    /* tiles are label + checkbox */
    @UiField
    FlowPanel tileList;
    @UiField
    SpanElement title;

    public CheckboxListUI(final String title, final Set<CheckboxListEntryModel> entries) {
        initWidget(binder.createAndBindUi(this));
        this.title.setInnerText(title);
        for (CheckboxListEntryModel e : entries) {
            tileList.add(generateTile(e));
        }
    }

    private SuggestedMultiSelectionNotificationToggle generateTile(CheckboxListEntryModel e) {
        final SuggestedMultiSelectionNotificationToggle tile = new SuggestedMultiSelectionNotificationToggle(e.label);
        tile.setValue(e.initialValue);
        tile.setEnabled(e.changeHandler != null);
        tile.addValueChangeHandler(ev -> {
            final Boolean newValue = ev.getValue();
            try {
                e.changeHandler.onEntryValueChanged.accept(newValue);
                final String msg = e.changeHandler.successMessage;
                if (msg != null && msg.length() != 0) {
                    Notification.notify(msg, NotificationType.SUCCESS);
                }
            } catch (Exception e2) {
                final String msg = e.changeHandler.failureMessage;
                if (msg != null && msg.length() != 0) {
                    Notification.notify(msg, NotificationType.ERROR);
                }
            }
        });
        return tile;
    }
}