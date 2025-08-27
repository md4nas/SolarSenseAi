package com.example.solarsenseapp.managers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.solarsenseapp.MainActivity;
import com.example.solarsenseapp.utils.Constants;

public class LocationServiceManager {
    private static final String TAG = "LocationServiceManager";

    private final MainActivity activity;
    private final LocationListener locationListener;
    private android.location.LocationManager systemLocationManager;

    public LocationServiceManager(MainActivity activity, LocationListener locationListener) {
        this.activity = activity;
        this.locationListener = locationListener;

        initializeLocationService();
    }

    public void initializeLocationService() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted");
            return;
        }

        systemLocationManager = (android.location.LocationManager) activity.getSystemService(MainActivity.LOCATION_SERVICE);

        if (systemLocationManager == null) {
            Log.e(TAG, "LocationManager is null");
            activity.showToast("Location services not available");
            return;
        }

        try {
            // Request location updates from GPS
            if (systemLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                systemLocationManager.requestLocationUpdates(
                        android.location.LocationManager.GPS_PROVIDER,
                        Constants.LOCATION_UPDATE_INTERVAL,
                        Constants.LOCATION_MIN_DISTANCE,
                        locationListener);
                Log.d(TAG, "GPS location updates requested");
            }

            // Also request from network provider as backup
            if (systemLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                systemLocationManager.requestLocationUpdates(
                        android.location.LocationManager.NETWORK_PROVIDER,
                        Constants.LOCATION_UPDATE_INTERVAL,
                        Constants.LOCATION_MIN_DISTANCE,
                        locationListener);
                Log.d(TAG, "Network location updates requested");
            }

            // Get last known location immediately
            getLastKnownLocation();

        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception when requesting location updates: " + e.getMessage());
            activity.showToast("Location permission denied");
        }
    }

    private void getLastKnownLocation() {
        if (!hasLocationPermission() || systemLocationManager == null) {
            return;
        }

        try {
            android.location.Location gpsLocation = systemLocationManager.getLastKnownLocation(
                    android.location.LocationManager.GPS_PROVIDER);
            android.location.Location networkLocation = systemLocationManager.getLastKnownLocation(
                    android.location.LocationManager.NETWORK_PROVIDER);

            // Use GPS location if available and recent, otherwise use network location
            android.location.Location bestLocation = null;
            if (gpsLocation != null && networkLocation != null) {
                bestLocation = gpsLocation.getTime() > networkLocation.getTime() ? gpsLocation : networkLocation;
            } else if (gpsLocation != null) {
                bestLocation = gpsLocation;
            } else if (networkLocation != null) {
                bestLocation = networkLocation;
            }

            if (bestLocation != null) {
                locationListener.onLocationChanged(bestLocation);
                Log.d(TAG, String.format("Last known location: %.6f, %.6f",
                        bestLocation.getLatitude(), bestLocation.getLongitude()));
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception when getting last known location: " + e.getMessage());
        }
    }

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isGPSEnabled() {
        return systemLocationManager != null &&
                systemLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    public boolean isNetworkLocationEnabled() {
        return systemLocationManager != null &&
                systemLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
    }

    public void cleanup() {
        if (systemLocationManager != null && hasLocationPermission()) {
            try {
                systemLocationManager.removeUpdates(locationListener);
                Log.d(TAG, "Location updates stopped");
            } catch (SecurityException e) {
                Log.e(TAG, "Security Exception when removing location updates: " + e.getMessage());
            }
        }
    }

    public String getLocationStatus() {
        if (!hasLocationPermission()) {
            return "Location permission not granted";
        }

        if (systemLocationManager == null) {
            return "Location services not available";
        }

        boolean gpsEnabled = isGPSEnabled();
        boolean networkEnabled = isNetworkLocationEnabled();

        if (gpsEnabled && networkEnabled) {
            return "GPS and Network location enabled";
        } else if (gpsEnabled) {
            return "GPS location enabled";
        } else if (networkEnabled) {
            return "Network location enabled";
        } else {
            return "No location providers enabled";
        }
    }
}