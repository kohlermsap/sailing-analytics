package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.BranchIO;
import com.sap.sailing.domain.common.MailInvitationType;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.controls.GenericListBox;
import com.sap.sse.gwt.client.controls.GenericListBox.ValueBuilder;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class RegistrationLinkWithQRCodeDialog extends DataEntryDialog<RegistrationLinkWithQRCode> {

    private final String regattaName;

    private final SailingServiceAsync sailingService;
    private final StringMessages stringMessages;
    private final CaptionPanel registrationLinkPanel;
    private final VerticalPanel registrationLinkPanelContent;
    private final Label registrationLinkExplain;
    private final CaptionPanel barcodePanel;
    private final VerticalPanel barcodePanelContent;
    private final Label barcodeExplainLabel;
    private final TextBox urlTextBox;
    private final Image qrCodeImage;
    private final String secret;
    private final GenericListBox<EventDTO> events;

    private RegistrationLinkWithQRCode registrationLinkWithQRCode;
    private MailInvitationType invitationType;

    public RegistrationLinkWithQRCodeDialog(final SailingServiceAsync sailingService,
            final StringMessages stringMessages, String regattaName,
            RegistrationLinkWithQRCode registrationLinkWithQRCode,
            DialogCallback<RegistrationLinkWithQRCode> callback, final String secret,
            MailInvitationType invitationType) {
        super(stringMessages.registrationLinkDialog(), stringMessages.explainRegistrationLinkDialog(),
                stringMessages.ok(), stringMessages.cancel(), null, true, callback);
        this.sailingService = sailingService;
        this.stringMessages = stringMessages;
        this.regattaName = regattaName;
        this.invitationType = invitationType;
        this.registrationLinkWithQRCode = registrationLinkWithQRCode == null ? new RegistrationLinkWithQRCode()
                : registrationLinkWithQRCode;
        registrationLinkPanel = new CaptionPanel(stringMessages.registrationLinkUrl());
        registrationLinkPanelContent = new VerticalPanel();
        registrationLinkPanel.add(registrationLinkPanelContent);
        final Label eventExplain = new Label(stringMessages.event());
        events = createGenericListBox(new ValueBuilder<EventDTO>() {
            @Override
            public String getValue(EventDTO item) {
                if (item == null) {
                    return "";
                }
                return item.getName();
            }
        }, false);
        sailingService.getEventsForLeaderboard(regattaName, new AsyncCallback<Collection<EventDTO>>() {
            @Override
            public void onSuccess(Collection<EventDTO> result) {
                events.addItems(result);
                updateDisplayWidgets();
            }

            @Override
            public void onFailure(Throwable caught) {
                Notification.notify("Could not load events: " + caught.getMessage(), NotificationType.ERROR);
            }
        });
        events.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                updateDisplayWidgets();
            }
        });
        registrationLinkPanelContent.add(eventExplain);
        registrationLinkPanelContent.add(events);
        registrationLinkExplain = new Label(stringMessages.registrationLinkUrlExplain());
        registrationLinkPanelContent.add(registrationLinkExplain);
        urlTextBox = createTextBox("URL", 100);
        urlTextBox.ensureDebugId("RegistrationLinkUrl");
        registrationLinkPanelContent.add(urlTextBox);
        Anchor copyAnchor = new Anchor(stringMessages.copyToClipboard());
        copyAnchor.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                urlTextBox.setFocus(true);
                urlTextBox.selectAll();
                copyToClipboard();
            }
        });
        registrationLinkPanelContent.add(copyAnchor);
        barcodePanel = new CaptionPanel(stringMessages.registrationLinkDialogQrcode());
        barcodePanelContent = new VerticalPanel();
        barcodePanel.add(barcodePanelContent);
        barcodeExplainLabel = new Label(stringMessages.registrationLinkDialogQrcodeExplain());
        barcodePanelContent.add(barcodeExplainLabel);
        qrCodeImage = new Image();
        qrCodeImage.ensureDebugId("OpenRegattaRegistrationLinkQrCode");
        barcodePanelContent.add(qrCodeImage);
        this.secret = secret;
    }

    @Override
    protected RegistrationLinkWithQRCode getResult() {
        return registrationLinkWithQRCode;
    }

    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel panel = new VerticalPanel();
        Widget additionalWidget = super.getAdditionalWidget();
        if (additionalWidget != null) {
            panel.add(additionalWidget);
        }
        final VerticalPanel dialogPanel = new VerticalPanel();
        panel.add(dialogPanel);
        dialogPanel.add(registrationLinkPanel);
        dialogPanel.add(barcodePanel);
        return panel;
    }

    private void updateDisplayWidgets() {
        if (events.getValue() == null) {
            Notification.notify(stringMessages.noEventSelected(), NotificationType.ERROR);
            return;
        }
        String baseUrl = GWT.getHostPageBaseURL();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/"));
            baseUrl = baseUrl.substring(0, baseUrl.indexOf("/gwt"));
        }
        final String eventIdAsString = events.getValue().id.toString();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("regatta_name", regattaName);
        parameters.put("secret", secret);
        parameters.put("server", baseUrl);
        parameters.put("event_id", eventIdAsString);
        if (invitationType.isSupportsOpenRegattas()) {
            String deeplinkUrl = BranchIO.generateLink(invitationType.getBranchIOopenRegattaURL(), parameters,
                    URL::encodeQueryString);
            urlTextBox.setText(deeplinkUrl);
            sailingService.openRegattaRegistrationQrCode(deeplinkUrl, new AsyncCallback<String>() {
                @Override
                public void onFailure(Throwable caught) {
                    GWT.log("Qrcode generation failed: ", caught);
                }
    
                @Override
                public void onSuccess(String result) {
                    GWT.log("Qrcode generated for url: " + deeplinkUrl);
                    qrCodeImage.setUrl("data:image/png;base64, " + result);
                }
            });
        } else {
            if (ClientConfiguration.getInstance().isBrandingActive()){
                getStatusLabel().setText(stringMessages.warningSailInsightVersion(ClientConfiguration.getInstance().getBrandTitle(Optional.empty())));

            } else {
                getStatusLabel().setText(stringMessages.warningSailInsightVersion(""));

            }
            getStatusLabel().setStyleName("errorLabel");
        }
    }

    private static native boolean copyToClipboard() /*-{
        return $doc.execCommand('copy');
    }-*/;

}
