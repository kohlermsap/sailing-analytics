package com.sap.sse.landscape.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.landscape.Release;

public class TestFolderBasedReleaseRepository {
    private static final String MAIN = "main";
    private static FolderBasedReleaseRepositoryImpl repository;
    
    @BeforeAll
    public static void setUp() {
        repository = new FolderBasedReleaseRepositoryImpl("https://releases.sapsailing.com", MAIN);
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
}
