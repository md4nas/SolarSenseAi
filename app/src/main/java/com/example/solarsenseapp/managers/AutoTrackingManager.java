package com.example.solarsenseapp.managers;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import com.example.solarsenseapp.MainActivity;
import com.example.solarsenseapp.controllers.ServoController;
import com.example.solarsenseapp.utils.Constants;
import com.example.solarsenseapp.utils.SolarCalculator;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class AutoTrackingManager {
    private static final String TAG = "AutoTrackingManager";

    private final MainActivity activity;
    private final ServoController servoController;
    private final Handler autoHandler;
    private Runnable autoRunnable;

    private boolean isAutoTrackingActive = false;

    public AutoTrackingManager(MainActivity activity, ServoController servoController) {
        this.activity = activity;
        this.servoController = servoController;
        this.autoHandler = new Handler();

        setupAutoRunnable();
    }

    private void setupAutoRunnable() {
        autoRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAutoTrackingActive) {
                    Location currentLocation = activity.getCurrentLocation();
                    if (currentLocation != null) {
                        updatePanelPosition(currentLocation);
                    }
                    autoHandler.postDelayed(this, Constants.AUTO_UPDATE_INTERVAL);
                }
            }
        };
    }

    public void startAutoMode(Location currentLocation, String locationText) {
        if (currentLocation != null) {
            startTrackingWithLocation(currentLocation);
            Log.d(TAG, "Auto tracking started with GPS location");
        } else if (!locationText.isEmpty()) {
            startTrackingWithGeocoding(locationText);
        } else {
            activity.showToast("Please enable GPS or enter location");
            // Reset toggle if no location available
            activity.runOnUiThread(() -> {
                // This would require access to the toggle, might need a callback
            });
        }
    }

    private void startTrackingWithLocation(Location location) {
        isAutoTrackingActive = true;
        updatePanelPosition(location);
        autoHandler.postDelayed(autoRunnable, Constants.AUTO_UPDATE_INTERVAL);
        activity.speakFeedback("Auto tracking started");
    }

    private void startTrackingWithGeocoding(String locationText) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(activity);
                List<Address> addresses = geocoder.getFromLocationName(locationText, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Location location = new Location("");
                    location.setLatitude(addresses.get(0).getLatitude());
                    location.setLongitude(addresses.get(0).getLongitude());

                    activity.setCurrentLocation(location);

                    activity.runOnUiThread(() -> {
                        startTrackingWithLocation(location);
                    });
                    Log.d(TAG, "Auto tracking started with geocoded location");
                } else {
                    activity.showToast("Could not find location");
                    stopAutoMode();
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoding error", e);
                activity.showToast("Geocoding error");
                stopAutoMode();
            }
        }).start();
    }

    public void stopAutoMode() {
        isAutoTrackingActive = false;
        autoHandler.removeCallbacks(autoRunnable);
        activity.speakFeedback("Auto tracking stopped");
        Log.d(TAG, "Auto tracking stopped");
    }

    public void updatePanelPosition(Location location) {
        if (location == null) {
            Log.w(TAG, "Cannot update panel position: location is null");
            return;
        }

        // Calculate solar position
        SolarCalculator.SolarPosition solarPosition = SolarCalculator.calculateSolarPosition(
                location.getLatitude(),
                location.getLongitude(),
                new Date()
        );

        // Convert to servo angles
        int[] servoAngles = SolarCalculator.solarPositionToServoAngles(solarPosition);
        int baseAngle = servoAngles[0];
        int panelAngle = servoAngles[1];

        // Update servos
        servoController.setBaseServoAuto(baseAngle);
        servoController.setPanelServoAuto(panelAngle);

        // Log the update
        Log.d(TAG, String.format("Panel position updated - Base: %d°, Panel: %d° (Solar: %.1f° azimuth, %.1f° altitude)",
                baseAngle, panelAngle, solarPosition.azimuth, solarPosition.altitude));

        // Check if it's nighttime
        if (!SolarCalculator.isDaylight(solarPosition)) {
            Log.i(TAG, "Sun is below horizon - positioning for next sunrise");
        }
    }

    public boolean isAutoTrackingActive() {
        return isAutoTrackingActive;
    }

    public void cleanup() {
        stopAutoMode();
    }
}