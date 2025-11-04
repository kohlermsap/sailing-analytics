package com.sap.sailing.declination.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/*      License Statement from the NOAA
* The WMM source code is in the public domain and not licensed or
* under copyright. The information and software may be used freely
* by the public. As required by 17 U.S.C. 403, third parties producing
* copyrighted works consisting predominantly of the material produced
* by U.S. government agencies must provide notice with such work(s)
* identifying the U.S. Government material incorporated and stating
* that such material is not subject to copyright protection.*/

import java.util.GregorianCalendar;

import com.sap.sse.common.Duration;
import com.sap.sse.common.Named;
import com.sap.sse.common.TimePoint;

/**
 * <p>
 * Class to calculate magnetic declination, magnetic field strength, inclination etc. for any point on the earth.
 * </p>
 * <p>
 * Adapted from the geomagc software and World Magnetic Model of the NOAA Satellite and Information Service, National
 * Geophysical Data Center. Caching removed to make stateless, except for the reading of the coefficients file.
 * Results are now returned as instances of the inner class {@link Result}.
 * </p>
 * http://www.ngdc.noaa.gov/geomag/WMM/DoDWMM.shtml
 * <p>
 * © Deep Pradhan, 2017
 * </p>
 */
class Geomagnetism implements Named {
    private static final long serialVersionUID = -2814152697634383730L;

    class Result {
        /** Geomagnetic declination (decimal degrees) [opposite of variation, positive Eastward/negative Westward] */
        private final double declination;

        /** Geomagnetic inclination/dip angle (degrees) [positive downward] */
        private final double inclination;

        /** Geomagnetic field intensity/strength (nano Teslas) */
        private final double intensity;

        /** Geomagnetic horizontal field intensity/strength (nano Teslas) */
        private final double bh;

        /** Geomagnetic vertical field intensity/strength (nano Teslas) [positive downward] */
        private final double bz;

        /** Geomagnetic North South (northerly component) field intensity/strength (nano Tesla) */
        private final double bx;

        /** Geomagnetic East West (easterly component) field intensity/strength (nano Teslas) */
        private final double by;

        public Result(double declination, double inclination, double intensity, double bh, double bz, double bx, double by) {
            this.declination = declination;
            this.inclination = inclination;
            this.intensity = intensity;
            this.bh = bh;
            this.bz = bz;
            this.bx = bx;
            this.by = by;
        }

        /** @return Geomagnetic declination (degrees) [opposite of variation, positive Eastward/negative Westward] */
        double getDeclination() {
            return declination;
        }

        /** @return Geomagnetic inclination/dip angle (degrees) [positive downward] */
        double getInclination() {
            return inclination;
        }

        /** @return Geomagnetic field intensity/strength (nano Teslas) */
        double getIntensity() {
            return intensity;
        }

        /** @return Geomagnetic horizontal field intensity/strength (nano Teslas) */
        double getHorizontalIntensity() {
            return bh;
        }

        /** @return Geomagnetic vertical field intensity/strength (nano Teslas) [positive downward] */
        double getVerticalIntensity() {
            return bz;
        }

        /** @return Geomagnetic North South (northerly component) field intensity/strength (nano Tesla) */
        double getNorthIntensity() {
            return bx;
        }

        /** @return Geomagnetic East West (easterly component) field intensity/strength (nano Teslas) */
        double getEastIntensity() {
            return by;
        }
        
        String getModelName() {
            return getName();
        }
        
        TimePoint getModelIssueTimePoint() {
            return getIssueTimePoint();
        }
    }
    
    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");

    private final TimePoint startOfValidity;
    
