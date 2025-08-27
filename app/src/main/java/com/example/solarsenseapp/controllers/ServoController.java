package com.example.solarsenseapp.controllers;

import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.solarsenseapp.MainActivity;
import com.example.solarsenseapp.network.ESPCommunicator;
import com.example.solarsenseapp.utils.Constants;

import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServoController {
    private static final String TAG = "ServoController";

    private final MainActivity activity;
    private final ESPCommunicator espCommunicator;

    // UI Components
    private final SeekBar baseServoSeekBar;
    private final SeekBar panelServoSeekBar;
    private final TextView baseServoValue;
    private final TextView panelServoValue;

    private Button btnBaseClockwise, btnBaseCounterClockwise;
    private Button btnPanelToZero, btnPanelToMax;

    // Current positions
    private int baseCurrentAngle = Constants.DEFAULT_BASE_ANGLE;
    private int panelCurrentAngle = Constants.DEFAULT_PANEL_ANGLE;

    public ServoController(MainActivity activity, ESPCommunicator espCommunicator,
                           SeekBar baseServoSeekBar, SeekBar panelServoSeekBar,
                           TextView baseServoValue, TextView panelServoValue) {
        this.activity = activity;
        this.espCommunicator = espCommunicator;
        this.baseServoSeekBar = baseServoSeekBar;
        this.panelServoSeekBar = panelServoSeekBar;
        this.baseServoValue = baseServoValue;
        this.panelServoValue = panelServoValue;

        initializeButtons();
    }

    private void initializeButtons() {
        btnBaseClockwise = activity.findViewById(com.example.solarsenseapp.R.id.btnBaseClockwise);
        btnBaseCounterClockwise = activity.findViewById(com.example.solarsenseapp.R.id.btnBaseCounterClockwise);
        btnPanelToZero = activity.findViewById(com.example.solarsenseapp.R.id.btnPanelZero);
        btnPanelToMax = activity.findViewById(com.example.solarsenseapp.R.id.btnPanelMax);
    }

    public void setupSeekBars(BooleanSupplier isAutoModeSupplier) {
        // Base servo setup with reversed controls
        baseServoSeekBar.setMax(Constants.SERVO_MAX_ANGLE);
        baseServoSeekBar.setProgress(baseCurrentAngle);
        baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);

        baseServoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isAutoModeSupplier.getAsBoolean()) {
                    // Calculate reversed value (180 - progress)
                    baseCurrentAngle = Constants.SERVO_MAX_ANGLE - progress;
                    baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
                    sendBaseServoCommand(baseCurrentAngle);
                    seekBar.setProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Panel servo setup
        panelServoSeekBar.setMax(Constants.SERVO_MAX_ANGLE);
        panelServoSeekBar.setProgress(panelCurrentAngle);
        panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);

        panelServoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isAutoModeSupplier.getAsBoolean()) {
                    panelCurrentAngle = progress;
                    panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
                    sendPanelServoCommand(panelCurrentAngle);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public void setupButtons() {
        // Reversed button actions for base
        btnBaseClockwise.setOnClickListener(v -> adjustBaseServo(-Constants.SERVO_STEP_SIZE, false));
        btnBaseCounterClockwise.setOnClickListener(v -> adjustBaseServo(Constants.SERVO_STEP_SIZE, false));

        // Panel buttons
        btnPanelToZero.setOnClickListener(v -> setPanelServo(Constants.SERVO_MIN_ANGLE, false));
        btnPanelToMax.setOnClickListener(v -> setPanelServo(Constants.SERVO_MAX_ANGLE, false));
    }

    public void adjustBaseServo(int delta, boolean isAutoMode) {
        if (!isAutoMode) {
            baseCurrentAngle = Math.max(Constants.SERVO_MIN_ANGLE,
                    Math.min(Constants.SERVO_MAX_ANGLE, baseCurrentAngle + delta));

            // Update seekbar with reversed value
            baseServoSeekBar.setProgress(Constants.SERVO_MAX_ANGLE - baseCurrentAngle);
            baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
            sendBaseServoCommand(baseCurrentAngle);
        }
    }

    public void adjustPanelServo(int delta, boolean isAutoMode) {
        if (!isAutoMode) {
            panelCurrentAngle = Math.max(Constants.SERVO_MIN_ANGLE,
                    Math.min(Constants.SERVO_MAX_ANGLE, panelCurrentAngle + delta));

            panelServoSeekBar.setProgress(panelCurrentAngle);
            panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
            sendPanelServoCommand(panelCurrentAngle);
        }
    }

    public void setBaseServo(int angle, boolean isAutoMode) {
        if (!isAutoMode) {
            baseCurrentAngle = Math.max(Constants.SERVO_MIN_ANGLE,
                    Math.min(Constants.SERVO_MAX_ANGLE, angle));

            baseServoSeekBar.setProgress(Constants.SERVO_MAX_ANGLE - baseCurrentAngle);
            baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
            sendBaseServoCommand(baseCurrentAngle);
        }
    }

    public void setPanelServo(int angle, boolean isAutoMode) {
        if (!isAutoMode) {
            panelCurrentAngle = Math.max(Constants.SERVO_MIN_ANGLE,
                    Math.min(Constants.SERVO_MAX_ANGLE, angle));

            panelServoSeekBar.setProgress(panelCurrentAngle);
            panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
            sendPanelServoCommand(panelCurrentAngle);
        }
    }

    // Auto mode servo control (bypasses manual restrictions)
    public void setBaseServoAuto(int angle) {
        baseCurrentAngle = Math.max(Constants.SERVO_MIN_ANGLE,
                Math.min(Constants.SERVO_MAX_ANGLE, angle));

        activity.runOnUiThread(() -> {
            baseServoSeekBar.setProgress(Constants.SERVO_MAX_ANGLE - baseCurrentAngle);
            baseServoValue.setText("Base: " + baseCurrentAngle + "°");
        });

        sendBaseServoCommand(baseCurrentAngle);
    }

    public void setPanelServoAuto(int angle) {
        panelCurrentAngle = Math.max(Constants.SERVO_MIN_ANGLE,
                Math.min(Constants.SERVO_MAX_ANGLE, angle));

        activity.runOnUiThread(() -> {
            panelServoSeekBar.setProgress(panelCurrentAngle);
            panelServoValue.setText("Panel: " + panelCurrentAngle + "°");
        });

        sendPanelServoCommand(panelCurrentAngle);
    }

    public void extractAndSetAngle(String command, boolean isAutoMode) {
        try {
            Pattern pattern = Pattern.compile("(\\d{1,3})");
            Matcher matcher = pattern.matcher(command);
            if (matcher.find()) {
                int angle = Math.max(Constants.SERVO_MIN_ANGLE,
                        Math.min(Constants.SERVO_MAX_ANGLE, Integer.parseInt(matcher.group(1))));

                if (command.contains("base")) {
                    setBaseServo(angle, isAutoMode);
                } else if (command.contains("panel")) {
                    setPanelServo(angle, isAutoMode);
                } else if (command.contains("angle") || command.contains("set")) {
                    setPanelServo(angle, isAutoMode);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing angle", e);
        }
    }

    private void sendBaseServoCommand(int angle) {
        espCommunicator.sendServoCommand(Constants.BASE_SERVO_ENDPOINT + angle);
    }

    private void sendPanelServoCommand(int angle) {
        espCommunicator.sendServoCommand(Constants.PANEL_SERVO_ENDPOINT + angle);
    }

    public void disableManualControls() {
        baseServoSeekBar.setEnabled(false);
        panelServoSeekBar.setEnabled(false);
        btnBaseClockwise.setEnabled(false);
        btnBaseCounterClockwise.setEnabled(false);
        btnPanelToZero.setEnabled(false);
        btnPanelToMax.setEnabled(false);
    }

    public void enableManualControls() {
        baseServoSeekBar.setEnabled(true);
        panelServoSeekBar.setEnabled(true);
        btnBaseClockwise.setEnabled(true);
        btnBaseCounterClockwise.setEnabled(true);
        btnPanelToZero.setEnabled(true);
        btnPanelToMax.setEnabled(true);
    }

    // Getters
    public int getBaseCurrentAngle() {
        return baseCurrentAngle;
    }

    public int getPanelCurrentAngle() {
        return panelCurrentAngle;
    }
}