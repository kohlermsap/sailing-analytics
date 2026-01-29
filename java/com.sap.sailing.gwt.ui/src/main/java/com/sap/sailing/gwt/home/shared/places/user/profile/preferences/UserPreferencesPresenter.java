package com.sap.sailing.gwt.home.shared.places.user.profile.preferences;

import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.communication.user.profile.FavoriteBoatClassesDTO;
import com.sap.sailing.gwt.home.communication.user.profile.FavoriteCompetitorsDTO;
import com.sap.sailing.gwt.home.communication.user.profile.FavoritesResult;
import com.sap.sailing.gwt.home.communication.user.profile.GetFavoritesAction;
import com.sap.sailing.gwt.home.communication.user.profile.GetMiscEmailPreferencesAction;
import com.sap.sailing.gwt.home.communication.user.profile.SaveFavoriteBoatClassesAction;
import com.sap.sailing.gwt.home.communication.user.profile.SaveFavoriteCompetitorsAction;
import com.sap.sailing.gwt.home.communication.user.profile.SaveMiscEmailPreferences;
import com.sap.sailing.gwt.home.shared.app.ClientFactoryWithDispatch;
import com.sap.sailing.gwt.home.shared.partials.checkboxtile.CheckBoxTile;
import com.sap.sailing.gwt.home.shared.partials.multiselection.AbstractSuggestedBoatClassMultiSelectionPresenter;
import com.sap.sailing.gwt.home.shared.partials.multiselection.AbstractSuggestedCompetitorMultiSelectionPresenter;
import com.sap.sailing.gwt.ui.client.refresh.ErrorAndBusyClientFactory;
import com.sap.sse.gwt.dispatch.shared.commands.BooleanResult;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

/**
 * Reusable implementation of {@link UserPreferencesView.Presenter} which handles the selections and notification
 * toggles of a {@link UserPreferencesView}. It only require an appropriate client factory which implements
 * {@link ClientFactoryWithDispatch} and {@link ErrorAndBusyClientFactory}.
 * 
 * @param <C>
 *            the provided client factory type
 */
