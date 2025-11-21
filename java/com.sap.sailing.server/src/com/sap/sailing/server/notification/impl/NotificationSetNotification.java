package com.sap.sailing.server.notification.impl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.mail.MailService;
import com.sap.sse.mail.SerializableDefaultMimeBodyPartSupplier;
import com.sap.sse.mail.SerializableFileMimeBodyPartSupplier;
import com.sap.sse.mail.SerializableMultipartSupplier;
import com.sap.sse.mail.queue.MailNotification;
import com.sap.sse.security.PreferenceObjectBasedNotificationSet;
import com.sap.sse.security.shared.impl.User;

public abstract class NotificationSetNotification<T> implements MailNotification {
    private static final Logger logger = Logger.getLogger(NotificationSetNotification.class.getName());
    
    private static final String TEMPLATE_FILE = "notification-mail-template.html";
    private static final String LOGO_FILE = "sap_logo_header.png";

    private static final String TEMPLATE = loadTemplateFile();
    private static final byte[] LOGO_BYTES = loadLogoFile();

    private static String loadTemplateFile() {
        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                NotificationSetNotification.class.getResourceAsStream(TEMPLATE_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException exc) {
            logger.log(Level.SEVERE, "Error while loading notification mail template!", exc);
        }
        return content.toString();
    }

    private static byte[] loadLogoFile() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream in = NotificationSetNotification.class.getResourceAsStream(LOGO_FILE)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) >= 0) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        } catch (IOException exc) {
            logger.log(Level.SEVERE, "Error while loading notification mail template!", exc);
            return null;
        }
    }

    private final T objectToNotifyAbout;
    private final PreferenceObjectBasedNotificationSet<?, T> associatedNotificationSet;
    private static final ResourceBundleStringMessages messages = ResourceBundleStringMessages.create(
            SailingNotificationServiceImpl.STRING_MESSAGES_BASE_NAME,
            NotificationSetNotification.class.getClassLoader(), StandardCharsets.UTF_8.name());

    public NotificationSetNotification(T objectToNotifyAbout, PreferenceObjectBasedNotificationSet<?, T> associatedNotificationSet) {
        this.objectToNotifyAbout = objectToNotifyAbout;
        this.associatedNotificationSet = associatedNotificationSet;

    }

    @Override
    public void sendNotifications(final MailService mailService) {
        // TODO idea (c) by Axel Uhl: In case of performance/mail queue problems: group mail addresses by locale and
        // send batches with the actual mail addresses as bcc?
        associatedNotificationSet.forUsersWithVerifiedEmailMappedTo(objectToNotifyAbout, (user) -> {
            Locale locale = user.getLocaleOrDefault();
            final NotificationMailTemplate mailTemplate = getMailTemplate(objectToNotifyAbout, locale);
            try {
                final SerializableMultipartSupplier multipartSupplier = new SerializableMultipartSupplier("related");
                multipartSupplier.addBodyPart(new SerializableDefaultMimeBodyPartSupplier(
                        getMailContent(mailTemplate, user, locale), "text/html"));
                multipartSupplier.addBodyPart(new SerializableFileMimeBodyPartSupplier(LOGO_BYTES, "image/png",
                        "saplogo", "saplogo.png"));
                mailService.sendMail(user.getEmail(), mailTemplate.getSubject(), multipartSupplier);
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                        "Could not send mail notification for \"" + objectToNotifyAbout + "\" to user \"" + user + "\"", e);
            }
        });
    }
    
    private String getMailContent(NotificationMailTemplate notificationMailTemplate, User user, Locale locale) {
        StringBuilder bodyContent = new StringBuilder();
        if (notificationMailTemplate.getTitle() != null) {
            bodyContent.append("<h1>")
                .append(notificationMailTemplate.getTitle())
                .append("</h1>");
        }
        String name = user.getFullName() == null || user.getFullName().isEmpty() ? user.getName() : user.getFullName();
        bodyContent.append("<div class=\"paragraph\" style=\" border-top: 5px solid white; border-bottom: 5px solid white;\">")
                .append(htmlify(messages.get(locale, "salutation", name)))
                .append("</div>");
        bodyContent.append("<div class=\"paragraph\" style=\" border-top: 5px solid white; border-bottom: 5px solid white;\">")
                .append(htmlify(notificationMailTemplate.getText()))
                .append("</div>");
        StringBuilder buttons = new StringBuilder();
        for (Pair<String, String> link : notificationMailTemplate.getLabelsAndLinkUrls()) {
            buttons.append("<span class=\"buttonContainer\" style=\"border-top: 10px solid white; border-right: 10px solid white;\">")
                .append("<a class=\"linkButton\" href=\"")
                .append(link.getB())
                .append("\" style=\"display:inline-block;background-color:#337ab7;border-radius:4px;color:#ffffff;border:1px solid #2e6da4;text-decoration:none;\">")
                .append("<span class=\"linkButtonContent\" style=\"border:15px solid #337ab7;display:inline-block;background-color: #337ab7;\">")
                .append(htmlify(link.getA()).replaceAll(" ", "&nbsp;"))
                .append("</span>")
                .append("</a> ")
                .append("</span>");
        }
        String siteLink = "<a href=\"" + notificationMailTemplate.getServerBaseUrl() + "/gwt/Home.html\">"
                + notificationMailTemplate.getServerBaseUrl() + "</a>";
        StringBuilder footerLinks = new StringBuilder();
        footerLinks.append("<a href=\"")
                .append(notificationMailTemplate.getServerBaseUrl())
                .append("/gwt/Home.html#/user/profile/:\">")
                .append(htmlify(messages.get(locale, "userProfile")))
                .append("</a>")
                .append(" | ");
        footerLinks.append("<a href=\"http://go.sap.com/about/legal/impressum.html?campaigncode=CRM-XH21-OSP-Sailing\">")
                .append(htmlify(messages.get(locale, "imprint")))
                .append("</a>")
                .append(" | ");
        footerLinks.append("<a href=\"http://go.sap.com/about/legal/privacy.html?campaigncode=CRM-XH21-OSP-Sailing\">")
                .append(htmlify(messages.get(locale, "privacy")))
                .append("</a>");

        String subscriptionInformation = htmlify(messages.get(locale, "subscriptionInformation"));
        return TEMPLATE
                .replace("${title}", notificationMailTemplate.getSubject())
                .replace("${content}", bodyContent.toString())
                .replace("${buttons}", buttons.toString())
                .replace("${subscription_information}", subscriptionInformation)
                .replace("${site}", siteLink) //
                .replace("${footer_links}", footerLinks.toString())
        ;
    }
    
    private String htmlify(String source) {
        StringBuilder result = new StringBuilder();
        for (char c : source.toCharArray()) {
            result.append(("\"<>&".indexOf(c) >= 0 || c > 127) ? ( "&#" + (int) c + ";") : Character.toString(c));
        }
        return result.toString();
    }
    
    protected abstract NotificationMailTemplate getMailTemplate(T objectToNotifyAbout, Locale locale);

}