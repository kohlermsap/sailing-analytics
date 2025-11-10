package com.sap.sse.landscape.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sap.sse.landscape.Release;
import com.sap.sse.util.HttpUrlConnectionHelper;

/**
 * Assumes a simple folder exposed by a web server, such as Apache httpd, where the "repository base" references the
 * folder which shows an "index" of the sub-folders in it, so that we can explore it. Each sub-folder is expected to be
 * named after the corresponding release and must contain the release-notes.txt file (see
 * {@link Release#RELEASE_NOTES_FILE_NAME}) as well as the .tar.gz file (see {@link Release#ARCHIVE_EXTENSION}) that
 * represents the actual release file.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class FolderBasedReleaseRepositoryImpl extends AbstractReleaseRepository {
    private static final Logger logger = Logger.getLogger(FolderBasedReleaseRepositoryImpl.class.getName());
    private final String repositoryBase;

    public FolderBasedReleaseRepositoryImpl(String repositoryBase, String defaultReleaseNamePrefix) {
        super(defaultReleaseNamePrefix);
        this.repositoryBase = repositoryBase;
    }

    private String getRepositoryBase() {
        return repositoryBase;
    }

    @Override
    public Iterator<Release> iterator() {
        return getAvailableReleases().iterator();
    }

    private Iterable<Release> getAvailableReleases() {
        final List<Release> result = new LinkedList<>();
        try {
            final URLConnection connection = HttpUrlConnectionHelper.redirectConnection(new URL(getRepositoryBase()));
            final InputStream index = (InputStream) connection.getContent();
            int read = 0;
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while ((read=index.read()) != -1) {
                bos.write(read);
            }
            index.close();
            final String contents = bos.toString();
            final Pattern pattern = Pattern.compile("<a href=\"(([^/]*)-([0-9]*))/\">([^/]*)-([0-9]*)/</a>");
            final Matcher m = pattern.matcher(contents);
            int lastMatch = 0;
            while (m.find(lastMatch)) {
                result.add(new FolderBasedReleaseImpl(m.group(1), getRepositoryBase()));
                lastMatch = m.end();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading releases from repository at "+getRepositoryBase()+". Continuing empty.", e);
            return Collections.emptyList();
        }
        return result;
    }
}
