package com.sap.sailing.declination.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import com.sap.sailing.declination.impl.NOAAImporterForTesting;

@Disabled("US Government Shutdown around 2026-02-04")
public class NOAADeclinationImportTest extends DeclinationImportTest<NOAAImporterForTesting> {
    @BeforeEach
    public void setUp() {
        importer = new NOAAImporterForTesting();
    }
}
