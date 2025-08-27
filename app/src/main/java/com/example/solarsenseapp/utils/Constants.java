package com.example.solarsenseapp.utils;

public class Constants {
    // API Keys
    public static final String WEATHER_API_KEY = "4caa58a2c8523065469f2b1e7bd4c050";

    // Network
    public static final String DEFAULT_ESP_IP = "http://192.168.63.219";

    // Permission Request Codes
    public static final int LOCATION_PERMISSION_REQUEST = 100;
    public static final int RECORD_AUDIO_PERMISSION_REQUEST = 101;

    // Auto Mode Settings
    public static final int AUTO_UPDATE_INTERVAL = 300000; // 5 minutes in milliseconds

    // Servo Settings
    public static final int SERVO_MIN_ANGLE = 0;
    public static final int SERVO_MAX_ANGLE = 180;
    public static final int DEFAULT_BASE_ANGLE = 90;
    public static final int DEFAULT_PANEL_ANGLE = 90;
    public static final int SERVO_STEP_SIZE = 40;

    // Location Settings
    public static final long LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds
    public static final float LOCATION_MIN_DISTANCE = 10; // 10 meters

    // Network Timeouts
    public static final int CONNECTION_TIMEOUT = 5000;
    public static final int READ_TIMEOUT = 5000;

    // ESP Endpoints
    public static final String BASE_SERVO_ENDPOINT = "/baseServo?angle=";
    public static final String PANEL_SERVO_ENDPOINT = "/panelServo?angle=";

    // Weather API
    public static final String WEATHER_API_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    // Voice Recognition
    public static final String VOICE_COMMANDS_HELP =
            "Supported Voice Commands:\n\n" +
                    "Base Rotation:\n" +
                    "- 'Clockwise' (rotate base clockwise)\n" +
                    "- 'Counter-clockwise'/'Anti-clockwise' (rotate base counter-clockwise)\n\n" +
                    "Panel Tilt:\n" +
                    "- 'Up'/'Panel up' (tilt panel up)\n" +
                    "- 'Down'/'Panel down' (tilt panel down)\n\n" +
                    "Panel Position:\n" +
                    "- 'Panel zero'/'Panel Angle zero' (set panel to 0°)\n" +
                    "- 'Panel max'/'Panel Angle max' (set panel to 180°)\n\n" +
                    "Base Position:\n" +
                    "- 'Base zero'/'Base Position zero' (set base to 0°)\n" +
                    "- 'Base max'/'Base Position max' (set base to 180°)\n\n" +
                    "Auto Mode:\n" +
                    "- 'Auto mode on'/'Start tracking' (enable auto tracking)\n" +
                    "- 'Auto mode off'/'Stop tracking' (disable auto tracking)\n\n" +
                    "Weather:\n" +
                    "- 'Weather for [location]' (fetch weather data)\n\n" +
                    "Location:\n" +
                    "- 'Reset location'/'Clear location'/'Location reset' (clear current location)";
}