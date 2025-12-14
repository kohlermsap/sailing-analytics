package com.sap.sailing.server.notification.impl;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.impl.preferences.BoatClassResultsNotificationSet;
import com.sap.sailing.server.impl.preferences.BoatClassUpcomingRaceNotificationSet;
import com.sap.sailing.server.impl.preferences.CompetitorResultsNotificationSet;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.notification.SailingNotificationService;
import com.sap.sailing.server.util.RaceBoardLinkFactory;
import com.sap.sse.common.Stoppable;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.mail.queue.MailQueue;

public class SailingNotificationServiceImpl implements SailingNotificationService {
    public static final String STRING_MESSAGES_BASE_NAME = "stringmessages/StringMessages";

    private static final Logger logger = Logger.getLogger(SailingNotificationServiceImpl.class.getName());

    private final Set<Stoppable> toStop = new HashSet<>();
    private final MailQueue mailQueue;
    private RacingEventService racingEventService;
    private final ResourceBundleStringMessages messages;

    private final BoatClassResultsNotificationSet boatClassResults;
    private final BoatClassUpcomingRaceNotificationSet boatClassUpcomingRace;
    private final CompetitorResultsNotificationSet competitorResults;

    public SailingNotificationServiceImpl(BundleContext bundleContext, MailQueue mailQueue) throws MalformedURLException {
        this(mailQueue,
                new BoatClassResultsNotificationSet(bundleContext),
                new BoatClassUpcomingRaceNotificationSet(bundleContext),
                new CompetitorResultsNotificationSet(bundleContext));
    }
    
    public SailingNotificationServiceImpl(MailQueue mailQueue, BoatClassResultsNotificationSet boatClassResults,
            BoatClassUpcomingRaceNotificationSet boatClassUpcomingRace,
            CompetitorResultsNotificationSet competitorResults) throws MalformedURLException {
        this.mailQueue = mailQueue;
        this.messages = ResourceBundleStringMessages.create(STRING_MESSAGES_BASE_NAME,
                this.getClass().getClassLoader(), StandardCharsets.UTF_8.name());
        toStop.add(this.boatClassResults = boatClassResults);
        toStop.add(this.boatClassUpcomingRace = boatClassUpcomingRace);
        toStop.add(this.competitorResults = competitorResults);
    }

    @Override
    public void stop() {
        toStop.forEach(Stoppable::stop);
    }

    public void setRacingEventService(RacingEventServiceImpl racingEventService) {
        this.racingEventService = racingEventService;
    }

    /**
     * Calculates the best matching {@link Event} and {@link LeaderboardGroup} for the given {@link Leaderboard}. If
     * there is just one Event/LeaderboardGroup, this pair is returned. If an event series is found and the associated
     * Event can be obtained, this combination is returned. The first Event of the series is used otherwise.
     */
    private Pair<Event, LeaderboardGroup> calculateAssociatedEventForLeaderboard(final Leaderboard leaderboard) {
        Set<Event> foundEvents = new LinkedHashSet<>();
        Set<LeaderboardGroup> foundLeaderboardGroups = new LinkedHashSet<>();
        racingEventService.getAllEvents().forEach(event -> {
            event.getLeaderboardGroups().forEach(leaderboardGroup -> {
                if (Util.contains(leaderboardGroup.getLeaderboards(), leaderboard)) {
                    foundEvents.add(event);
                    foundLeaderboardGroups.add(leaderboardGroup);
                }
            });
        });
        int foundEventsCount = foundEvents.size();
        if (foundEventsCount == 1) {
            // if multiple LeaderboardGroups of the single event reference the same leaderboard, we just use the
            // first LeaderboardGroup. This could be a non optimal match but helps to e.g. construct valid links.
            return new Pair<>(Util.get(foundEvents, 0), Util.get(foundLeaderboardGroups, 0));
        } else if (foundEventsCount > 1) {
            // could be a series
            for (final LeaderboardGroup leaderboardGroup : foundLeaderboardGroups) {
                if (leaderboardGroup.hasOverallLeaderboard()) {
                    for (Event event : foundEvents) {
                        if (Util.containsAny(event.getVenue().getCourseAreas(), leaderboard.getCourseAreas())) {
                            return new Pair<>(event, leaderboardGroup);
                        }
                    }
                }
                // Event <-> Leaderboard association is not set up correctly. The UI will show the Leaderboard for
                // multiple Events. So any of the found Events is ok for this case.
                return new Pair<>(Util.get(foundEvents, 0), Util.get(foundLeaderboardGroups, 0));
            }
        }
        // No associated event found or association is ambiguous
        return null;
    }

