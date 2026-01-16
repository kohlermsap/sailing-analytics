package com.sap.sailing.gwt.home.shared.partials.checkboxList;

import java.util.function.Consumer;

public class CheckboxListEntryModel {
    final String label;
    final boolean initialValue;
    final ChangeHandler changeHandler;

    public static class ChangeHandler {
        final Consumer<Boolean> onEntryValueChanged;
        final boolean isAsync;
        final String successMessage;
        final String failureMessage;

        public ChangeHandler(final Consumer<Boolean> onEntryValueChanged, final boolean isAsync,
                final String successMessage, final String failureMessage) {
            this.onEntryValueChanged = onEntryValueChanged;
            this.isAsync = isAsync;
            this.successMessage = successMessage;
            this.failureMessage = failureMessage;
        }
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass() && (((CheckboxListEntryModel) obj).label) == label;
    }

    /**
     * @param changeHandler
     *            to disable checkbox, pass null here
     */
    public CheckboxListEntryModel(final String label, final boolean initialValue, final ChangeHandler changeHandler) {
        this.label = label;
        this.initialValue = initialValue;
        this.changeHandler = changeHandler;
    }

    boolean getValue() {
        return initialValue;
    }
}