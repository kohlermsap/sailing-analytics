package com.sap.sse.gwt.dispatch.shared.commands;

import com.sap.sse.gwt.shared.DTO;

public class BooleanResult implements DTO, Result {
    private boolean value;

    @Deprecated // Used for GWT serialization only
    BooleanResult() {
    }

    public BooleanResult(final boolean b) {
        this.value = b;
    }

    public boolean getValue() {
        return value;
    }
}