    /**
     * Calculated the best matching Event/LeaderBoardGroup pair via
     * {@link #calculateAssociatedEventForLeaderboard(Leaderboard)} and calls the given consumer with these instances.
     * If the racingEventService isn't already set, this will do nothing.
     */
    private void doWithEvent(Leaderboard leaderboard, BiConsumer<Event, LeaderboardGroup> consumer) {
        if (racingEventService == null) {
            logger.severe(
                    "Can't send notifications if " + getClass().getSimpleName() + ".racingEventService isn't set");
            return;
        }
        Pair<Event, LeaderboardGroup> eventAndLeaderboardGroup = calculateAssociatedEventForLeaderboard(leaderboard);
        if (eventAndLeaderboardGroup != null) {
            consumer.accept(eventAndLeaderboardGroup.getA(), eventAndLeaderboardGroup.getB());
        }
    }

    private String calculateRaceDescription(Locale locale, Event event, Leaderboard leaderboard, RaceColumn raceColumn,
            Fleet fleet) {
        String raceName = raceColumn.getName();
        String eventName = event.getName();
        String leaderboardDisplayName = leaderboard.getDisplayName() != null ? leaderboard.getDisplayName()
                : leaderboard.getName();
        if (Util.size(raceColumn.getFleets()) > 1) {
            return messages.get(locale, "raceInFleetInRegattaOfEvent", raceName, leaderboardDisplayName, eventName,
                    fleet.getName());
        } else {
            return messages.get(locale, "raceInRegattaOfEvent", raceName, leaderboardDisplayName, eventName);
        }
    }

    private String calculateLeaderboardDescription(Locale locale, Event event, Leaderboard leaderboard) {
        String eventName = event.getName();
        String leaderboardDisplayName = leaderboard.getDisplayName() != null ? leaderboard.getDisplayName()
                : leaderboard.getName();
        return messages.get(locale, "leaderboardOfEvent", leaderboardDisplayName, eventName);
    }
    
    private Pair<String, String> createRaceBoardShowRaceLink(TrackedRace trackedRace, Leaderboard leaderboard,
            Event event, LeaderboardGroup leaderboardGroup, Locale locale) {
        return createRaceBoardLinkWithMessage(trackedRace, leaderboard, event, leaderboardGroup, "PLAYER",
                "raceboardShowRaceLinkTitle", locale);
    }

    private Pair<String, String> createRaceBoardRaceAnalysisLink(TrackedRace trackedRace, Leaderboard leaderboard,
            Event event, LeaderboardGroup leaderboardGroup, Locale locale) {
        return createRaceBoardLinkWithMessage(trackedRace, leaderboard, event, leaderboardGroup, "FULL_ANALYSIS",
                "raceboardRaceAnalysisLinkTitle", locale);
    }

    private Pair<String, String> createRaceBoardLinkWithMessage(TrackedRace trackedRace, Leaderboard leaderboard, Event event,
            LeaderboardGroup leaderboardGroup, String raceboardMode, String labelMessageKey, Locale locale) {
        String link = RaceBoardLinkFactory.createRaceBoardLink(trackedRace, leaderboard, event, leaderboardGroup, raceboardMode, locale);
        return new Pair<String, String>(messages.get(locale, labelMessageKey), link);
    }

    private Pair<String, String> createHomeRacesListLink(Leaderboard leaderboard, Event event, Locale locale) {
        return createHomeRegattaLink("races", "racesOverviewLinkTitle", leaderboard, event, locale);
    }
    
    private Pair<String, String> createHomeLeaderboardLink(Leaderboard leaderboard, Event event,
            Locale locale) {
        return createHomeRegattaLink("leaderboard", "leaderboardShowResultsLinkTitle", leaderboard, event, locale);
    }
    
    private Pair<String, String> createHomeRegattaLink(String tab, String labelMessageKey, Leaderboard leaderboard,
            Event event, Locale locale) {
        String link = RaceBoardLinkFactory.getBaseURL(event).toString() + "/gwt/Home.html?locale=" + locale.toLanguageTag()
                + "#/regatta/" + tab + "/:eventId=" + event.getId() + "&regattaId=" + leaderboard.getName();
        return new Pair<String, String>(messages.get(locale, labelMessageKey), link);
    }
    
    @Override
    public void notifyUserOnBoatClassRaceChangesStateToFinished(BoatClass boatClass, TrackedRace trackedRace,
            Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet) {
        doWithEvent(leaderboard, (event, leaderboardGroup) -> {
            mailQueue.addNotification(new NotificationSetNotification<BoatClass>(boatClass, boatClassResults) {
                @Override
                protected NotificationMailTemplate getMailTemplate(BoatClass objectToNotifyAbout, Locale locale) {
                    String raceDescription = calculateRaceDescription(locale, event, leaderboard, raceColumn, fleet);
                    return new NotificationMailTemplate(
                            messages.get(locale, "boatClassRaceFinishedSubject", boatClass.getName()), 
                            messages.get(locale, "boatClassRaceFinishedBody", boatClass.getName(),
                                    raceDescription),
                            RaceBoardLinkFactory.getBaseURL(event),
                            createRaceBoardShowRaceLink(trackedRace, leaderboard, event, leaderboardGroup, locale),
                            createRaceBoardRaceAnalysisLink(trackedRace, leaderboard, event, leaderboardGroup, locale));
                }
            });
        });
    }