public class UserPreferencesPresenter<C extends ClientFactoryWithDispatch & ErrorAndBusyClientFactory>
        implements UserPreferencesView.Presenter {

    private final BoatClassSelectionPresenter boatClassSelectionPresenter = new BoatClassSelectionPresenterImpl();
    private final CompetitorSelectionPresenter competitorSelectionPresenter;
    private final C clientFactory;

    public UserPreferencesPresenter(C clientFactory) {
        this.clientFactory = clientFactory;
        this.competitorSelectionPresenter = new CompetitorSelectionPresenterImpl(clientFactory);
    }

    @Override
    public void loadPreferences() {
        clientFactory.getDispatch().execute(new GetFavoritesAction(), new AsyncCallback<FavoritesResult>() {
            @Override
            public void onFailure(Throwable caught) {
                clientFactory.createErrorView("Error while loading notification preferences!", caught);
            }

            @Override
            public void onSuccess(FavoritesResult result) {
                initFavoriteCompetitors(result.getFavoriteCompetitors());
                initFavoriteBoatClasses(result.getFavoriteBoatClasses());
            }
        });
    }

    @Override
    public CompetitorSelectionPresenter getFavoriteCompetitorsDataProvider() {
        return competitorSelectionPresenter;
    }

    @Override
    public BoatClassSelectionPresenter getFavoriteBoatClassesDataProvider() {
        return boatClassSelectionPresenter;
    }

    private void initFavoriteCompetitors(FavoriteCompetitorsDTO favoriteCompetitors) {
        competitorSelectionPresenter.initNotifications(favoriteCompetitors.isNotifyAboutResults());
        competitorSelectionPresenter.initSelectedItems(favoriteCompetitors.getSelectedCompetitors());
    }

    private void initFavoriteBoatClasses(FavoriteBoatClassesDTO favoriteBoatClasses) {
        boatClassSelectionPresenter.initNotifications(favoriteBoatClasses.isNotifyAboutUpcomingRaces(),
                favoriteBoatClasses.isNotifyAboutResults());
        boatClassSelectionPresenter.initSelectedItems(favoriteBoatClasses.getSelectedBoatClasses());
    }

    private class BoatClassSelectionPresenterImpl
            extends AbstractSuggestedBoatClassMultiSelectionPresenter<BoatClassSelectionPresenter.Display>
            implements BoatClassSelectionPresenter {

        private boolean notifyAboutUpcomingRaces;
        private boolean notifyAboutResults;

        @Override
        public void initNotifications(boolean notifyAboutUpcomingRaces, boolean notifyAboutResults) {
            this.notifyAboutUpcomingRaces = notifyAboutUpcomingRaces;
            this.notifyAboutResults = notifyAboutResults;
            this.displays.forEach(display -> {
                display.setNotifyAboutUpcomingRaces(notifyAboutUpcomingRaces);
                display.setNotifyAboutResults(notifyAboutResults);
            });
        }

        @Override
        public void setNotifyAboutUpcomingRaces(boolean notifyAboutUpcomingRaces) {
            this.notifyAboutUpcomingRaces = notifyAboutUpcomingRaces;
            this.persist();
        }

        @Override
        public void setNotifyAboutResults(boolean notifyAboutResults) {
            this.notifyAboutResults = notifyAboutResults;
            this.persist();
        }

        @Override
        protected void persist(Collection<BoatClassDTO> selectedItem) {
            final FavoriteBoatClassesDTO favorites = new FavoriteBoatClassesDTO(selectedItem, notifyAboutUpcomingRaces,
                    notifyAboutResults);
            clientFactory.getDispatch().execute(new SaveFavoriteBoatClassesAction(favorites), new SaveAsyncCallback());
        }
    }

    private class CompetitorSelectionPresenterImpl
            extends AbstractSuggestedCompetitorMultiSelectionPresenter<CompetitorSelectionPresenter.Display>
            implements CompetitorSelectionPresenter {

        private boolean notifyAboutResults;

        private CompetitorSelectionPresenterImpl(ClientFactoryWithDispatch clientFactory) {
            super(clientFactory);
        }

        @Override
        public void initNotifications(boolean notifyAboutResults) {
            this.notifyAboutResults = notifyAboutResults;
            this.displays.forEach(display -> display.setNotifyAboutResults(notifyAboutResults));
        }

        @Override
        public void setNotifyAboutResults(boolean notifyAboutResults) {
            this.notifyAboutResults = notifyAboutResults;
            this.persist();
        }

        @Override
        protected final void persist(Collection<SimpleCompetitorWithIdDTO> selectedItem) {
            final FavoriteCompetitorsDTO favorites = new FavoriteCompetitorsDTO(selectedItem, notifyAboutResults);
            clientFactory.getDispatch().execute(new SaveFavoriteCompetitorsAction(favorites), new SaveAsyncCallback());
        }

    }

    private class SaveAsyncCallback implements AsyncCallback<VoidResult> {
        @Override
        public void onFailure(Throwable caught) {
            clientFactory.createErrorView("Error while saving notification preferences!", caught);
        }

        @Override
        public void onSuccess(VoidResult result) {
        }
    }

    @Override
    public void setIsSubscribedToFeatureAndCommunityUpdates(final boolean b, final AsyncCallback<VoidResult> callback) {
        clientFactory.getDispatch().execute(new SaveMiscEmailPreferences(b), callback);
    }

    /** get value via dispatch method, set first correct value onto checkbox */
    @Override
    public void initIsSubscribedToFeatureAndCommunityUpdates(final CheckBoxTile ui) {
        final AsyncCallback<BooleanResult> callback = new AsyncCallback<BooleanResult>() {
            @Override
            public void onFailure(Throwable caught) {
                clientFactory.createErrorView("Error while loading miscellaneous email subscriptions!", caught);
            }

            @Override
            public void onSuccess(BooleanResult result) {
                ui.setValue(result.getValue());
            }
        };
        clientFactory.getDispatch().execute(new GetMiscEmailPreferencesAction(), callback);
    }

}
