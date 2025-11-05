package com.sap.sse.landscape.impl;

import java.util.Iterator;

import com.sap.sse.common.Util;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.ReleaseRepository;

public abstract class AbstractReleaseRepository implements ReleaseRepository {
    private final String defaultReleaseNamePrefix;
    
    public AbstractReleaseRepository(String defaultReleaseNamePrefix) {
        super();
        this.defaultReleaseNamePrefix = defaultReleaseNamePrefix;
    }

    @Override
    public String getDefaultReleaseNamePrefix() {
        return defaultReleaseNamePrefix;
    }
    
    protected abstract Iterable<Release> getAvailableReleases();

    @Override
    public Iterator<Release> iterator() {
        return getAvailableReleases().iterator();
    }

    @Override
    public Release getLatestRelease(String releaseNamePrefix) {
        Release result = null;
        for (final Release release : getAvailableReleases()) {
            if (release.getBaseName().equals(releaseNamePrefix) &&
                    (result == null || release.getCreationDate().after(result.getCreationDate()))) {
                result = release;
            }
        }
        return result;
    }

    @Override
    public Release getRelease(String releaseName) {
        return Util.first(Util.filter(getAvailableReleases(), r->r.getName().equals(releaseName)));
    }
}