    @Override
    public void notifyUserOnBoatClassWhenScoreCorrectionsAreAvailable(BoatClass boatClass, Leaderboard leaderboard) {
        // TODO don't send notifications when a notification for the same boatClass / leaderboard has already been sent shortly before
        doWithEvent(leaderboard, (event, leaderboardGroup) -> {
            mailQueue.addNotification(new NotificationSetNotification<BoatClass>(boatClass, boatClassResults) {
                @Override
                protected NotificationMailTemplate getMailTemplate(BoatClass objectToNotifyAbout, Locale locale) {
                    String leaderboardDescription = calculateLeaderboardDescription(locale, event, leaderboard);
                    return new NotificationMailTemplate(
                            messages.get(locale, "boatClassScoreCorrectionSubject", boatClass.getName()), 
                            messages.get(locale, "boatClassScoreCorrectionBody", boatClass.getName(),
                                    leaderboardDescription),
                            RaceBoardLinkFactory.getBaseURL(event),
                            createHomeLeaderboardLink(leaderboard, event, locale));
                }
            });
        });
    }

    public void notifyUserOnBoatClassUpcomingRace(BoatClass boatClass, Leaderboard leaderboard, RaceColumn raceColumn,
            Fleet fleet,  TimePoint when) {
        doWithEvent(leaderboard, (event, leaderboardGroup) -> {
            mailQueue.addNotification(new NotificationSetNotification<BoatClass>(boatClass, boatClassUpcomingRace) {
                @Override
                protected NotificationMailTemplate getMailTemplate(BoatClass objectToNotifyAbout, Locale locale) {
                    String raceDescription = calculateRaceDescription(locale, event, leaderboard, raceColumn, fleet);
                    String time = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG, locale)
                            .format(when.asDate());
                    return new NotificationMailTemplate(
                            messages.get(locale, "boatClassUpcomingRaceSubject", boatClass.getName()),
                            messages.get(locale, "boatClassUpcomingRaceBody", boatClass.getName(),
                                    raceDescription, time),
                            RaceBoardLinkFactory.getBaseURL(event),
                            createHomeRacesListLink(leaderboard, event, locale));
                }
            });
        });
    }

    @Override
    public void notifyUserOnCompetitorPassesFinish(Competitor competitor, TrackedRace trackedRace,
            Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet) {
        doWithEvent(leaderboard, (event, leaderboardGroup) -> {
            mailQueue.addNotification(new NotificationSetNotification<String>(competitor.getId().toString(), competitorResults) {
                @Override
                protected NotificationMailTemplate getMailTemplate(String objectToNotifyAbout, Locale locale) {
                    String raceDescription = calculateRaceDescription(locale, event, leaderboard, raceColumn, fleet);
                    return new NotificationMailTemplate(
                            messages.get(locale, "competitorPassesFinishSubject", competitor.getName()),
                            messages.get(locale, "competitorPassesFinishBody", competitor.getName(), raceDescription),
                            RaceBoardLinkFactory.getBaseURL(event),
                            createRaceBoardShowRaceLink(trackedRace, leaderboard, event, leaderboardGroup, locale),
                            createRaceBoardRaceAnalysisLink(trackedRace, leaderboard, event, leaderboardGroup, locale));
                }
            });
        });
    }

    @Override
    public void notifyUserOnCompetitorScoreCorrections(Competitor competitor, Leaderboard leaderboard) {
        doWithEvent(leaderboard, (event, leaderboardGroup) -> {
            mailQueue.addNotification(new NotificationSetNotification<String>(competitor.getId().toString(), competitorResults) {
                @Override
                protected NotificationMailTemplate getMailTemplate(String objectToNotifyAbout, Locale locale) {
                    String leaderboardDescription = calculateLeaderboardDescription(locale, event, leaderboard);
                    return new NotificationMailTemplate(
                            messages.get(locale, "competitorScoreCorrectionSubject", competitor.getName()),
                            messages.get(locale, "competitorScoreCorrectionBody", competitor.getName(),
                                    leaderboardDescription),
                            RaceBoardLinkFactory.getBaseURL(event),
                            createHomeLeaderboardLink(leaderboard, event, locale));
                }
            });
        });
    }
}