    /**
     * Initialise the instance without calculations
     * 
     * @param cofReader
     *            a reader for a file in .COF format, such as WMM2025.COF; expects to find trailing lines with at least
     *            "999999" in them
     */
    Geomagnetism(BufferedReader cofReader) throws IOException, ParseException {
        // Initialize constants
        maxord = MAX_DEG;
        sp[0] = 0;
        cp[0] = snorm[0] = pp[0] = 1;
        dp[0][0] = 0;
        c[0][0] = 0;
        cd[0][0] = 0;
        final String headerLine = cofReader.readLine();
        final String[] headerFields = headerLine.trim().split("\\s+");
        epoch = Double.parseDouble(headerFields[0]);
        startOfValidity = TimePoint.of(new GregorianCalendar((int) epoch, 1, 1, 0, 0, 0).getTimeInMillis()).plus(Duration.ONE_HOUR.times(365.0*24.0*(epoch-((int) epoch))));
        name = headerFields[1];
        issueTimePoint = TimePoint.of(simpleDateFormat.parse(headerFields[2]));
        String[] tokens;
        double gnm, hnm, dgnm, dhnm;
        String line;
        while (!(line=cofReader.readLine()).startsWith("999999")) {
            tokens = line.trim().split("\\s+");
            final int n = Integer.parseInt(tokens[0]);
            final int m = Integer.parseInt(tokens[1]);
            gnm = Double.parseDouble(tokens[2]);
            hnm = Double.parseDouble(tokens[3]);
            dgnm = Double.parseDouble(tokens[4]);
            dhnm = Double.parseDouble(tokens[5]);
            if (m <= n) {
                c[m][n] = gnm;
                cd[m][n] = dgnm;
                if (m != 0) {
                    c[n][m - 1] = hnm;
                    cd[n][m - 1] = dhnm;
                }
            }
        }
        // Convert schmidt normalized gauss coefficients to unnormalized
        snorm[0] = 1;
        double flnmj;
        for (int j, n = 1; n <= maxord; n++) {
            snorm[n] = snorm[n - 1] * (2 * n - 1) / n;
            j = 2;
            for (int m = 0, d1 = 1, d2 = (n - m + d1) / d1; d2 > 0; d2--, m += d1) {
                k[m][n] = (double) (((n - 1) * (n - 1)) - (m * m)) / (double) ((2 * n - 1) * (2 * n - 3));
                if (m > 0) {
                    flnmj = ((n - m + 1) * j) / (double) (n + m);
                    snorm[n + m * 13] = snorm[n + (m - 1) * 13] * Math.sqrt(flnmj);
                    j = 1;
                    c[n][m - 1] = snorm[n + m * 13] * c[n][m - 1];
                    cd[n][m - 1] = snorm[n + m * 13] * cd[n][m - 1];
                }
                c[m][n] = snorm[n + m * 13] * c[m][n];
                cd[m][n] = snorm[n + m * 13] * cd[m][n];
            }
            fn[n] = (n + 1);
            fm[n] = n;
        }
        k[1][1] = 0;
        fm[0] = 0;
    }

    /**
     * Calculate for given location, altitude and date
     * 
     * @param longitude
     *            Longitude in decimal degrees
     * @param latitude
     *            Latitude in decimal degrees
     * @param altitude
     *            Altitude in metres (with respect to WGS-1984 ellipsoid)
     * @param calendar
     *            Calendar for date of calculation
     */
    Result calculate(double longitude, double latitude, double altitude, GregorianCalendar calendar) {
        double rlon = Math.toRadians(longitude), rlat = Math.toRadians(latitude),
                altitudeKm = Double.isNaN(altitude) ? 0 : altitude / 1000,
                yearFraction = calendar.get(GregorianCalendar.YEAR)
                        + (double) calendar.get(GregorianCalendar.DAY_OF_YEAR)
                                / calendar.getActualMaximum(GregorianCalendar.DAY_OF_YEAR),
                dt = yearFraction - epoch, srlon = Math.sin(rlon), srlat = Math.sin(rlat), crlon = Math.cos(rlon),
                crlat = Math.cos(rlat), srlat2 = srlat * srlat, crlat2 = crlat * crlat, a2 = WGS84_A * WGS84_A,
                b2 = WGS84_B * WGS84_B, c2 = a2 - b2, a4 = a2 * a2, b4 = b2 * b2, c4 = a4 - b4;
        sp[1] = srlon;
        cp[1] = crlon;
        // Convert from geodetic coords. to spherical coords.
        double q = Math.sqrt(a2 - c2 * srlat2), q1 = altitudeKm * q,
                q2 = ((q1 + a2) / (q1 + b2)) * ((q1 + a2) / (q1 + b2)),
                r2 = ((altitudeKm * altitudeKm) + 2 * q1 + (a4 - c4 * srlat2) / (q * q));
        ct = srlat / Math.sqrt(q2 * crlat2 + srlat2);
        st = Math.sqrt(1 - (ct * ct));
        r = Math.sqrt(r2);
        d = Math.sqrt(a2 * crlat2 + b2 * srlat2);
        ca = (altitudeKm + d) / r;
        sa = c2 * crlat * srlat / (r * d);
        for (int m = 2; m <= maxord; m++) {
            sp[m] = sp[1] * cp[m - 1] + cp[1] * sp[m - 1];
            cp[m] = cp[1] * cp[m - 1] - sp[1] * sp[m - 1];
        }
        double aor = IAU66_RADIUS / r, ar = aor * aor, br = 0, bt = 0, bp = 0, bpp = 0, par, parp, temp1, temp2;
        for (int n = 1; n <= maxord; n++) {
            ar = ar * aor;
            for (int m = 0, d3 = 1, d4 = (n + m + d3) / d3; d4 > 0; d4--, m += d3) {
                // Compute unnormalized associated legendre polynomials and derivatives via recursion relations
                if (n == m) {
                    snorm[n + m * 13] = st * snorm[n - 1 + (m - 1) * 13];
                    dp[m][n] = st * dp[m - 1][n - 1] + ct * snorm[n - 1 + (m - 1) * 13];
                }
                if (n == 1 && m == 0) {
                    snorm[n + m * 13] = ct * snorm[n - 1 + m * 13];
                    dp[m][n] = ct * dp[m][n - 1] - st * snorm[n - 1 + m * 13];
                }
                if (n > 1 && n != m) {
                    if (m > n - 2) {
                        snorm[n - 2 + m * 13] = 0;
                    }
                    if (m > n - 2) {
                        dp[m][n - 2] = 0;
                    }
                    snorm[n + m * 13] = ct * snorm[n - 1 + m * 13] - k[m][n] * snorm[n - 2 + m * 13];
                    dp[m][n] = ct * dp[m][n - 1] - st * snorm[n - 1 + m * 13] - k[m][n] * dp[m][n - 2];
                }
                // Time adjust the gauss coefficients
                tc[m][n] = c[m][n] + dt * cd[m][n];
                if (m != 0) {
                    tc[n][m - 1] = c[n][m - 1] + dt * cd[n][m - 1];
                }
                // Accumulate terms of the spherical harmonic expansions
                par = ar * snorm[n + m * 13];
                if (m == 0) {
                    temp1 = tc[m][n] * cp[m];
                    temp2 = tc[m][n] * sp[m];
                } else {
                    temp1 = tc[m][n] * cp[m] + tc[n][m - 1] * sp[m];
                    temp2 = tc[m][n] * sp[m] - tc[n][m - 1] * cp[m];
                }
                bt = bt - ar * temp1 * dp[m][n];
                bp += (fm[m] * temp2 * par);
                br += (fn[n] * temp1 * par);
                // Special case: north/south geographic poles
                if (st == 0 && m == 1) {
                    if (n == 1) {
                        pp[n] = pp[n - 1];
                    } else {
                        pp[n] = ct * pp[n - 1] - k[m][n] * pp[n - 2];
                    }
                    parp = ar * pp[n];
                    bpp += (fm[m] * temp2 * parp);
                }
            }
        }
        if (st == 0) {
            bp = bpp;
        } else {
            bp /= st;
        }
        // Rotate magnetic vector components from spherical to geodetic coordinates
        // bx must be the east-west field component
        // by must be the north-south field component
        // bz must be the vertical field component.
        final double bx = -bt * ca - br * sa;
        final double by = bp;
        final double bz = bt * sa - br * ca;
        // Compute declination (dec), inclination (dip) and total intensity (ti)
        final double bh = Math.sqrt((bx * bx) + (by * by));
        final double intensity = Math.sqrt((bh * bh) + (bz * bz));
        // Calculate the declination.
        final double declination = Math.toDegrees(Math.atan2(by, bx));
        final double inclination = Math.toDegrees(Math.atan2(bz, bh));
        return new Result(declination, inclination, intensity, bh, bz, bx, by);
    }

