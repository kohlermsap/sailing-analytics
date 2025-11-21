package com.sap.sailing.gwt.home.mobile.places.solutions;

import java.util.Optional;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.home.mobile.app.MobileApplicationClientFactory;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class SolutionsActivity extends AbstractActivity implements SolutionsView.Presenter {
    
    private final MobileApplicationClientFactory clientFactory;

    public SolutionsActivity(SolutionsPlace place, MobileApplicationClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public void start(final AcceptsOneWidget panel, final EventBus eventBus) {
        SolutionsView solutionsView = new SolutionsViewImpl(this, clientFactory.getNavigator());
        panel.setWidget(solutionsView.asWidget());
        final String locale = LocaleInfo.getCurrentLocale().getLocaleName();
        Window.setTitle((ClientConfiguration.getInstance().isBrandingActive() 
                ? ClientConfiguration.getInstance().getSailingAnalyticsSailing(Optional.empty())
                : StringMessages.INSTANCE.whitelabelSailing()) + " - " + StringMessages.INSTANCE.solutions());
        clientFactory.getSailingService().getBrandAffiliationWithSailing(locale, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                solutionsView.clearContent();
            }

            @Override
            public void onSuccess(String htmlFromServer) {
                SafeHtml safe = SimpleHtmlSanitizer.sanitizeHtml(htmlFromServer == null ? "" : htmlFromServer);
                solutionsView.setContentHtml(safe);
            }
        });
    }
}
