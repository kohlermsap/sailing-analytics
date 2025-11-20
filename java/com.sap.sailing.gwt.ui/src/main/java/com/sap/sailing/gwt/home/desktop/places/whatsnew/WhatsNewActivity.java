package com.sap.sailing.gwt.home.desktop.places.whatsnew;

import java.util.Optional;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class WhatsNewActivity extends AbstractActivity {
    private final WhatsNewPlace place;
    private final WhatsNewClientFactory clientFactory;

    public WhatsNewActivity(WhatsNewPlace place, WhatsNewClientFactory clientFactory) {
        this.place = place;
        this.clientFactory = clientFactory;
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        WhatsNewView whatsNewView = clientFactory.createWhatsNewView(place.getNavigationTab());
        panel.setWidget(whatsNewView.asWidget());
        Window.setTitle((ClientConfiguration.getInstance().isBrandingActive() 
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing()) + " - " + StringMessages.INSTANCE.whatsNew());
    }
}
