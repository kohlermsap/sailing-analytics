package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import com.sap.sailing.declination.impl.NOAAImporter;

@Disabled("US Government Shutdown around 2026-02-04")
public class NOAADeclinationServiceTest extends DeclinationServiceTest<NOAAImporter> {
    @Override
    @BeforeEach
    public void setUp() {
        importer = new NOAAImporter();
        super.setUp();
    }
}
