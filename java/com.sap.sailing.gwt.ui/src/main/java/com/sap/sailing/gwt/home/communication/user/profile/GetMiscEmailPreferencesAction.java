package com.sap.sailing.gwt.home.communication.user.profile;

import com.google.gwt.core.shared.GwtIncompatible;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sse.gwt.dispatch.shared.commands.BooleanResult;
import com.sap.sse.gwt.dispatch.shared.commands.HasWriteAction;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;

public class GetMiscEmailPreferencesAction implements SailingAction<BooleanResult>, HasWriteAction, IsSerializable {
    // @GwtIncompatible
    public GetMiscEmailPreferencesAction() {
    }

    @Override
    @GwtIncompatible
    public BooleanResult execute(SailingDispatchContext ctx) throws DispatchException {
        final boolean didOptOut = ctx.getSecurityService().getCurrentUser().getDidOptOutOfFeatureAndCommunityEmails();
        return new BooleanResult(!didOptOut);
    }
}
