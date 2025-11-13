package com.sap.sse.landscape.impl;

import java.net.MalformedURLException;
import java.net.URL;

import com.sap.sse.landscape.Release;

/**
 * Collaborates with {@link FolderBasedReleaseRepositoryImpl}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class FolderBasedReleaseImpl extends AbstractRelease implements Release {
    private static final long serialVersionUID = -225240683033821028L;
    private final String repositoryBase;

    public FolderBasedReleaseImpl(String name, String repositoryBase) {
        super(name);
        this.repositoryBase = repositoryBase;
    }

    @Override
    public URL getReleaseNotesURL() {
        try {
            return new URL(getDownloadFolderURL()+RELEASE_NOTES_FILE_NAME);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public URL getDeployableArchiveURL() {
        try {
            return new URL(getDownloadFolderURL()+getName()+ARCHIVE_EXTENSION);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private String getDownloadFolderURL() {
        return repositoryBase+"/"+getName()+"/";
    }
}
