package com.example.solarsenseapp.network;

import android.util.Log;

import com.example.solarsenseapp.utils.Constants;

import java.net.HttpURLConnection;
import java.net.URL;

public class ESPCommunicator {
    private static final String TAG = "ESPCommunicator";

    private String espIp = Constants.DEFAULT_ESP_IP;

    public interface ESPCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public void updateIP(String newIp) {
        this.espIp = newIp;
        Log.d(TAG, "ESP IP updated to: " + espIp);
    }

    public String getCurrentIP() {
        return espIp;
    }

    public void sendServoCommand(String endpoint) {
        sendServoCommand(endpoint, null);
    }

    public void sendServoCommand(String endpoint, ESPCallback callback) {
        String fullUrl = espIp + endpoint;
        sendRequest(fullUrl, callback);
    }

    public void sendRequest(String urlStr) {
        sendRequest(urlStr, null);
    }

    public void sendRequest(String urlStr, ESPCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(Constants.CONNECTION_TIMEOUT);
                connection.setReadTimeout(Constants.READ_TIMEOUT);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "ESP request successful: " + urlStr);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    String errorMsg = "ESP responded with code: " + responseCode;
                    Log.w(TAG, errorMsg + " for URL: " + urlStr);
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }
            } catch (Exception e) {
                String errorMsg = "Network Error: " + e.getMessage();
                Log.e(TAG, errorMsg + " for URL: " + urlStr, e);
                if (callback != null) {
                    callback.onError(errorMsg);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    // Test connection to ESP
    public void testConnection(ESPCallback callback) {
        sendRequest(espIp + "/", callback);
    }

    // Send both servo commands simultaneously
    public void sendBothServos(int baseAngle, int panelAngle, ESPCallback callback) {
        sendServoCommand(Constants.BASE_SERVO_ENDPOINT + baseAngle, new ESPCallback() {
            @Override
            public void onSuccess() {
                sendServoCommand(Constants.PANEL_SERVO_ENDPOINT + panelAngle, callback);
            }

            @Override
            public void onError(String errorMessage) {
                if (callback != null) {
                    callback.onError("Base servo error: " + errorMessage);
                }
            }
        });
    }
}