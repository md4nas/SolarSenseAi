package com.example.solarsenseapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private SeekBar baseServoSeekBar, panelServoSeekBar;
    private TextView baseServoValue, panelServoValue;
    private Button btnBaseClockwise, btnBaseCounterClockwise;
    private Button btnPanelToZero, btnPanelToMax;

    private final String espIp = "http://192.168.63.219"; // Replace with your actual ESP IP

    private int baseCurrentAngle = 90;   // Default position
    private int panelCurrentAngle = 90;  // Default position

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        baseServoSeekBar = findViewById(R.id.baseServoSeekBar);
        panelServoSeekBar = findViewById(R.id.panelServoSeekBar);
        baseServoValue = findViewById(R.id.baseServoValue);
        panelServoValue = findViewById(R.id.panelServoValue);

        btnBaseClockwise = findViewById(R.id.btnBaseClockwise);
        btnBaseCounterClockwise = findViewById(R.id.btnBaseCounterClockwise);
        btnPanelToZero = findViewById(R.id.btnPanelZero);
        btnPanelToMax = findViewById(R.id.btnPanelMax);

        // --- Base Servo SeekBar Setup (0–180) ---
        baseServoSeekBar.setMax(180);
        baseServoSeekBar.setProgress(baseCurrentAngle);
        baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);

        baseServoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                baseCurrentAngle = progress;
                baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
                sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // --- Panel Servo SeekBar Setup (0–180) ---
        panelServoSeekBar.setMax(180);
        panelServoSeekBar.setProgress(panelCurrentAngle);
        panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);

        panelServoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                panelCurrentAngle = progress;
                panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
                sendRequest(espIp + "/panelServo?angle=" + panelCurrentAngle);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // --- Base Clockwise Button ---
        btnBaseClockwise.setOnClickListener(v -> {
            baseCurrentAngle += 15;
            if (baseCurrentAngle > 180) baseCurrentAngle = 180;
            baseServoSeekBar.setProgress(baseCurrentAngle);
            baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
            sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
        });

        // --- Base CounterClockwise Button ---
        btnBaseCounterClockwise.setOnClickListener(v -> {
            baseCurrentAngle -= 15;
            if (baseCurrentAngle < 0) baseCurrentAngle = 0;
            baseServoSeekBar.setProgress(baseCurrentAngle);
            baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
            sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
        });

        // --- Panel to 0° Button ---
        btnPanelToZero.setOnClickListener(v -> {
            panelCurrentAngle = 0;
            panelServoSeekBar.setProgress(panelCurrentAngle);
            panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
            sendRequest(espIp + "/panelServo?angle=" + panelCurrentAngle);
        });

        // --- Panel to Max (180°) Button ---
        btnPanelToMax.setOnClickListener(v -> {
            panelCurrentAngle = 180;
            panelServoSeekBar.setProgress(panelCurrentAngle);
            panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
            sendRequest(espIp + "/panelServo?angle=" + panelCurrentAngle);
        });
    }

    // Send GET request to the ESP
    private void sendRequest(String urlStr) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                int responseCode = connection.getResponseCode();

                // Check if the response code is successful (200 OK)
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    showErrorToast("Failed to connect to ESP!");
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                showErrorToast("Network Error: " + e.getMessage());
            }
        }).start();
    }

    // Method to show error message on the UI
    private void showErrorToast(String message) {
        // Ensure UI updates are done on the main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }
}
