package com.sap.sse.landscape.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.landscape.Release;

public class TestGithubReleaseRepository {
    private static final String DOCKER_25 = "docker-25";
    private static final String MAIN = "main";
    private static GithubReleasesRepository repository;
    
    @BeforeAll
    public static void setUp() {
        repository = new GithubReleasesRepository("SAP", "sailing-analytics", MAIN);
    }
    
    @Test
    public void testAtLeastOneRelease() {
        assertFalse(Util.isEmpty(repository));
    }
    
    @Test
    public void testAtLeastOneMainRelease() {
        final Release latestMasterRelease = repository.getLatestDefaultRelease();
        assertNotNull(latestMasterRelease);
        assertEquals(MAIN, latestMasterRelease.getBaseName());
    }

    @Test
    public void testAtLeastOneDocker25Release() {
        final Release latestDocker25Release = repository.getLatestRelease(DOCKER_25);
        assertNotNull(latestDocker25Release);
        assertEquals(DOCKER_25, latestDocker25Release.getBaseName());
    }
    
    @Test
    public void testLinkToNextPageFromFirst() {
        assertEquals("https://api.github.com/repositories/790295432/releases?page=2",
                repository.getNextPageURL("<https://api.github.com/repositories/790295432/releases?page=2>; rel=\"next\", <https://api.github.com/repositories/790295432/releases?page=27>; rel=\"last\""));
    }

    @Test
    public void testLinkToNextPageFromMiddleWith100PerPage() {
        assertEquals("https://api.github.com/repositories/790295432/releases?per_page=100&page=5",
                repository.getNextPageURL("<https://api.github.com/repositories/790295432/releases?per_page=100&page=3>; rel=\"prev\", <https://api.github.com/repositories/790295432/releases?per_page=100&page=5>; rel=\"next\", <https://api.github.com/repositories/790295432/releases?per_page=100&page=8>; rel=\"last\", <https://api.github.com/repositories/790295432/releases?per_page=100&page=1>; rel=\"first\""));
    }
    
    @Test
    public void testLinkToNextPageFromLaste() {
        assertNull(
                repository.getNextPageURL("<https://api.github.com/repositories/790295432/releases?per_page=100&page=7>; rel=\"prev\", <https://api.github.com/repositories/790295432/releases?per_page=100&page=1>; rel=\"first\""));
    }
}
