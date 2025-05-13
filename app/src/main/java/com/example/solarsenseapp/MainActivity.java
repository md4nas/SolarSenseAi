package com.example.solarsenseapp;

import static androidx.constraintlayout.motion.widget.Debug.getLocation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements LocationListener {

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

    // Constants
    private static final String API_KEY = "4caa58a2c8523065469f2b1e7bd4c050";
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int RECORD_AUDIO_PERMISSION_REQUEST = 101;

    // System State
    private String espIp = "http://192.168.63.219";
    private int baseCurrentAngle = 90;
    private int panelCurrentAngle = 90;
    private boolean isAutoMode = false;
    private Location currentLocation;

    // Handlers and Services
    private Handler autoHandler = new Handler();
    private Runnable autoRunnable;
    private LocationManager locationManager;
    private SpeechRecognizer speechRecognizer;
    private boolean isAutoModeEnabled = false;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private Handler handler = new Handler();
    private Runnable autoUpdater;
    private Handler azimuthHandler = new Handler();
    private Runnable azimuthUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUI();
        checkAndRequestPermissions();
        setupSeekBars();
        setupButtons();
        setupVoiceRecognition();
        setupWeatherFetching();
        setupAutoModeToggle();
        initializeLocationService();
        setupAzimuthUpdater();

    }
    private void setupAutoModeToggle() {
        autoRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAutoMode) {
                    if (currentLocation != null) {
                        // Calculate and send new panel position based on sun position
                        latitude = currentLocation.getLatitude();
                        longitude = currentLocation.getLongitude();
                        Date now = new Date();
                        double azimuth = calculateAzimuth(latitude, longitude, now);
                        double elevation = calculateElevation(latitude, longitude, now);

                        // Convert angles to servo positions (0-180)
                        int baseAngle = (int) Math.round(azimuth % 180);
                        int panelAngle = (int) Math.round(elevation % 180);

                        // Update UI
                        runOnUiThread(() -> {
                            baseServoSeekBar.setProgress(baseAngle);
                            baseServoValue.setText("Base Servo Angle: " + baseAngle);
                            panelServoSeekBar.setProgress(panelAngle);
                            panelServoValue.setText("Panel Servo Angle: " + panelAngle);
                        });

                        // Send commands to ESP
                        sendRequest(espIp + "/baseServo?angle=" + baseAngle);
                        sendRequest(espIp + "/panelServo?angle=" + panelAngle);

                        Log.d("AutoMode", "Adjusted panel to azimuth: " + azimuth + ", elevation: " + elevation);
                    } else {
                        String location = locationInput.getText().toString().trim();
                        if (!location.isEmpty()) {
                            calculatePanelAngleFromLocation(location);
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,
                                        "No location available for auto mode",
                                        Toast.LENGTH_SHORT).show();
                                toggleAutoMode.setChecked(false);
                            });
                        }
                    }
                    autoHandler.postDelayed(this, 300000); // 5 minutes
                }
            }
        };

        toggleAutoMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAutoMode = isChecked;
            isAutoModeEnabled = isChecked;
            if (isAutoMode) {
                disableManualControls();

                // Immediately calculate and set initial position
                if (currentLocation != null) {
                    autoHandler.post(autoRunnable); // Start the tracking loop
                } else {
                    // Try to get location from text input
                    String location = locationInput.getText().toString().trim();
                    if (!location.isEmpty()) {
                        calculatePanelAngleFromLocation(location);
                        autoHandler.post(autoRunnable); // Start the tracking loop
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    "Enable GPS or enter location for auto mode",
                                    Toast.LENGTH_LONG).show();
                            toggleAutoMode.setChecked(false);
                        });
                    }
                }
            } else {
                enableManualControls();
                autoHandler.removeCallbacks(autoRunnable);
            }
        });
    }

    // elevation calculation method
    private double calculateElevation(double latitude, double longitude, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);

        // Approximate declination angle (δ)
        double decl = 23.45 * Math.sin(Math.toRadians(360.0 / 365.0 * (284 + dayOfYear)));

        double hourAngle = (cal.get(Calendar.HOUR_OF_DAY) - 12) * 15;  // degrees
        double latRad = Math.toRadians(latitude);
        double declRad = Math.toRadians(decl);
        double hourRad = Math.toRadians(hourAngle);

        // Solar elevation angle calculation
        double elevation = Math.toDegrees(Math.asin(
                Math.sin(latRad) * Math.sin(declRad) +
                        Math.cos(latRad) * Math.cos(declRad) * Math.cos(hourRad)
        ));

        return elevation;
    }

    private void setupAzimuthUpdater() {
        azimuthUpdater = new Runnable() {
            @Override
            public void run() {
                if (isAutoModeEnabled && currentLocation != null) {
                    latitude = currentLocation.getLatitude();
                    longitude = currentLocation.getLongitude();
                    Date now = new Date();
                    double azimuth = calculateAzimuth(latitude, longitude, now);
                    sendAzimuthToESP8266(azimuth);
                    azimuthHandler.postDelayed(this, 600000); // Every 10 minutes
                }
            }
        };
    }

    private void sendAzimuthToESP8266(double azimuth) {
        // Convert azimuth to servo angle (0-180) if needed
        int servoAngle = (int) Math.round(azimuth % 180);
        String url = espIp + "/autoAzimuth?angle=" + servoAngle;
        sendRequest(url);
    }

    public static double calculateAzimuth(double latitude, double longitude, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);

        // Approximate declination angle (δ)
        double decl = 23.45 * Math.sin(Math.toRadians(360.0 / 365.0 * (284 + dayOfYear)));

        double hourAngle = (cal.get(Calendar.HOUR_OF_DAY) - 12) * 15;  // degrees
        double latRad = Math.toRadians(latitude);
        double declRad = Math.toRadians(decl);
        double hourRad = Math.toRadians(hourAngle);

        double azimuth = Math.toDegrees(Math.acos(
                (Math.sin(declRad) - Math.sin(latRad) * Math.sin(declRad)) /
                        (Math.cos(latRad) * Math.cos(declRad))
        ));

        return azimuth;  // returns angle to send to servo
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

        espIpInput.setText(espIp.replace("http://", ""));
    }

    private void checkAndRequestPermissions() {
        // Check microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_REQUEST);
        }

        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupVoiceRecognition();
            } else {
                Toast.makeText(this, "Voice commands disabled - microphone permission required",
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeLocationService();
            } else {
                Toast.makeText(this, "Auto mode disabled - location permission required",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeLocationService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            try {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        10000,  // 10 seconds
                        10,     // 10 meters
                        this);
            } catch (SecurityException e) {
                Log.e("Location", "Security Exception: " + e.getMessage());
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        if (isAutoMode) {
            updatePanelPosition();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    private void setupSeekBars() {
        baseServoSeekBar.setMax(180);
        baseServoSeekBar.setProgress(baseCurrentAngle);
        baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
        baseServoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isAutoMode) {
                    baseCurrentAngle = progress;
                    baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
                    sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        panelServoSeekBar.setMax(180);
        panelServoSeekBar.setProgress(panelCurrentAngle);
        panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
        panelServoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isAutoMode) {
                    panelCurrentAngle = progress;
                    panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
                    sendRequest(espIp + "/panelServo?angle=" + panelCurrentAngle);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupButtons() {
        btnBaseClockwise.setOnClickListener(v -> {
            if (!isAutoMode) {
                baseCurrentAngle = Math.min(baseCurrentAngle + 15, 180);
                baseServoSeekBar.setProgress(baseCurrentAngle);
                sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
            }
        });

        btnBaseCounterClockwise.setOnClickListener(v -> {
            if (!isAutoMode) {
                baseCurrentAngle = Math.max(baseCurrentAngle - 15, 0);
                baseServoSeekBar.setProgress(baseCurrentAngle);
                sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
            }
        });

        btnPanelToZero.setOnClickListener(v -> {
            if (!isAutoMode) {
                panelCurrentAngle = 0;
                panelServoSeekBar.setProgress(panelCurrentAngle);
                sendRequest(espIp + "/panelServo?angle=0");
            }
        });

        btnPanelToMax.setOnClickListener(v -> {
            if (!isAutoMode) {
                panelCurrentAngle = 180;
                panelServoSeekBar.setProgress(panelCurrentAngle);
                sendRequest(espIp + "/panelServo?angle=180");
            }
        });

        final String[] finalEspIp = {espIp}; // Create a final array to hold the IP
        btnUpdateIp.setOnClickListener(v -> {
            String newIp = espIpInput.getText().toString().trim();
            if (!newIp.isEmpty()) {
                finalEspIp[0] = "http://" + newIp;
                espIp = finalEspIp[0]; // Update the original variable
                Toast.makeText(this, "ESP IP updated to: " + espIp, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupVoiceRecognition() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {}

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {}

                @Override
                public void onError(int error) {
                    runOnUiThread(() -> {
                        voiceProgressBar.setVisibility(View.GONE);
                        btnVoice.setText("🎤 Voice Command");
                        btnVoice.setBackgroundColor(getColor(android.R.color.holo_purple));
                        Toast.makeText(MainActivity.this,
                                "Voice error: " + getErrorText(error),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResults(Bundle results) {
                    runOnUiThread(() -> {
                        voiceProgressBar.setVisibility(View.GONE);
                        btnVoice.setText("🎤 Voice Command");
                        btnVoice.setBackgroundColor(getColor(android.R.color.holo_purple));

                        ArrayList<String> matches = results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty()) {
                            String command = matches.get(0).toLowerCase();
                            handleVoiceCommand(command);
                        }
                    });
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });

            btnVoice.setOnClickListener(v -> {
                if (!isAutoMode) {
                    voiceProgressBar.setVisibility(View.VISIBLE);
                    btnVoice.setText("Listening...");
                    btnVoice.setBackgroundColor(getColor(android.R.color.holo_red_dark));
                    speechRecognizer.startListening(speechIntent);
                }
            });
        } else {
            btnVoice.setEnabled(false);
            Toast.makeText(this, "Voice recognition not available on this device",
                    Toast.LENGTH_LONG).show();
        }

        btnShowCommands.setOnClickListener(v -> {
            if (txtVoiceCommands.getVisibility() == View.GONE) {
                txtVoiceCommands.setVisibility(View.VISIBLE);
                btnShowCommands.setText("⬆️ Hide Voice Commands");
            } else {
                txtVoiceCommands.setVisibility(View.GONE);
                btnShowCommands.setText("❓ Voice Commands");
            }
        });
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "RecognitionService busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }

    private void setupWeatherFetching() {
        btnFetchWeather.setOnClickListener(v -> {
            String location = locationInput.getText().toString().trim();
            if (!location.isEmpty()) {
                fetchWeatherData(location);
            } else {
                Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void disableManualControls() {
        baseServoSeekBar.setEnabled(false);
        panelServoSeekBar.setEnabled(false);
        btnBaseClockwise.setEnabled(false);
        btnBaseCounterClockwise.setEnabled(false);
        btnPanelToZero.setEnabled(false);
        btnPanelToMax.setEnabled(false);
        btnVoice.setEnabled(false);
    }

    private void enableManualControls() {
        baseServoSeekBar.setEnabled(true);
        panelServoSeekBar.setEnabled(true);
        btnBaseClockwise.setEnabled(true);
        btnBaseCounterClockwise.setEnabled(true);
        btnPanelToZero.setEnabled(true);
        btnPanelToMax.setEnabled(true);
        btnVoice.setEnabled(true);
    }

    private void sendRequest(final String urlStr) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                final int responseCode = connection.getResponseCode();
                runOnUiThread(() -> {
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        Toast.makeText(MainActivity.this,
                                "ESP responded with code: " + responseCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "Network Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void updatePanelPosition() {
        if (currentLocation != null) {
            String url = espIp + "/autoPosition?lat=" + currentLocation.getLatitude() +
                    "&lon=" + currentLocation.getLongitude();
            sendRequest(url);
        }
    }

    private void calculatePanelAngleFromLocation(String location) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(MainActivity.this);
                List<Address> addresses = geocoder.getFromLocationName(location, 1);
                if (addresses == null || addresses.isEmpty()) {
                    runOnUiThread(() -> {
                        showErrorToast("Location not found");
                        toggleAutoMode.setChecked(false);
                    });
                    return;
                }

                Address address = addresses.get(0);
                String url = espIp + "/autoPosition?lat=" + address.getLatitude() +
                        "&lon=" + address.getLongitude();
                sendRequest(url);

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "Updating position for: " + location,
                            Toast.LENGTH_SHORT).show();
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    showErrorToast("Error calculating position: " + e.getMessage());
                    toggleAutoMode.setChecked(false);
                });
            }
        }).start();
    }

    private void fetchWeatherData(final String location) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" +
                        location + "&units=metric&appid=" + API_KEY;
                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    final int responseCode = connection.getResponseCode(); // Create final copy
                    runOnUiThread(() -> {
                        showErrorToast("Weather API error: " + responseCode);
                    });
                    return;
                }

                Scanner scanner = new Scanner(connection.getInputStream());
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                parseWeatherData(response.toString(), location);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showErrorToast("Weather fetch error: " + e.getMessage());
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void parseWeatherData(String jsonResponse, String location) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject main = jsonObject.getJSONObject("main");
            double temp = main.getDouble("temp");
            int humidity = main.getInt("humidity");

            JSONObject wind = jsonObject.getJSONObject("wind");
            double windSpeed = wind.getDouble("speed");
            double windDeg = wind.has("deg") ? wind.getDouble("deg") : 0;
            String windDirectionLabel = getWindDirectionLabel(windDeg);

            // Weather condition tracking
            String weatherCondition = "Clear";
            String weatherIcon = "☀️";
            double rainAmount = 0;
            double snowAmount = 0;
            boolean isThunderstorm = false;

            JSONArray weatherArray = jsonObject.getJSONArray("weather");
            for (int i = 0; i < weatherArray.length(); i++) {
                JSONObject weatherObj = weatherArray.getJSONObject(i);
                weatherCondition = weatherObj.getString("main");
                String description = weatherObj.getString("description").toLowerCase();

                if (weatherCondition.equalsIgnoreCase("Rain")) {
                    weatherIcon = "🌧️";
                } else if (weatherCondition.equalsIgnoreCase("Snow")) {
                    weatherIcon = "❄️";
                } else if (weatherCondition.equalsIgnoreCase("Thunderstorm")) {
                    weatherIcon = "⚡";
                    isThunderstorm = true;
                } else if (weatherCondition.equalsIgnoreCase("Clouds")) {
                    weatherIcon = "☁️";
                }
            }

            // Get precipitation amounts
            if (jsonObject.has("rain")) {
                JSONObject rainObj = jsonObject.getJSONObject("rain");
                rainAmount = rainObj.has("1h") ? rainObj.getDouble("1h") : 0;
            }
            if (jsonObject.has("snow")) {
                JSONObject snowObj = jsonObject.getJSONObject("snow");
                snowAmount = snowObj.has("1h") ? snowObj.getDouble("1h") : 0;
            }

            // Create final variables for use in lambda
            final String finalWeatherCondition = weatherCondition;
            final String finalWeatherIcon = weatherIcon;
            final double finalRainAmount = rainAmount;
            final double finalSnowAmount = snowAmount;
            final boolean finalIsThunderstorm = isThunderstorm;

            runOnUiThread(() -> {
                // Update the main weather display
                String weatherInfo = String.format(Locale.getDefault(),
                        "%s Weather in %s\n\n" +
                                "🌡️ Temperature: %.1f°C\n" +
                                "💧 Humidity: %d%%\n" +
                                "🌬️ Wind: %.1f m/s %s\n" +
                                "%s Rain: %.1f mm\n" +
                                "%s Snow: %.1f mm\n" +
                                "%s Thunderstorm: %s",
                        finalWeatherIcon, location, temp, humidity,
                        windSpeed, windDirectionLabel,
                        finalRainAmount > 0 ? "🌧️" : "  ", finalRainAmount,
                        finalSnowAmount > 0 ? "❄️" : "  ", finalSnowAmount,
                        finalIsThunderstorm ? "⚡" : "  ", finalIsThunderstorm ? "Yes" : "No");

                weatherDataText.setText(weatherInfo);

                // Show weather alerts
                if (finalWeatherCondition.equalsIgnoreCase("Rain") ||
                        finalWeatherCondition.equalsIgnoreCase("Snow") ||
                        finalWeatherCondition.equalsIgnoreCase("Thunderstorm")) {

                    String alertMessage = "⚠️ " + finalWeatherCondition + " detected!";
                    Toast.makeText(MainActivity.this, alertMessage, Toast.LENGTH_LONG).show();

                    // For thunderstorms, automatically flatten panel
                    if (finalWeatherCondition.equalsIgnoreCase("Thunderstorm")) {
                        setPanelServo(0); // Flat position for safety
                        Toast.makeText(MainActivity.this,
                                "DANGER: Setting panel to flat position",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                showErrorToast("Error parsing weather data: " + e.getMessage());
            });
        }
    }

    private String formatPrecipitation(double amount, String type) {
        if (amount <= 0) return "None";
        return String.format(Locale.getDefault(), "%.1f mm %s", amount, type);
    }
    private String getWindDirectionLabel(double degrees) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return directions[(int) Math.round(((degrees % 360) / 45)) % 8];
    }

    private void handleVoiceCommand(String command) {
        command = command.toLowerCase();

        if (command.contains("clockwise")) {
            adjustBaseServo(15);
        } else if (command.contains("counter") || command.contains("anti-clockwise")) {
            adjustBaseServo(-15);
        } else if (command.contains("up")) {
            adjustPanelServo(15);
        } else if (command.contains("down")) {
            adjustPanelServo(-15);
        } else if (command.contains("panel zero") || command.contains("panel to zero")) {
            setPanelServo(0);
        } else if (command.contains("panel max") || command.contains("panel to max")) {
            setPanelServo(180);
        } else if (command.contains("base zero") || command.contains("base to zero")) {
            setBaseServo(0);
        } else if (command.contains("base max") || command.contains("base to max")) {
            setBaseServo(180);
        } else if (command.contains("auto mode on") || command.contains("start tracking")) {
            runOnUiThread(() -> toggleAutoMode.setChecked(true));
        } else if (command.contains("auto mode off") || command.contains("stop tracking")) {
            runOnUiThread(() -> toggleAutoMode.setChecked(false));
        } else if (command.contains("weather")) {
            String location = extractLocation(command);
            if (location != null && !location.isEmpty()) {
                runOnUiThread(() -> {
                    locationInput.setText(location);
                    fetchWeatherData(location);
                });
            } else if (!locationInput.getText().toString().isEmpty()) {
                runOnUiThread(() -> fetchWeatherData(locationInput.getText().toString()));
            }
        } else {
            // Try to extract numeric angle commands
            extractAndSetAngle(command);
        }
    }

    private String extractLocation(String command) {
        Pattern pattern = Pattern.compile("(weather|for|in)\\s+(.+?)(\\s+(please|now|today)|$)");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return null;
    }

    private void extractAndSetAngle(String command) {
        try {
            Pattern pattern = Pattern.compile("(\\d{1,3})");
            Matcher matcher = pattern.matcher(command);
            if (matcher.find()) {
                int angle = Integer.parseInt(matcher.group(1));
                angle = Math.max(0, Math.min(180, angle));

                if (command.contains("base")) {
                    setBaseServo(angle);
                } else if (command.contains("panel")) {
                    setPanelServo(angle);
                } else if (command.contains("angle") || command.contains("set")) {
                    // Default to panel if no specific servo mentioned
                    setPanelServo(angle);
                }
            }
        } catch (Exception e) {
            Log.e("VoiceCommand", "Error parsing angle", e);
        }
    }

    private void adjustBaseServo(int delta) {
        if (!isAutoMode) {
            baseCurrentAngle = Math.max(0, Math.min(180, baseCurrentAngle + delta));
            runOnUiThread(() -> {
                baseServoSeekBar.setProgress(baseCurrentAngle);
                baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
            });
            sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
        }
    }

    private void adjustPanelServo(int delta) {
        if (!isAutoMode) {
            panelCurrentAngle = Math.max(0, Math.min(180, panelCurrentAngle + delta));
            runOnUiThread(() -> {
                panelServoSeekBar.setProgress(panelCurrentAngle);
                panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
            });
            sendRequest(espIp + "/panelServo?angle=" + panelCurrentAngle);
        }
    }

    private void setBaseServo(int angle) {
        if (!isAutoMode) {
            baseCurrentAngle = Math.max(0, Math.min(180, angle));
            runOnUiThread(() -> {
                baseServoSeekBar.setProgress(baseCurrentAngle);
                baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
            });
            sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
        }
    }

    private void setPanelServo(int angle) {
        if (!isAutoMode) {
            panelCurrentAngle = Math.max(0, Math.min(180, angle));
            runOnUiThread(() -> {
                panelServoSeekBar.setProgress(panelCurrentAngle);
                panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
            });
            sendRequest(espIp + "/panelServo?angle=" + panelCurrentAngle);
        }
    }

    private void showErrorToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        autoHandler.removeCallbacks(autoRunnable);
    }


}