    /**
     * Calculate for given location, altitude and current date
     * 
     * @param longitude
     *            Longitude in decimal degrees
     * @param latitude
     *            Latitude in decimal degrees
     * @param altitude
     *            Altitude in metres (with respect to WGS-1984 ellipsoid)
     */
    Result calculate(double longitude, double latitude, double altitude) {
        return calculate(longitude, latitude, altitude, new GregorianCalendar());
    }

    /**
     * Calculate for given location, zero altitude and current date
     * 
     * @param longitude
     *            Longitude in decimal degrees
     * @param latitude
     *            Latitude in decimal degrees
     */
    Result calculate(double longitude, double latitude) {
        return calculate(longitude, latitude, 0);
    }
    
    /** @return The date in years, for the start of the valid time of the fit coefficients */
    public double getEpoch() {
        return epoch;
    }

    public TimePoint getStartOfValidity() {
        return startOfValidity;
    }

    public String getName() {
        return name;
    }

    public TimePoint getIssueTimePoint() {
        return issueTimePoint;
    }
    private final String name;

    private final TimePoint issueTimePoint;
    
    /** Mean radius of IAU-66 ellipsoid, in km */
    private static final double IAU66_RADIUS = 6371.2;

    /** Semi-major axis of WGS-1984 ellipsoid, in km */
    private static final double WGS84_A = 6378.137;

    /** Semi-minor axis of WGS-1984 ellipsoid, in km */
    private static final double WGS84_B = 6356.7523142;

    /** The maximum number of degrees of the spherical harmonic model */
    private static final int MAX_DEG = 12;

    /** The Gauss coefficients of main geomagnetic model (nt) */
    private double c[][] = new double[300][300];

    /** The Gauss coefficients of secular geomagnetic model (nt/yr) */
    private double cd[][] = new double[300][300];

    /** The time adjusted geomagnetic gauss coefficients (nt) */
    private double tc[][] = new double[13][13];

    /** The theta derivative of p(n,m) (unnormalized) */
    private double dp[][] = new double[13][13];

    /** The Schmidt normalization factors */
    private double snorm[] = new double[169];

    /** The sine of (m*spherical coordinate longitude) */
    private double sp[] = new double[13];

    /** The cosine of (m*spherical coordinate longitude) */
    private double cp[] = new double[13];
    private double fn[] = new double[13];
    private double fm[] = new double[13];

    /** The maximum order of spherical harmonic model */
    private int maxord;

    /** The associated Legendre polynomials for m = 1 (unnormalized) */
    private double pp[] = new double[13];

    private double k[][] = new double[13][13];

    /** The date in years, for the start of the valid time of the fit coefficients */
    private double epoch;

    private double r, d, ca, sa, ct, st;
}