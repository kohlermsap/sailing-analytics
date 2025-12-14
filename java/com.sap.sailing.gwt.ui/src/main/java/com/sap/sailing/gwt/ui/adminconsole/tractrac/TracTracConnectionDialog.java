package com.sap.sailing.gwt.ui.adminconsole.tractrac;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.adminconsole.TracTracEventManagementPanel;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationWithSecurityDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.security.ui.client.UserService;

/**
 * Creates a {@link TracTracConfigurationWithSecurityDTO} object. Can be accessed from
 * {@link TracTracEventManagementPanel}<p>
 * 
 * When the password field is empty, {@code null} will be set as the value of the
 * {@link TracTracConfigurationWithSecurityDTO#getTracTracPassword() password field} in the
 * result which shall be interpreted as "don't update" because TracTrac does not support
 * empty passwords.
 */
public class TracTracConnectionDialog extends DataEntryDialog<TracTracConfigurationWithSecurityDTO> {
    private static final StringMessages stringMessages = StringMessages.INSTANCE;
    private Grid grid;

    protected TextBox storedURITextBox;
    protected TextBox liveURITextBox;
    protected TextBox jsonURLTextBox;
    protected TextBox tracTracUpdateURITextBox;
    protected TextBox tracTracApiTokenTextBox;
    protected boolean tracTracApiTokenAvailable;
    protected PasswordTextBox tractracPasswordTextBox;
    private final ErrorReporter errorReporter;
    protected String creatorName;
    protected String name;

    /**
     * The class creates the UI-dialog to create a {@link TracTracConfigurationWithSecurityDTO}.
     */
    public TracTracConnectionDialog(
            final DialogCallback<TracTracConfigurationWithSecurityDTO> callback, final UserService userService,
            final ErrorReporter errorReporter) {
        super(stringMessages.tracTracConnection(), /* message */null, stringMessages.ok(), stringMessages.cancel(),
                /* validator */ null, /* animationEnabled */true, callback);
        this.errorReporter = errorReporter;
        this.ensureDebugId("TracTracConnectionDialog");
        createUi();
    }

    private void createUi() {
        grid = new Grid(6, 3);
        grid.setWidget(0, 0, new Label(stringMessages.details() + ":"));
        Label liveURILabel = new Label(stringMessages.liveUri() + ":");
        liveURILabel.setTitle(stringMessages.leaveEmptyForDefault());
        liveURITextBox = createTextBox("");
        liveURITextBox.ensureDebugId("LiveURITextBox");
        liveURITextBox.setVisibleLength(40);
        liveURITextBox.setTitle(stringMessages.leaveEmptyForDefault());
        grid.setWidget(1, 0, liveURILabel);
        grid.setWidget(1, 1, liveURITextBox);
        Label storedURILabel = new Label(stringMessages.storedUri() + ":");
        storedURILabel.setTitle(stringMessages.leaveEmptyForDefault());
        storedURITextBox = createTextBox("");
        storedURITextBox.ensureDebugId("StoredURITextBox");
        storedURITextBox.setVisibleLength(40);
        storedURITextBox.setTitle(stringMessages.leaveEmptyForDefault());
        grid.setWidget(2, 0, storedURILabel);
        grid.setWidget(2, 1, storedURITextBox);
        // JSON URL
        Label jsonURLLabel = new Label(stringMessages.jsonUrl() + ":");
        jsonURLTextBox = createTextBox("");
        jsonURLTextBox.ensureDebugId("JsonURLTextBox");
        jsonURLTextBox.setVisibleLength(100);
        // validation: User should not create empty connections
        jsonURLTextBox.addKeyUpHandler(e -> super.getOkButton().setEnabled(!jsonURLTextBox.getText().isEmpty()));
        grid.setWidget(3, 0, jsonURLLabel);
        grid.setWidget(3, 1, jsonURLTextBox);
        // Course design Update URL
        Label tracTracUpdateURLLabel = new Label(stringMessages.tracTracUpdateUrl() + ":");
        tracTracUpdateURITextBox = createTextBox("");
        tracTracUpdateURITextBox.ensureDebugId("TracTracUpdateURITextBox");
        tracTracUpdateURITextBox.setVisibleLength(100);
        tracTracUpdateURITextBox.setTitle(stringMessages.leaveEmptyForDefault());
        grid.setWidget(4, 0, tracTracUpdateURLLabel);
        grid.setWidget(4, 1, tracTracUpdateURITextBox);
        // TracTrac Username
        tracTracApiTokenTextBox = createTextBox("");
        tracTracApiTokenTextBox.ensureDebugId("TracTracApiTokenTextBox");
        tracTracApiTokenTextBox.setVisibleLength(40);
        grid.setWidget(5, 0, new Label(stringMessages.tractracApiToken() + ":"));
        grid.setWidget(5, 1, tracTracApiTokenTextBox);
        final Button testConnectionButton = new Button(stringMessages.testConnection());
        testConnectionButton.addClickHandler(e->testTracTracConnection());
        testConnectionButton.setEnabled(false); // need active entry of API token
        tracTracApiTokenTextBox.addKeyUpHandler(e -> testConnectionButton.setEnabled(Util.hasLength(tracTracApiTokenTextBox.getValue())));
        grid.setWidget(5, 2, testConnectionButton);
    }

