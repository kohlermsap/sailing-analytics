package com.sap.sailing.gwt.home.shared.places.user.profile.preferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sap.sailing.gwt.home.shared.partials.checkboxList.CheckboxListEntryModel;

public class MiscellaneousAlertsPresenter {
    public Set<CheckboxListEntryModel> composeEntries() {
        final CheckboxListEntryModel passwordReset = new CheckboxListEntryModel("Password Reset", true, null);
        final CheckboxListEntryModel.ChangeHandler securityAlertsSubscriptionChangeHandler = new CheckboxListEntryModel.ChangeHandler(
                this::setSecurityAlertsSubscription, true, "Successfully changed ", "fail");
        final CheckboxListEntryModel securityAlerts = new CheckboxListEntryModel("Security Alerts", true,
                securityAlertsSubscriptionChangeHandler);
        final CheckboxListEntryModel.ChangeHandler newsletterSubscriptionChangeHandler = new CheckboxListEntryModel.ChangeHandler(
                this::setNewsletterSubscription, true, "succ", "fail");
        final CheckboxListEntryModel newsletter = new CheckboxListEntryModel("Newsletter", false,
                newsletterSubscriptionChangeHandler);
        final Set<CheckboxListEntryModel> entries = new HashSet<>(
                Arrays.asList(passwordReset, securityAlerts, newsletter));
        new CheckboxListEntryModel(null, false, newsletterSubscriptionChangeHandler);
        return entries;
    }

    private void setSecurityAlertsSubscription(final boolean value) {
    }

    private void setNewsletterSubscription(final boolean value) {
    }
}