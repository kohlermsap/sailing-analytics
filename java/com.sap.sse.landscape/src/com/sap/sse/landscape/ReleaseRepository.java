package com.sap.sse.landscape;

/**
 * Contains ready-built releases that can be used as part of the {@code INSTALL_FROM_RELEASE} directive. Iterating over
 * this iterable's elements lists the {@link Release}s available.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface ReleaseRepository extends Iterable<Release> {
    /**
     * @return the latest build with name prefix {@link #MASTER_RELEASE_NAME_PREFIX} if such a release exists in this
     *         repository, or {@code null} otherwise
     */
    default Release getLatestDefaultRelease() {
        return getLatestRelease(getDefaultReleaseNamePrefix());
    }
    
    /**
     * Returns the latest release with the prefix specified by the parameter.
     */
    Release getLatestRelease(String releaseNamePrefix);
    
    /**
     * @return the {@link Release} with name {@code releaseName} if it exists, or {@code null} otherwise
     */
    Release getRelease(String releaseName);

    String getDefaultReleaseNamePrefix();
}
