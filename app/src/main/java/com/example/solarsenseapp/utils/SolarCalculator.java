package com.example.solarsenseapp.utils;

import java.util.Calendar;
import java.util.Date;

public class SolarCalculator {
    private static final String TAG = "SolarCalculator";

    public static class SolarPosition {
        public final double azimuth;
        public final double altitude;

        public SolarPosition(double azimuth, double altitude) {
            this.azimuth = azimuth;
            this.altitude = altitude;
        }

        @Override
        public String toString() {
            return String.format("SolarPosition{azimuth=%.2f°, altitude=%.2f°}", azimuth, altitude);
        }
    }

    /**
     * Calculate solar position (azimuth and altitude) for given location and time
     * @param latitude Location latitude in degrees
     * @param longitude Location longitude in degrees
     * @param date Date and time for calculation
     * @return SolarPosition containing azimuth (0-360°) and altitude (-90 to 90°)
     */
    public static SolarPosition calculateSolarPosition(double latitude, double longitude, Date date) {
        double latRad = Math.toRadians(latitude);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        double hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0;

        // Solar declination (δ)
        double declination = 23.45 * Math.sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 81)));
        double declRad = Math.toRadians(declination);

        // Equation of time and time correction
        double B = Math.toRadians(360.0 / 365.0 * (dayOfYear - 81));
        double equationOfTime = 9.87 * Math.sin(2 * B) - 7.53 * Math.cos(B) - 1.5 * Math.sin(B);
        double timeCorrection = equationOfTime + 4 * (longitude - cal.getTimeZone().getRawOffset() / 3600000.0 * 15);
        double solarTime = hour + timeCorrection / 60.0;

        // Hour angle (ω)
        double hourAngle = Math.toRadians(15 * (solarTime - 12));

        // Solar azimuth (φ) - MODIFIED TO MATCH REAL-WORLD ORIENTATION
        double azimuth = Math.atan2(
                Math.sin(hourAngle),
                Math.cos(hourAngle) * Math.sin(latRad) - Math.tan(declRad) * Math.cos(latRad)
        );

        // Convert to degrees and normalize (0-360)
        azimuth = Math.toDegrees(azimuth);
        azimuth = (azimuth + 360) % 360;  // Ensure positive value

        // INVERT DIRECTION if servos rotate backwards
        azimuth = 360 - azimuth;  // Reverse rotation direction

        // Solar altitude (α)
        double altitude = Math.toDegrees(Math.asin(
                Math.sin(latRad) * Math.sin(declRad) +
                        Math.cos(latRad) * Math.cos(declRad) * Math.cos(hourAngle)
        ));

        return new SolarPosition(azimuth, altitude);
    }

    /**
     * Convert solar position to servo angles
     * @param solarPosition Solar position from calculateSolarPosition
     * @return int array [baseAngle, panelAngle] suitable for servo control
     */
    public static int[] solarPositionToServoAngles(SolarPosition solarPosition) {
        // Convert azimuth from 360° to 180° range for base servo
        int baseAngle = (int) Math.round(solarPosition.azimuth / 2);

        // Convert altitude from 90° to 180° range for panel servo
        int panelAngle = (int) Math.round(solarPosition.altitude * 2);

        // Clamp to valid servo range
        baseAngle = Math.max(Constants.SERVO_MIN_ANGLE, Math.min(Constants.SERVO_MAX_ANGLE, baseAngle));
        panelAngle = Math.max(Constants.SERVO_MIN_ANGLE, Math.min(Constants.SERVO_MAX_ANGLE, panelAngle));

        return new int[]{baseAngle, panelAngle};
    }

    /**
     * Check if the sun is above horizon (daylight)
     * @param solarPosition Solar position to check
     * @return true if sun is above horizon
     */
    public static boolean isDaylight(SolarPosition solarPosition) {
        return solarPosition.altitude > 0;
    }

    /**
     * Get a descriptive string for current sun position
     * @param solarPosition Solar position to describe
     * @return Human readable description
     */
    public static String getSunPositionDescription(SolarPosition solarPosition) {
        if (solarPosition.altitude < 0) {
            return "Sun is below horizon (nighttime)";
        }

        String direction;
        double azimuth = solarPosition.azimuth;

        if (azimuth >= 337.5 || azimuth < 22.5) {
            direction = "North";
        } else if (azimuth >= 22.5 && azimuth < 67.5) {
            direction = "Northeast";
        } else if (azimuth >= 67.5 && azimuth < 112.5) {
            direction = "East";
        } else if (azimuth >= 112.5 && azimuth < 157.5) {
            direction = "Southeast";
        } else if (azimuth >= 157.5 && azimuth < 202.5) {
            direction = "South";
        } else if (azimuth >= 202.5 && azimuth < 247.5) {
            direction = "Southwest";
        } else if (azimuth >= 247.5 && azimuth < 292.5) {
            direction = "West";
        } else {
            direction = "Northwest";
        }

        return String.format("Sun is %.1f° above horizon in the %s", solarPosition.altitude, direction);
    }
}