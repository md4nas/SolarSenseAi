package com.example.solarsenseapp.managers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.solarsenseapp.MainActivity;
import com.example.solarsenseapp.utils.Constants;

public class PermissionManager {
    private static final String TAG = "PermissionManager";

    private final MainActivity activity;

    // Required permissions
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    public PermissionManager(MainActivity activity) {
        this.activity = activity;
    }

    public void checkAndRequestPermissions() {
        // Check microphone permission
        if (!hasMicrophonePermission()) {
            requestMicrophonePermission();
        }

        // Check location permissions
        if (!hasLocationPermissions()) {
            requestLocationPermissions();
        }

        // Log permission status
        logPermissionStatus();
    }

    public boolean hasMicrophonePermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasInternetPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicrophonePermission() {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                Constants.RECORD_AUDIO_PERMISSION_REQUEST);
        Log.d(TAG, "Requesting microphone permission");
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                Constants.LOCATION_PERMISSION_REQUEST);
        Log.d(TAG, "Requesting location permissions");
    }

    public void handlePermissionResult(int requestCode, int[] grantResults) {
        switch (requestCode) {
            case Constants.RECORD_AUDIO_PERMISSION_REQUEST:
                handleMicrophonePermissionResult(grantResults);
                break;

            case Constants.LOCATION_PERMISSION_REQUEST:
                handleLocationPermissionResult(grantResults);
                break;

            default:
                Log.w(TAG, "Unknown permission request code: " + requestCode);
                break;
        }
    }

    private void handleMicrophonePermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Microphone permission granted");
            activity.showToast("Microphone permission granted - Voice commands enabled");
        } else {
            Log.w(TAG, "Microphone permission denied");
            activity.showToast("Microphone permission denied - Voice commands disabled");
        }
    }

    private void handleLocationPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission granted");
            activity.showToast("Location permission granted - Auto tracking enabled");
        } else {
            Log.w(TAG, "Location permission denied");
            activity.showToast("Location permission denied - Manual location entry required");
        }
    }

    private void logPermissionStatus() {
        Log.d(TAG, "Permission Status:");
        Log.d(TAG, "  Microphone: " + (hasMicrophonePermission() ? "GRANTED" : "DENIED"));
        Log.d(TAG, "  Fine Location: " + (hasFineLocationPermission() ? "GRANTED" : "DENIED"));
        Log.d(TAG, "  Coarse Location: " + (hasLocationPermissions() ? "GRANTED" : "DENIED"));
        Log.d(TAG, "  Internet: " + (hasInternetPermission() ? "GRANTED" : "DENIED"));
    }

    public boolean areAllPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public String[] getDeniedPermissions() {
        java.util.List<String> denied = new java.util.ArrayList<>();

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                denied.add(permission);
            }
        }

        return denied.toArray(new String[0]);
    }

    public String getPermissionStatusString() {
        StringBuilder status = new StringBuilder("Permission Status:\n");

        status.append("🎤 Microphone: ").append(hasMicrophonePermission() ? "✓" : "✗").append("\n");
        status.append("📍 Location: ").append(hasLocationPermissions() ? "✓" : "✗").append("\n");
        status.append("🌐 Internet: ").append(hasInternetPermission() ? "✓" : "✗").append("\n");

        return status.toString();
    }
}