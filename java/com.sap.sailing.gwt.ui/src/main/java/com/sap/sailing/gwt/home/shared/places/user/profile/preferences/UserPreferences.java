package com.sap.sailing.gwt.home.shared.places.user.profile.preferences;

import java.util.function.BiConsumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.shared.partials.checkboxtile.CheckBoxTile;
import com.sap.sailing.gwt.home.shared.partials.labeledbox.LabeledBox;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelection;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

/**
 * Implementation of {@link UserPreferencesView} where users can change their preferred selections and notifications.
 */
public class UserPreferences extends Composite implements UserPreferencesView {

    private static UserPreferencesUiBinder uiBinder = GWT.create(UserPreferencesUiBinder.class);

    interface UserPreferencesUiBinder extends UiBinder<Widget, UserPreferences> {
    }
    
    interface Style extends CssResource {
        String edgeToEdge();
    }
    
    @UiField Style style;
    @UiField SharedResources res;
    @UiField(provided = true) SuggestedMultiSelection<SimpleCompetitorWithIdDTO> favoriteCompetitorsSelctionUi;
    @UiField(provided = true) SuggestedMultiSelection<BoatClassDTO> favoriteBoatClassesSelctionUi;
    @UiField(provided = true) LabeledBox miscUi;
    @UiField DivElement notificationsTextUi;

    public UserPreferences(UserPreferencesView.Presenter presenter, FlagImageResolver flagImageResolver) {
        favoriteCompetitorsSelctionUi = new CompetitorDisplayImpl(
                presenter.getFavoriteCompetitorsDataProvider(), flagImageResolver).selectionUi;
        favoriteBoatClassesSelctionUi = new BoatClassDisplayImpl(
                presenter.getFavoriteBoatClassesDataProvider()).selectionUi;
        miscUi = (new MiscellaneousDisplayImpl(presenter)).selectionUi;
        initWidget(uiBinder.createAndBindUi(this));
        // TODO hide notificationsTextUi if the user's mail address is already verified
    }
    
    private class MiscellaneousDisplayImpl {
        public final LabeledBox selectionUi;

        public MiscellaneousDisplayImpl(final UserPreferencesView.Presenter presenter) {
            final FlowPanel tileList = new FlowPanel();
            final CheckBoxTile securityUpdates = new CheckBoxTile(StringMessages.INSTANCE.securityUpdates(), true,
                    null);
            tileList.add(securityUpdates);
            final CheckBoxTile featureAndCommunityUpdates = composeFeatureAndCommunityUpdatesTile(presenter);
            tileList.add(featureAndCommunityUpdates);
            selectionUi = new LabeledBox(StringMessages.INSTANCE.miscellaneous(), tileList);
            presenter.initIsSubscribedToFeatureAndCommunityUpdates(featureAndCommunityUpdates);
        }

        private CheckBoxTile composeFeatureAndCommunityUpdatesTile(final UserPreferencesView.Presenter presenter) {
            final BiConsumer<Boolean, AsyncCallback<VoidResult>> onToggle = (isReceiveNowTrue, callback) -> {
                final AsyncCallback<VoidResult> wrapCallbackWithToastNotification = new AsyncCallback<VoidResult>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        final String message = StringMessages.INSTANCE.failedToSetStatusOfFeatureAndCommunityUpdates();
                        Notification.notify(message, NotificationType.ERROR);
                        callback.onFailure(caught);
                    }

                    @Override
                    public void onSuccess(VoidResult result) {
                        final String willRcvMsg = StringMessages.INSTANCE.youWillNowReceiveFeatureAndCommunityUpdates();
                        final String willNotRcvMsg = StringMessages.INSTANCE.youWillNotReceiveFeatureAndCommunityUpdatesAnymore();
                        final String message = isReceiveNowTrue ? willRcvMsg : willNotRcvMsg;
                        Notification.notify(message, NotificationType.SUCCESS);
                        callback.onSuccess(result);
                    }
                };
                presenter.setIsSubscribedToFeatureAndCommunityUpdates(isReceiveNowTrue, wrapCallbackWithToastNotification);
            };
            return new CheckBoxTile(StringMessages.INSTANCE.featureAndCommunityUpdates(), false, onToggle);
        }
    }

    public void setEdgeToEdge(boolean edgeToEdge) {
        favoriteBoatClassesSelctionUi.setStyleName(style.edgeToEdge(), edgeToEdge);
        favoriteCompetitorsSelctionUi.setStyleName(style.edgeToEdge(), edgeToEdge);
        favoriteBoatClassesSelctionUi.getElement().getParentElement().removeClassName(res.mediaCss().column());
        favoriteCompetitorsSelctionUi.getElement().getParentElement().removeClassName(res.mediaCss().column());
    }
    
    private class CompetitorDisplayImpl implements CompetitorSelectionPresenter.Display {
        private final SuggestedMultiSelection<SimpleCompetitorWithIdDTO> selectionUi;
        private final HasValue<Boolean> notifyAboutResultsUi;
        
        private CompetitorDisplayImpl(final CompetitorSelectionPresenter dataProvider,
                FlagImageResolver flagImageResolver) {
            selectionUi = SuggestedMultiSelection.forCompetitors(dataProvider, StringMessages.INSTANCE.favoriteCompetitors(), flagImageResolver);
            notifyAboutResultsUi = selectionUi.addNotificationToggle(dataProvider::setNotifyAboutResults,
                    StringMessages.INSTANCE.notificationAboutNewResults());
            dataProvider.addDisplay(this);
        }
        
        @Override
        public void setSelectedItems(Iterable<SimpleCompetitorWithIdDTO> selectedItems) {
            selectionUi.setSelectedItems(selectedItems);
        }

        @Override
        public void setNotifyAboutResults(boolean notifyAboutResults) {
            notifyAboutResultsUi.setValue(notifyAboutResults);
        }
    }
    
    private class BoatClassDisplayImpl implements BoatClassSelectionPresenter.Display {
        private final SuggestedMultiSelection<BoatClassDTO> selectionUi;
        private final HasValue<Boolean> notifyAboutUpcomingRacesUi;
        private final HasValue<Boolean> notifyAboutResultsUi;
        
        private BoatClassDisplayImpl(final BoatClassSelectionPresenter dataProvider) {
            selectionUi = SuggestedMultiSelection.forBoatClasses(dataProvider, StringMessages.INSTANCE.favoriteBoatClasses());
            notifyAboutUpcomingRacesUi = selectionUi.addNotificationToggle(dataProvider::setNotifyAboutUpcomingRaces,
                    StringMessages.INSTANCE.notificationAboutUpcomingRaces());
            notifyAboutResultsUi = selectionUi.addNotificationToggle(dataProvider::setNotifyAboutResults,
                    StringMessages.INSTANCE.notificationAboutNewResults());
            dataProvider.addDisplay(this);
        }
        
        @Override
        public void setSelectedItems(Iterable<BoatClassDTO> selectedItems) {
            selectionUi.setSelectedItems(selectedItems);
        }
        
        @Override
        public void setNotifyAboutUpcomingRaces(boolean notifyAboutUpcomingRaces) {
            notifyAboutUpcomingRacesUi.setValue(notifyAboutUpcomingRaces);
        }
        
        @Override
        public void setNotifyAboutResults(boolean notifyAboutResults) {
            notifyAboutResultsUi.setValue(notifyAboutResults);
        }
    }

}
