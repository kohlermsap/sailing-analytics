package com.sap.sse.landscape.impl;

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
    
    @Override
    public Release getLatestRelease(String releaseNamePrefix) {
        Release result = null;
        for (final Release release : this) { // invokes the iterator() method
            if (release.getBaseName().equals(releaseNamePrefix) &&
                    (result == null || release.getCreationDate().after(result.getCreationDate()))) {
                result = release;
            }
        }
        return result;
    }

    @Override
    public Release getRelease(String releaseName) {
        return Util.first(Util.filter(this, r->r.getName().equals(releaseName)));
    }
}
