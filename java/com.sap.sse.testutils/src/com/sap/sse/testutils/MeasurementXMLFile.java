package com.sap.sse.testutils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import com.sap.sse.common.Util;

/**
 * A utility for generating output files that contain an artificial test result with a &lt;system-out&gt; tag that has
 * &lt;measurement&gt; tags embedded which list named values for use by the Hudson/Jenkins Measurements Plot plug-in.
 * The output is written to the <code>bin/surefire-reports/</code> directory by the {@link #write()} method, but only if
 * a <code>bin/</code> directory exists as subdirectory of the current working directory. The <code>surefire-reports</code>
 * subdirectory will be created if it doesn't exist.<p>
 * 
 * Use this class as in the following example:
 * <pre>
 *       MeasurementXMLFile performanceReport = new MeasurementXMLFile(this.getClass());
 *       MeasurementCase performanceReportCase = performanceReport.addCase(getClass().getSimpleName());
 *       performanceReportCase.addMeasurement(new Measurement("This is a measurement name", 12345)); // 12345 is the value to be recorded
 *       performanceReport.write();
 * </pre>
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class MeasurementXMLFile {
    
    private final String reportFileName;
    private final String testSuiteName;
    private final Set<MeasurementCase> cases;
    private final String testClassName;
    
    public MeasurementXMLFile(Class<?> testClass) {
        this("TEST-"+testClass.getSimpleName()+".xml", testClass.getSimpleName(), testClass.getName());
    }
    
    public MeasurementXMLFile(String reportFileName, String testSuiteName, String testClassName) {
        this.reportFileName = reportFileName;
        this.testSuiteName = testSuiteName;
        this.testClassName = testClassName;
        cases = new HashSet<>();
    }
    
    public MeasurementCase addCase(String name) {
        MeasurementCase result = new MeasurementCase(name);
        cases.add(result);
        return result;
    }
    
    private Iterable<MeasurementCase> getCases() {
        return cases;
    }
    
    public void write() throws IOException {
        File binDir = new File("./bin");
        if (binDir.exists() && binDir.isDirectory()) {
            File surefire_reports = new File(binDir, "surefire-reports");
            if (!surefire_reports.exists()) {
                surefire_reports.mkdir();
            }
            Writer w = new BufferedWriter(new FileWriter(new File(surefire_reports, reportFileName)));
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            w.write("<testsuite name=\"" + getSuiteName() + "\" tests=\"" + getNumberOfTests() + "\" >\n");
            for (MeasurementCase measurementCase : getCases()) {
                w.write("<testcase name=\"" + measurementCase.getName() + "\" status=\"run\" time=\"0\" classname=\""
                        + getTestClassName() + "\">\n");
                w.write("<system-out>\n");
                for (Measurement measurement : measurementCase.getMeasurements()) {
                    w.write("&lt;measurement&gt;&lt;name&gt;" + measurement.getName() + "&lt;/name&gt;&lt;value&gt;"
                            + measurement.getValue() + "&lt;/value&gt;&lt;/measurement&gt;");
                }
                w.write("</system-out>\n");
                w.write("</testcase>\n");
            }
            w.write("</testsuite>\n");
            w.close();
        }
    }

    private int getNumberOfTests() {
        return Util.size(getCases());
    }

    private String getTestClassName() {
        return testClassName;
    }

    private String getSuiteName() {
        return testSuiteName;
    }
}
