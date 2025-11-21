package com.sap.sailing.server.gateway.trackfiles.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.json.simple.JSONObject;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.common.dto.ExpeditionAllInOneConstants;
import com.sap.sailing.domain.common.dto.ExpeditionAllInOneConstants.ImportMode;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapterFactory;
import com.sap.sailing.server.gateway.impl.AbstractFileUploadServlet;
import com.sap.sailing.server.gateway.trackfiles.impl.ExpeditionAllInOneImporter.ImporterResult;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.util.ServiceTrackerFactory;

/**
 * Import servlet for sensor data files. Importers are located through the OSGi service registry and matched against the
 * name provided by the upload form.
 * 
 * @see ExpeditionAllInOneImporter
 */
public class ExpeditionAllInOneImportServlet extends AbstractFileUploadServlet {
    private static final long serialVersionUID = 1120226743039934620L;
    private static final Logger logger = Logger.getLogger(ExpeditionAllInOneImportServlet.class.getName());

    private static final String STRING_MESSAGES_BASE_NAME = "stringmessages/StringMessages";

    private ServiceTracker<RaceLogTrackingAdapterFactory, RaceLogTrackingAdapterFactory> raceLogTrackingAdapterTracker;
    private ResourceBundleStringMessages serverStringMessages;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        raceLogTrackingAdapterTracker = ServiceTrackerFactory.createAndOpen(getContext(),
                RaceLogTrackingAdapterFactory.class);
        serverStringMessages = ResourceBundleStringMessages.create(STRING_MESSAGES_BASE_NAME,
                this.getClass().getClassLoader(), StandardCharsets.UTF_8.name());
    }

    /**
     * Process the uploaded file items.
     */
    @Override
    protected void process(List<FileItem> fileItems, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        ImporterResult importerResult = null;
        try {
            String fileName = null;
            FileItem fileItem = null;
            String boatClassName = null;
            String regattaName = null;
            String importModeName = null;
            String localeName = null;
            boolean importStartData = false;
            for (FileItem fi : fileItems) {
                if (!fi.isFormField()) {
                    fileName = fi.getName();
                    fileItem = fi;
                } else if (fi.getFieldName() != null) {
                    if (ExpeditionAllInOneConstants.REQUEST_PARAMETER_BOAT_CLASS.equals(fi.getFieldName())) {
                        boatClassName = fi.getString();
                    }
                    if (ExpeditionAllInOneConstants.REQUEST_PARAMETER_REGATTA_NAME.equals(fi.getFieldName())) {
                        regattaName = fi.getString();
                    }
                    if (ExpeditionAllInOneConstants.REQUEST_PARAMETER_IMPORT_MODE.equals(fi.getFieldName())) {
                        importModeName = fi.getString();
                    }
                    if (ExpeditionAllInOneConstants.REQUEST_PARAMETER_LOCALE.equals(fi.getFieldName())) {
                        localeName = fi.getString();
                    }
                    if (ExpeditionAllInOneConstants.REQUEST_PARAMETER_IMPORT_START_DATA.equals(fi.getFieldName())) {
                        importStartData = Boolean.valueOf(fi.getString().equalsIgnoreCase("on"));
                    }
                }
            }
            Locale uiLocale;
            if (localeName == null) {
                uiLocale = Locale.ENGLISH;
            } else {
                try {
                    uiLocale = Locale.forLanguageTag(localeName);
                } catch (Exception e) {
                    uiLocale = Locale.ENGLISH;
                }
            }
            if (fileItem == null) {
                throw new AllInOneImportException(serverStringMessages.get(uiLocale, "allInOneErrorImportFileMissing"));
            }
            final ImportMode importMode;
            if (importModeName == null) {
                importMode = ImportMode.NEW_EVENT;
            } else {
                try {
                    importMode = ImportMode.valueOf(importModeName);
                } catch (Exception e) {
                    throw new AllInOneImportException(serverStringMessages.get(uiLocale, "allInOneErrorUnknownImportMode"));
                }
            }
            if (importMode == ImportMode.NEW_EVENT) {
                if (boatClassName == null || boatClassName.isEmpty()) {
                    throw new AllInOneImportException(serverStringMessages.get(uiLocale, "allInOneErrorMissingBoatClass"));
                }
            } else {
                if (regattaName == null || regattaName.isEmpty()) {
                    throw new AllInOneImportException(serverStringMessages.get(uiLocale, "allInOneErrorMissingRegattaClass"));
                }
            }
            importerResult = new ExpeditionAllInOneImporter(serverStringMessages, uiLocale, getService(),
                    getSecurityService(), getRaceLogTrackingAdapter(), getServiceFinderFactory(), getContext())
                            .importFiles(fileName, fileItem, boatClassName, importMode, regattaName, importStartData);
        } catch (AllInOneImportException e) {
            importerResult = new ImporterResult(e, e.additionalErrors);
            logger.log(Level.SEVERE, e.getMessage());
        } catch (Throwable t) {
            importerResult = new ImporterResult(t, Collections.emptyList());
            logger.log(Level.SEVERE, t.getMessage());
        } finally {
            writeJsonIntoHtmlResponse(resp, toJSON(importerResult));
        }
    }

    private RaceLogTrackingAdapter getRaceLogTrackingAdapter() {
        return raceLogTrackingAdapterTracker.getService().getAdapter(getService().getBaseDomainFactory());
    }

    private JSONObject toJSON(ImporterResult importerResult) {
        final JSONObject json = new JSONObject();
        json.put(ExpeditionAllInOneConstants.RESPONSE_EVENT_ID, importerResult.eventId == null ? null : importerResult.eventId.toString());
        json.put(ExpeditionAllInOneConstants.RESPONSE_LEADER_BOARD_NAME, importerResult.leaderboardName);
        json.put(ExpeditionAllInOneConstants.RESPONSE_LEADER_BOARD_GROUP_NAME, importerResult.leaderboardGroupName);
        json.put(ExpeditionAllInOneConstants.RESPONSE_REGATTA_NAME, importerResult.regattaName);
        json.put(ExpeditionAllInOneConstants.RESPONSE_RACE_LIST, ImportResultSerializer.serializeRaceList(importerResult.raceNameRaceColumnNameFleetnameList));
        json.put(ExpeditionAllInOneConstants.RESPONSE_ERRORS, ImportResultSerializer.serializeErrorList(importerResult.errorList));
        json.put(ExpeditionAllInOneConstants.RESPONSE_GPS_DEVICE_IDS, ImportResultSerializer.serializeTrackList(importerResult.importGpsFixData));
        json.put(ExpeditionAllInOneConstants.RESPONSE_SENSOR_DEVICE_IDS, ImportResultSerializer.serializeTrackList(importerResult.importSensorFixData));
        json.put(ExpeditionAllInOneConstants.RESPONSE_SENSOR_FIX_IMPORTER_TYPE, importerResult.sensorFixImporterType);
        json.put(ExpeditionAllInOneConstants.RESPONSE_START_TIMES, importerResult.startData==null ? null :
            ImportResultSerializer.serializeIterable(importerResult.startData.getStartTimes(), startTime->startTime.asMillis()));
        return json;
    }
}
