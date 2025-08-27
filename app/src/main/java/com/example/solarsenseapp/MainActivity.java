package com.example.solarsenseapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.solarsenseapp.controllers.ServoController;
import com.example.solarsenseapp.controllers.VoiceController;
import com.example.solarsenseapp.controllers.WeatherController;
import com.example.solarsenseapp.managers.AutoTrackingManager;
import com.example.solarsenseapp.managers.LocationServiceManager;
import com.example.solarsenseapp.managers.PermissionManager;
import com.example.solarsenseapp.network.ESPCommunicator;
import com.example.solarsenseapp.utils.Constants;
import com.example.solarsenseapp.utils.SolarCalculator;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "SolarSenseApp";

    // UI Components
    private EditText locationInput, espIpInput;
    private Button btnFetchWeather, btnUpdateIp;
    private TextView weatherDataText, txtVoiceCommands;
    private SeekBar baseServoSeekBar, panelServoSeekBar;
    private TextView baseServoValue, panelServoValue;
    private Button btnBaseClockwise, btnBaseCounterClockwise;
    private Button btnPanelToZero, btnPanelToMax, btnVoice, btnShowCommands;
    private ProgressBar voiceProgressBar;
    private ToggleButton toggleAutoMode;
    private Button btnResetLocation;

    // Controllers and Managers
    private ServoController servoController;
    private VoiceController voiceController;
    private WeatherController weatherController;
    private AutoTrackingManager autoTrackingManager;
    private LocationServiceManager locationServiceManager;
    private PermissionManager permissionManager;
    private ESPCommunicator espCommunicator;
    private TextToSpeech textToSpeech;

    // System State
    private Location currentLocation;
    private boolean isAutoMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUI();
        initializeControllers();
        setupPermissions();
        setupUIListeners();
        initializeTextToSpeech();
    }

    private void initializeUI() {
        baseServoSeekBar = findViewById(R.id.baseServoSeekBar);
        panelServoSeekBar = findViewById(R.id.panelServoSeekBar);
        baseServoValue = findViewById(R.id.baseServoValue);
        panelServoValue = findViewById(R.id.panelServoValue);
        btnBaseClockwise = findViewById(R.id.btnBaseClockwise);
        btnBaseCounterClockwise = findViewById(R.id.btnBaseCounterClockwise);
        btnPanelToZero = findViewById(R.id.btnPanelZero);
        btnPanelToMax = findViewById(R.id.btnPanelMax);
        btnVoice = findViewById(R.id.btnVoice);
        voiceProgressBar = findViewById(R.id.voiceProgressBar);
        locationInput = findViewById(R.id.locationInput);
        btnFetchWeather = findViewById(R.id.btnFetchWeather);
        weatherDataText = findViewById(R.id.weatherDataText);
        toggleAutoMode = findViewById(R.id.toggleAutoMode);
        btnShowCommands = findViewById(R.id.btnShowCommands);
        txtVoiceCommands = findViewById(R.id.txtVoiceCommands);
        espIpInput = findViewById(R.id.espIpInput);
        btnUpdateIp = findViewById(R.id.btnUpdateIp);
        btnResetLocation = findViewById(R.id.btnResetLocation);

        espIpInput.setText(Constants.DEFAULT_ESP_IP.replace("http://", ""));
        setupVoiceCommandsText();
    }

    private void initializeControllers() {
        espCommunicator = new ESPCommunicator();
        servoController = new ServoController(this, espCommunicator, baseServoSeekBar,
                panelServoSeekBar, baseServoValue, panelServoValue);

        voiceController = new VoiceController(this, btnVoice, voiceProgressBar,
                txtVoiceCommands, btnShowCommands);

        weatherController = new WeatherController(this, weatherDataText, servoController);

        autoTrackingManager = new AutoTrackingManager(this, servoController);

        locationServiceManager = new LocationServiceManager(this, this);

        permissionManager = new PermissionManager(this);

        // Set voice command callback
        voiceController.setVoiceCommandCallback(this::handleVoiceCommand);
    }

    private void setupPermissions() {
        permissionManager.checkAndRequestPermissions();
    }

    private void setupUIListeners() {
        servoController.setupSeekBars(this::isAutoMode);
        servoController.setupButtons();

        btnUpdateIp.setOnClickListener(v -> updateESPIP());
        btnResetLocation.setOnClickListener(v -> resetLocation());
        btnFetchWeather.setOnClickListener(v -> fetchWeather());

        toggleAutoMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAutoMode = isChecked;
            if (isAutoMode) {
                startAutoMode();
            } else {
                stopAutoMode();
            }
        });
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });
    }

    private void setupVoiceCommandsText() {
        txtVoiceCommands.setText(
                "Voice Commands:\n" +
                        "- 'Clockwise' / 'Counter-clockwise' (base rotation)\n" +
                        "- 'Up' / 'Down' (panel tilt)\n" +
                        "- 'Panel to zero/max' (panel position)\n" +
                        "- 'Base to zero/max' (base position)\n" +
                        "- 'Auto mode on/off'\n" +
                        "- 'Weather for [location]'\n" +
                        "- 'Reset location' (clears current location)\n" +
                        "- '[Number] degrees' (set angle)");
        txtVoiceCommands.setVisibility(View.GONE);
    }

    // Auto Mode Methods
    private void startAutoMode() {
        servoController.disableManualControls();
        autoTrackingManager.startAutoMode(currentLocation, locationInput.getText().toString());
    }

    private void stopAutoMode() {
        servoController.enableManualControls();
        autoTrackingManager.stopAutoMode();
    }

    // Voice Command Handling
    private void handleVoiceCommand(String command) {
        if (command.contains("clockwise")) {
            servoController.adjustBaseServo(-40, isAutoMode);
        } else if (command.contains("counter") || command.contains("anti-clockwise")) {
            servoController.adjustBaseServo(40, isAutoMode);
        } else if (command.contains("up") || command.contains("panel up")) {
            servoController.adjustPanelServo(40, isAutoMode);
        } else if (command.contains("down") || command.contains("panel down")) {
            servoController.adjustPanelServo(-40, isAutoMode);
        } else if (command.contains("panel zero")) {
            servoController.setPanelServo(0, isAutoMode);
        } else if (command.contains("panel max")) {
            servoController.setPanelServo(180, isAutoMode);
        } else if (command.contains("base zero")) {
            servoController.setBaseServo(0, isAutoMode);
        } else if (command.contains("base max")) {
            servoController.setBaseServo(180, isAutoMode);
        } else if (command.contains("auto mode on") || command.contains("start tracking")) {
            runOnUiThread(() -> toggleAutoMode.setChecked(true));
        } else if (command.contains("auto mode off") || command.contains("stop tracking")) {
            runOnUiThread(() -> toggleAutoMode.setChecked(false));
        } else if (command.contains("weather")) {
            weatherController.handleWeatherVoiceCommand(command, locationInput);
        } else if (command.contains("reset location") || command.contains("clear location")) {
            resetLocation();
        } else {
            servoController.extractAndSetAngle(command, isAutoMode);
        }
    }

    // UI Event Handlers
    private void updateESPIP() {
        String newIp = espIpInput.getText().toString().trim();
        if (!newIp.isEmpty()) {
            String fullIp = "http://" + newIp;
            espCommunicator.updateIP(fullIp);
            showToast("ESP IP updated to: " + fullIp);
        }
    }

    private void resetLocation() {
        locationInput.setText("");
        weatherDataText.setText("");

        if (isAutoMode) {
            toggleAutoMode.setChecked(false);
            stopAutoMode();
        }

        currentLocation = null;
        showToast("Location has been reset");
        speakFeedback("Location reset complete");
    }

    private void fetchWeather() {
        String location = locationInput.getText().toString().trim();
        if (!location.isEmpty()) {
            weatherController.fetchWeatherData(location);
        } else {
            showToast("Please enter a location");
        }
    }

    // Location Listener Implementation
    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        if (isAutoMode) {
            autoTrackingManager.updatePanelPosition(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    // Permission Handling
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionManager.handlePermissionResult(requestCode, grantResults);

        if (requestCode == Constants.LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationServiceManager.initializeLocationService();
            }
        }
    }

    // Helper Methods
    public void speakFeedback(String message) {
        if (textToSpeech != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    public boolean isAutoMode() {
        return isAutoMode;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceController != null) {
            voiceController.cleanup();
        }
        if (locationServiceManager != null) {
            locationServiceManager.cleanup();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (autoTrackingManager != null) {
            autoTrackingManager.cleanup();
        }
    }
}