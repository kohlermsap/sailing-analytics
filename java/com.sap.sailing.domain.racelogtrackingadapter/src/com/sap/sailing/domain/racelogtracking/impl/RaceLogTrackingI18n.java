package com.sap.sailing.domain.racelogtracking.impl;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.sap.sse.i18n.ResourceBundleStringMessages;

public class RaceLogTrackingI18n {
    private static final String RESOURCE_BASE_NAME = "stringmessages/TrackingStringMessages";

    public static final ResourceBundleStringMessages STRING_MESSAGES = ResourceBundleStringMessages.create(
            RESOURCE_BASE_NAME, RaceLogTrackingI18n.class.getClassLoader(), StandardCharsets.UTF_8.name());

    public static String trackingInvitationFor(final Locale locale, final String invitee) {
        return STRING_MESSAGES.get(locale, "trackingInvitationFor") + " " + invitee;
    }

    public static String followBranchDeeplink(final Locale locale, final String appName, final String invitee) {
        return STRING_MESSAGES.get(locale, "followBranchDeeplink", appName, invitee);
    }
    
    public static String register(final Locale locale) {
        return STRING_MESSAGES.get(locale, "register");
    }

    public static String welcomeTo(final Locale locale, final String eventName, final String leaderboardName) {
        return STRING_MESSAGES.get(locale, "welcomeTo") + " " + eventName + ", " + leaderboardName;
    }

    public static String buoyTender(final Locale locale) {
        return STRING_MESSAGES.get(locale, "buoyTender");
    }

    public static String buoyPingerAppName(final Locale locale) {
        return STRING_MESSAGES.get(locale, "buoyPingerAppName");
    }

    public static String sailInSightAppName(final Locale locale) {
        return STRING_MESSAGES.get(locale, "sailInSightAppName");
    }

    // TODO: DELETE following once legacy is removed
    public static String iOSUsers(final Locale locale) {
        return STRING_MESSAGES.get(locale, "iOSUsers");
    }

    public static String androidUsers(final Locale locale) {
        return STRING_MESSAGES.get(locale, "androidUsers");
    }

    public static String alternativelyVisitThisLink(final Locale locale) {
        return STRING_MESSAGES.get(locale, "alternativelyVisitThisLink");
    }

    public static String appIos(final Locale locale) {
        return STRING_MESSAGES.get(locale, "appIos");
    }

    public static String appAndroid(final Locale locale) {
        return STRING_MESSAGES.get(locale, "appAndroid");
    }

    public static String scanQRCodeOrVisitUrlToRegisterAs(final Locale locale, final String appName) {
        return STRING_MESSAGES.get(locale, "scanQRCodeOrVisitUrlToRegisterAs", appName);
    }

    public static String appStoreInstallText(final Locale locale) {
        return STRING_MESSAGES.get(locale, "appStoreInstallText");
    }
}
