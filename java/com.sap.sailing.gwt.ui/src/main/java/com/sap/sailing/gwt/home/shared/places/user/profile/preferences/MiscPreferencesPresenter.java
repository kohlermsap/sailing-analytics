package com.sap.sailing.gwt.home.shared.places.user.profile.preferences;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.home.shared.partials.multiselection.MiscellaneousDisplayImpl;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

public interface MiscPreferencesPresenter {
    public void registerDisplay(MiscellaneousDisplayImpl display);
    
    public void updateIsSubscribedToFeatureAndCommunityUpdates(final boolean b, final AsyncCallback<VoidResult> callback);

    /** get value via dispatch method, set first correct value onto checkbox */
    public void initIsSubscribedToFeatureAndCommunityUpdates(final boolean b);
}