    private void testTracTracConnection() {
        // need to obtain event ID and perhaps server_update_uri from document referenced by JSON URL
        final RequestBuilder rbJsonUrl = new RequestBuilder(RequestBuilder.GET, jsonURLTextBox.getValue());
        try {
            rbJsonUrl.sendRequest(/* request data */ null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (200 == response.getStatusCode()) {
                        // Success
                        final String responseText = response.getText();
                        final JSONValue jsonResult = JSONParser.parseStrict(responseText);
                        if (jsonResult instanceof JSONObject) {
                            final JSONObject jsonObject = (JSONObject) jsonResult;
                            final JSONObject eventJson = (JSONObject) jsonObject.get("event");
                            final String eventId = eventJson.get("id").isString().stringValue();
                            String serverUpdateURI = null;
                            if (!Util.hasLength(tracTracUpdateURITextBox.getValue())) {
                                final JSONString serverUpdateURIJSONString = eventJson.get("server_update_uri").isString();
                                if (serverUpdateURIJSONString == null) {
                                    errorReporter.reportError(stringMessages.tracTracConnectionTestFailed("server_update_uri"));
                                    serverUpdateURI = null;
                                } else {
                                    serverUpdateURI = serverUpdateURIJSONString.stringValue();
                                }
                            } else {
                                serverUpdateURI = tracTracUpdateURITextBox.getValue();
                            }
                            if (serverUpdateURI == null) {
                                errorReporter.reportError(stringMessages.tracTracConnectionTestFailed("server_update_uri"));
                            } else {
                                 testTracTracConnectionWithUpdateURI(serverUpdateURI, eventId);
                            }
                        } else {
                            errorReporter.reportError(stringMessages.tracTracConnectionTestFailed(jsonResult.getClass().getName()));
                        }
                      } else {
                          errorReporter.reportError(stringMessages.tracTracConnectionTestFailed(response.getStatusText()));
                      }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    errorReporter.reportError(stringMessages.tracTracConnectionTestFailed(exception.getMessage()));
                }
            });
        } catch (RequestException e) {
            errorReporter.reportError(stringMessages.tracTracConnectionTestFailed(e.getMessage()));
        }
    }

    private void testTracTracConnectionWithUpdateURI(String serverUpdateURI, String eventId) {
        final RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, serverUpdateURI+"/api/v3/valid?eventId="+eventId);
        rb.setHeader("Authorization", "Bearer " + tracTracApiTokenTextBox.getValue());
        try {
            rb.sendRequest(/* request data */ null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (200 == response.getStatusCode()) {
                        Window.alert(stringMessages.ok()+"\n"+response.getText());
                    } else {
                        errorReporter.reportError(stringMessages.tracTracConnectionTestFailed(response.getStatusText()));
                    }                
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    errorReporter.reportError(stringMessages.tracTracConnectionTestFailed(exception.getMessage()));
                }
            });
        } catch (RequestException e) {
            errorReporter.reportError(stringMessages.tracTracConnectionTestFailed(e.getMessage()));
        }
    }
    
    @Override
    protected Focusable getInitialFocusWidget() {
        return jsonURLTextBox;
    }

    @Override
    protected TracTracConfigurationWithSecurityDTO getResult() {
        final String jsonURL = jsonURLTextBox.getValue();
        final String liveDataURI = liveURITextBox.getValue();
        final String storedDataURI = storedURITextBox.getValue();
        final String courseDesignUpdateURI = tracTracUpdateURITextBox.getValue();
        final String tracTracApiToken = tracTracApiTokenTextBox.getValue();
        return new TracTracConfigurationWithSecurityDTO(name, jsonURL, liveDataURI, storedDataURI,
                courseDesignUpdateURI, tracTracApiToken, tracTracApiTokenAvailable || /* tracTracApiTokenAvailable */ Util.hasLength(tracTracApiToken), creatorName);
    }

    @Override
    protected Widget getAdditionalWidget() {
        return grid;
    }

}
