package com.sap.sse.landscape;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.common.Named;
import com.sap.sse.common.TimePoint;

/**
 * Obtain from a {@link ReleaseRepository}. A release has a name that is composed of a base name and a time stamp.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface Release extends UserDataProvider, Named {
    Logger logger = Logger.getLogger(Release.class.getName());

    String RELEASE_NOTES_FILE_NAME = "release-notes.txt";
    String ARCHIVE_EXTENSION = ".tar.gz";
    
    default String getBaseName() {
        return getName().substring(0, getName().lastIndexOf("-"));
    }

    default TimePoint getCreationDate() {
        final String dateSubstring = getName().substring(getName().lastIndexOf("-")+1);
        try {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmm");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return TimePoint.of(simpleDateFormat.parse(dateSubstring));
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Error parsing release date "+dateSubstring+". Returning null instead.", e);
            return null;
        }
    }

    URL getReleaseNotesURL();
    
    URL getDeployableArchiveURL();
    
    @Override
    default Map<ProcessConfigurationVariable, String> getUserData() {
        final Map<ProcessConfigurationVariable, String> result = new HashMap<>();
        result.put(DefaultProcessConfigurationVariables.INSTALL_FROM_RELEASE, getName());
        return result;
    }
}
