package com.sap.sse.testutils;

/**
 * Create an instance of this class and {@link MeasurementCase#addMeasurement(Measurement) add it} to a
 * measurement case. The final reporting is then done by invoking {@link MeasurementXMLFile#write()} on
 * the measurement file created before.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class Measurement {
    private final String name;
    private final double value;

    public Measurement(String name, double value) {
        super();
        this.name = name;
        this.value = value;
    }

    String getName() {
        return name;
    }

    double getValue() {
        return value;
    }

}
