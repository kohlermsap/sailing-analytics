package com.sap.sailing.server.gateway.trackfiles.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItem;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.DoubleVectorFixImporter;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.server.gateway.trackfiles.impl.ImportResult.TrackImportDTO;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.util.FileItemHelper;

public class SensorDataImporter {
    private static final Logger logger = Logger.getLogger(SensorDataImporter.class.getName());
    
    private final RacingEventService service;
    private final BundleContext context;

    public SensorDataImporter(RacingEventService service, BundleContext context) {
        this.service = service;
        this.context = context;
    }

    /**
     * Searches the requested importer in the importers provided by the OSGi registry and imports the priovided sensor
     * data file.
     * 
     * @param importerNamesAndFiles
     *            the file items together with the names of the importer to use for importing the respective file's
     *            contents; the importer names are matched against {@link DoubleVectorFixImporter#getType()} for all
     *            importers found registered in the OSGi registry. The first matching importer is used for the file. The
     *            importer is selected on a per-file basis.
     * 
     * @throws IOException
     */
    public void importFiles(boolean enableDownsampler, ImportResult result, Iterable<Pair<String, FileItem>> importerNamesAndFiles)
            throws IOException {
        final Collection<DoubleVectorFixImporter> availableImporters = new LinkedHashSet<>();
        availableImporters.addAll(getOSGiRegisteredImporters());
        for (Pair<String, FileItem> file : importerNamesAndFiles) {
            final String requestedImporterName = file.getA();
            final FileItem fi = file.getB();
            DoubleVectorFixImporter importerToUse = null;
            for (DoubleVectorFixImporter candidate : availableImporters) {
                if (candidate.getType().equals(requestedImporterName)) {
                    importerToUse = candidate;
                    break;
                }
            }
            if (importerToUse == null) {
                throw new RuntimeException("Sensor importer not found: " + requestedImporterName);
            }
            logger.log(Level.INFO,
                    "Going to import sensor data file  with importer " + importerToUse.getClass().getSimpleName());
            
            HashSet<TrackFileImportDeviceIdentifier> deviceIds = new HashSet<>();
            try (BufferedInputStream in = new BufferedInputStream(fi.getInputStream())) {
                final String filename = fi.getName();
                try {
                    final Charset charset = FileItemHelper.getCharset(fi);
                    importerToUse.importFixes(in, charset, new DoubleVectorFixImporter.Callback() {
                        @Override
                        public void addFixes(Iterable<DoubleVectorFix> fixes, TrackFileImportDeviceIdentifier device) {
                            storeFixes(fixes, device);
                            deviceIds.add(device);
                        }
                    }, filename, requestedImporterName, enableDownsampler);
                    logger.log(Level.INFO, "Successfully imported file " + requestedImporterName);
                } catch (FormatNotSupportedException e) {
                    result.add(requestedImporterName, filename, e);
                }
            }
            for (TrackFileImportDeviceIdentifier device : deviceIds) {
                TimeRange range = service.getSensorFixStore().getTimeRangeCoveredByFixes(device);
                long amount = service.getSensorFixStore().getNumberOfFixes(device);
                result.addTrackData(new TrackImportDTO(device.getId(), range, amount));
            }
        }
    }

    private void storeFixes(Iterable<DoubleVectorFix> fixes, DeviceIdentifier deviceIdentifier) {
        try {
            service.getSensorFixStore().storeFixes(deviceIdentifier, fixes, /* returnManeuverUpdate */ false, /* returnLiveDelay */ false);
        } catch (NoCorrespondingServiceRegisteredException e) {
            logger.log(Level.WARNING, "Could not store fix for " + deviceIdentifier);
        }
    }

    /**
     * Finds all {@link DoubleVectorFixImporter} service references in the OSGi context.
     * 
     * @return
     */
    private Collection<DoubleVectorFixImporter> getOSGiRegisteredImporters() {
        List<DoubleVectorFixImporter> result = new ArrayList<>();
        Collection<ServiceReference<DoubleVectorFixImporter>> refs;
        try {
            refs = context.getServiceReferences(DoubleVectorFixImporter.class, null);
            for (ServiceReference<DoubleVectorFixImporter> ref : refs) {
                result.add(context.getService(ref));
            }
        } catch (InvalidSyntaxException e) {
            logger.log(Level.WARNING, "Could not create OSGi filter");
        }
        return result;
    }
}
