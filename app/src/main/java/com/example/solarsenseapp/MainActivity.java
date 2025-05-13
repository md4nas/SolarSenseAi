package com.example.solarsenseapp;

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
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements LocationListener {

    // Constants
    private static final String TAG = "SolarSenseApp";
    private static final String API_KEY = "4caa58a2c8523065469f2b1e7bd4c050";
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int RECORD_AUDIO_PERMISSION_REQUEST = 101;
    private static final int AUTO_UPDATE_INTERVAL = 300000; // 5 minutes
    private static final String DEFAULT_ESP_IP = "http://192.168.63.219";

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

    // System State
    private String espIp = DEFAULT_ESP_IP;
    private int baseCurrentAngle = 90;
    private int panelCurrentAngle = 90;
    private boolean isAutoMode = false;
    private Location currentLocation;

    // Handlers and Services
    private Handler autoHandler = new Handler();
    private Runnable autoRunnable;
    private LocationManager locationManager;
    private SpeechRecognizer speechRecognizer;
    private Button btnResetLocation;
    private TextToSpeech textToSpeech;

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

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });


    }

    // Initialization Methods
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

        espIpInput.setText(espIp.replace("http://", ""));

        txtVoiceCommands = findViewById(R.id.txtVoiceCommands);
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

    private void speakFeedback(String message) {
        if (textToSpeech != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
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

    // Location Methods
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
                Log.e(TAG, "Security Exception: " + e.getMessage());
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

    // Servo Control Methods
    private void setupSeekBars() {
        baseServoSeekBar.setMax(180);
        baseServoSeekBar.setProgress(baseCurrentAngle);
        baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
        baseServoSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isAutoMode) {
                    // Calculate reversed value (180 - progress)
                    baseCurrentAngle = 180 - progress;
                    baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
                    sendServoCommand("/baseServo?angle=" + baseCurrentAngle);

                    // Update the displayed progress (shows reversed value)
                    seekBar.setProgress(progress);
                }
            }
        });

        // Panel servo remains the same
        panelServoSeekBar.setMax(180);
        panelServoSeekBar.setProgress(panelCurrentAngle);
        panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
        panelServoSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isAutoMode) {
                    panelCurrentAngle = progress;
                    panelServoValue.setText("Panel Servo Angle: " + panelCurrentAngle);
                    sendServoCommand("/panelServo?angle=" + panelCurrentAngle);
                }
            }
        });
    }

    private void setupButtons() {
        // Reverse these button actions
        btnBaseClockwise.setOnClickListener(v -> adjustBaseServo(-40));     // Now increases angle
        btnBaseCounterClockwise.setOnClickListener(v -> adjustBaseServo(40)); // Now decreases angle

        // Keep panel buttons the same
        btnPanelToZero.setOnClickListener(v -> setPanelServo(0));
        btnPanelToMax.setOnClickListener(v -> setPanelServo(180));

        btnUpdateIp.setOnClickListener(v -> {
            String newIp = espIpInput.getText().toString().trim();
            if (!newIp.isEmpty()) {
                espIp = "http://" + newIp;
                Toast.makeText(this, "ESP IP updated to: " + espIp, Toast.LENGTH_SHORT).show();
            }
        });

        //reset location button
        btnResetLocation.setOnClickListener(v -> {
            // Clear location input
            locationInput.setText("");

            // Stop auto tracking if active
            if (isAutoMode) {
                toggleAutoMode.setChecked(false);
                stopAutoMode();
            }

            // Clear weather display
            weatherDataText.setText("");

            // Reset current location
            currentLocation = null;

            // Show confirmation
            Toast.makeText(MainActivity.this, "Location reset", Toast.LENGTH_SHORT).show();
        });
    }

    private void adjustBaseServo(int delta) {
        if (!isAutoMode) {
            // Normal angle calculation
            baseCurrentAngle = Math.max(0, Math.min(180, baseCurrentAngle + delta));

            // Update seekbar with reversed value (180 - angle)
            baseServoSeekBar.setProgress(180 - baseCurrentAngle);
            baseServoValue.setText("Base Servo Angle: " + baseCurrentAngle);
            sendServoCommand("/baseServo?angle=" + baseCurrentAngle);
        }
    }

    private void adjustPanelServo(int delta) {
        if (!isAutoMode) {
            panelCurrentAngle = Math.max(0, Math.min(180, panelCurrentAngle + delta));
            panelServoSeekBar.setProgress(panelCurrentAngle);
            sendServoCommand("/panelServo?angle=" + panelCurrentAngle);
        }
    }

    private void setBaseServo(int angle) {
        if (!isAutoMode) {
            baseCurrentAngle = Math.max(0, Math.min(180, angle));
            baseServoSeekBar.setProgress(baseCurrentAngle);
            sendServoCommand("/baseServo?angle=" + baseCurrentAngle);
        }
    }

    private void setPanelServo(int angle) {
        if (!isAutoMode) {
            panelCurrentAngle = Math.max(0, Math.min(180, angle));
            panelServoSeekBar.setProgress(panelCurrentAngle);
            sendServoCommand("/panelServo?angle=" + panelCurrentAngle);
        }
    }

    // Auto Mode Methods
    private void setupAutoModeToggle() {
        autoRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAutoMode) {
                    updatePanelPosition();
                    autoHandler.postDelayed(this, AUTO_UPDATE_INTERVAL);
                }
            }
        };

        toggleAutoMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAutoMode = isChecked;
            if (isAutoMode) {
                startAutoMode();
            } else {
                stopAutoMode();
            }
        });
    }

    private void startAutoMode() {
        disableManualControls();

        if (currentLocation != null) {
            updatePanelPosition();
            autoHandler.postDelayed(autoRunnable, AUTO_UPDATE_INTERVAL);
            Log.d(TAG, "Auto tracking started with GPS location");
        }
        else if (!locationInput.getText().toString().isEmpty()) {
            new Thread(this::geocodeAndStartTracking).start();
        }
        else {
            showToast("Please enable GPS or enter location");
            toggleAutoMode.setChecked(false);
        }
    }

    public void resetLocation() {
        runOnUiThread(() -> {
            // Clear UI elements
            locationInput.setText("");
            weatherDataText.setText("");

            // Stop auto tracking if active
            if (isAutoMode) {
                toggleAutoMode.setChecked(false);
                stopAutoMode();
            }

            // Reset location data
            currentLocation = null;

            // Provide feedback
            showToast("Location has been reset");

            // Optional: Speak confirmation
            speakFeedback("Location reset complete");
        });
    }


    private void geocodeAndStartTracking() {
        try {
            Geocoder geocoder = new Geocoder(this);
            List<Address> addresses = geocoder.getFromLocationName(
                    locationInput.getText().toString(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                currentLocation = new Location("");
                currentLocation.setLatitude(addresses.get(0).getLatitude());
                currentLocation.setLongitude(addresses.get(0).getLongitude());

                runOnUiThread(() -> {
                    updatePanelPosition();
                    autoHandler.postDelayed(autoRunnable, AUTO_UPDATE_INTERVAL);
                });
                Log.d(TAG, "Auto tracking started with entered location");
            } else {
                showToast("Could not find location");
                toggleAutoMode.setChecked(false);
            }
        } catch (IOException e) {
            showToast("Geocoding error");
            toggleAutoMode.setChecked(false);
        }
    }

    private void stopAutoMode() {
        enableManualControls();
        autoHandler.removeCallbacks(autoRunnable);
        Log.d(TAG, "Auto tracking stopped");
    }

    private void updatePanelPosition() {
        if (currentLocation == null) return;

        double[] solarPosition = calculateSolarPosition(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                new Date()
        );

        // Keep original azimuth calculation (no need to reverse here)
        int baseAngle = (int) Math.round(solarPosition[0] / 2); // Convert 360° to 180°
        int panelAngle = (int) Math.round(solarPosition[1] * 2); // Convert 90° to 180°

        // Clamp to valid servo range
        baseAngle = Math.max(0, Math.min(180, baseAngle));
        panelAngle = Math.max(0, Math.min(180, panelAngle));

        int finalBaseAngle = baseAngle;
        int finalPanelAngle = panelAngle;
        runOnUiThread(() -> {
            // Display reversed value on seekbar
            baseServoSeekBar.setProgress(180 - finalBaseAngle);
            baseServoValue.setText("Base: " + finalBaseAngle + "°");
            panelServoSeekBar.setProgress(finalPanelAngle);
            panelServoValue.setText("Panel: " + finalPanelAngle + "°");
        });

        sendServoCommand("/baseServo?angle=" + baseAngle);
        sendServoCommand("/panelServo?angle=" + panelAngle);
    }

    // Solar Calculation Methods
    private double[] calculateSolarPosition(double lat, double lon, Date date) {
        double latRad = Math.toRadians(lat);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        double hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE)/60.0;

        // Solar declination (δ)
        double declination = 23.45 * Math.sin(Math.toRadians(360.0/365.0 * (dayOfYear - 81)));
        double declRad = Math.toRadians(declination);

        // Equation of time and time correction
        double B = Math.toRadians(360.0/365.0 * (dayOfYear - 81));
        double equationOfTime = 9.87 * Math.sin(2*B) - 7.53 * Math.cos(B) - 1.5 * Math.sin(B);
        double timeCorrection = equationOfTime + 4*(lon - cal.getTimeZone().getRawOffset()/3600000.0*15);
        double solarTime = hour + timeCorrection/60.0;

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

        // Solar altitude (α) - unchanged
        double altitude = Math.toDegrees(Math.asin(
                Math.sin(latRad) * Math.sin(declRad) +
                        Math.cos(latRad) * Math.cos(declRad) * Math.cos(hourAngle)
        ));

        return new double[]{azimuth, altitude};
    }

    // Voice Recognition Methods
    private void setupVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            btnVoice.setEnabled(false);
            showToast("Voice recognition not available on this device");
            return;
        }

        // Initialize voice commands help text
        txtVoiceCommands.setText(
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
                        "- 'Reset location'/'Clear location'/'Location reset' (clear current location)");
        txtVoiceCommands.setVisibility(View.GONE);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}

            @Override
            public void onError(int error) {
                runOnUiThread(() -> {
                    voiceProgressBar.setVisibility(View.GONE);
                    btnVoice.setText("🎤 Voice Command");
                    btnVoice.setBackgroundColor(getColor(android.R.color.holo_purple));
                    showToast("Voice error: " + getErrorText(error));
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
                        handleVoiceCommand(matches.get(0).toLowerCase());
                    }
                });
            }
        });

        btnVoice.setOnClickListener(v -> {
            if (!isAutoMode) {
                voiceProgressBar.setVisibility(View.VISIBLE);
                btnVoice.setText("Listening...");
                btnVoice.setBackgroundColor(getColor(android.R.color.holo_red_dark));
                speechRecognizer.startListening(speechIntent);
            }
        });

        btnShowCommands.setOnClickListener(v -> {
            boolean isVisible = txtVoiceCommands.getVisibility() == View.VISIBLE;
            txtVoiceCommands.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            btnShowCommands.setText(isVisible ? "❓ Voice Commands" : "⬆️ Hide Voice Commands");
        });
    }

    private void handleVoiceCommand(String command) {
        if (command.contains("clockwise")) {
            adjustBaseServo(-40);  // Now increases angle
        } else if (command.contains("counter") || command.contains("anti-clockwise") || command.contains("counter-clockwise")) {
            adjustBaseServo(40); // Now decreases angle
        } else if (command.contains("up") || command.contains("panel up")) {
            adjustPanelServo(40);
        } else if (command.contains("down") || command.contains("panel down")) {
            adjustPanelServo(-40);
        } else if (command.contains("panel zero") || command.contains("panel Angle zero")) {
            setPanelServo(0);
        } else if (command.contains("panel max") || command.contains("panel Angle max")) {
            setPanelServo(180);
        } else if (command.contains("base zero") || command.contains("base Position zero")) {
            setBaseServo(0);
        } else if (command.contains("base max") || command.contains("base Position max")) {
            setBaseServo(180);
        } else if (command.contains("auto mode on") || command.contains("start tracking")) {
            runOnUiThread(() -> toggleAutoMode.setChecked(true));
        } else if (command.contains("auto mode off") || command.contains("stop tracking")) {
            runOnUiThread(() -> toggleAutoMode.setChecked(false));
        } else if (command.contains("weather")) {
            handleWeatherVoiceCommand(command);
        } else if (command.contains("reset location") || command.contains("clear location") || command.contains("location reset")) {
            resetLocation();
        }else {
            extractAndSetAngle(command);
        }
    }

    private void handleWeatherVoiceCommand(String command) {
        String location = extractLocation(command);
        if (location != null && !location.isEmpty()) {
            locationInput.setText(location);
            fetchWeatherData(location);
        } else if (!locationInput.getText().toString().isEmpty()) {
            fetchWeatherData(locationInput.getText().toString());
        }
    }

    private String extractLocation(String command) {
        Pattern pattern = Pattern.compile("(weather|for|in)\\s+(.+?)(\\s+(please|now|today)|$)");
        Matcher matcher = pattern.matcher(command);
        return matcher.find() ? matcher.group(2).trim() : null;
    }

    private void extractAndSetAngle(String command) {
        try {
            Pattern pattern = Pattern.compile("(\\d{1,3})");
            Matcher matcher = pattern.matcher(command);
            if (matcher.find()) {
                int angle = Math.max(0, Math.min(180, Integer.parseInt(matcher.group(1))));

                if (command.contains("base")) {
                    setBaseServo(angle);
                } else if (command.contains("panel")) {
                    setPanelServo(angle);
                } else if (command.contains("angle") || command.contains("set")) {
                    setPanelServo(angle);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing angle", e);
        }
    }

    // Weather Methods
    private void setupWeatherFetching() {
        btnFetchWeather.setOnClickListener(v -> {
            String location = locationInput.getText().toString().trim();
            if (!location.isEmpty()) {
                fetchWeatherData(location);
            } else {
                showToast("Please enter a location");
            }
        });
    }

    private void fetchWeatherData(String location) {
        new Thread(() -> {
            try {
                String weatherUrl = String.format(
                        "https://api.openweathermap.org/data/2.5/weather?q=%s&units=metric&appid=%s",
                        location, API_KEY);

                HttpURLConnection connection = (HttpURLConnection) new URL(weatherUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Scanner scanner = new Scanner(connection.getInputStream());
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNext()) {
                        response.append(scanner.nextLine());
                    }
                    scanner.close();
                    parseWeatherData(response.toString(), location);
                } else {
                    showToast("Weather API error: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                showToast("Weather fetch error: " + e.getMessage());
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

            // Weather condition tracking
            String weatherCondition = "Clear";
            String weatherIcon = "☀️";
            double rainAmount = 0;
            double snowAmount = 0;
            boolean isThunderstorm = false;

            JSONArray weatherArray = jsonObject.getJSONArray("weather");
            for (int i = 0; i < weatherArray.length(); i++) {
                JSONObject weatherObj = weatherArray.getJSONObject(i);
                String mainCondition = weatherObj.getString("main");

                if (mainCondition.equalsIgnoreCase("Rain")) {
                    weatherIcon = "🌧️";
                } else if (mainCondition.equalsIgnoreCase("Snow")) {
                    weatherIcon = "❄️";
                } else if (mainCondition.equalsIgnoreCase("Thunderstorm")) {
                    weatherIcon = "⚡";
                    isThunderstorm = true;
                } else if (mainCondition.equalsIgnoreCase("Clouds")) {
                    weatherIcon = "☁️";
                }
            }

            // Get precipitation amounts
            if (jsonObject.has("rain")) {
                rainAmount = jsonObject.getJSONObject("rain").optDouble("1h", 0);
            }
            if (jsonObject.has("snow")) {
                snowAmount = jsonObject.getJSONObject("snow").optDouble("1h", 0);
            }

            updateWeatherUI(location, weatherIcon, temp, humidity, windSpeed, windDeg,
                    rainAmount, snowAmount, isThunderstorm);

            checkWeatherAlerts(weatherCondition, isThunderstorm);

        } catch (Exception e) {
            showToast("Error parsing weather data: " + e.getMessage());
        }
    }

    private void updateWeatherUI(String location, String weatherIcon, double temp,
                                 int humidity, double windSpeed, double windDeg,
                                 double rainAmount, double snowAmount, boolean isThunderstorm) {
        runOnUiThread(() -> {
            String weatherInfo = String.format(Locale.getDefault(),
                    "%s Weather in %s\n\n" +
                            "🌡️ Temperature: %.1f°C\n" +
                            "💧 Humidity: %d%%\n" +
                            "🌬️ Wind: %.1f m/s %s\n" +
                            "%s Rain: %.1f mm\n" +
                            "%s Snow: %.1f mm\n" +
                            "%s Thunderstorm: %s",
                    weatherIcon, location, temp, humidity,
                    windSpeed, getWindDirectionLabel(windDeg),
                    rainAmount > 0 ? "🌧️" : "  ", rainAmount,
                    snowAmount > 0 ? "❄️" : "  ", snowAmount,
                    isThunderstorm ? "⚡" : "  ", isThunderstorm ? "Yes" : "No");

            weatherDataText.setText(weatherInfo);
        });
    }

    private void checkWeatherAlerts(String weatherCondition, boolean isThunderstorm) {
        if (weatherCondition.equalsIgnoreCase("Rain") ||
                weatherCondition.equalsIgnoreCase("Snow") ||
                weatherCondition.equalsIgnoreCase("Thunderstorm")) {

            showToast("⚠️ " + weatherCondition + " detected!");

            if (isThunderstorm) {
                setPanelServo(0); // Flat position for safety
                showToast("DANGER: Setting panel to flat position");
            }
        }
    }

    private String getWindDirectionLabel(double degrees) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return directions[(int) Math.round(((degrees % 360) / 45)) % 8];
    }

    // Helper Methods
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

    private void sendServoCommand(String endpoint) {
        sendRequest(espIp + endpoint);
    }

    private void sendRequest(final String urlStr) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                final int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    showToast("ESP responded with code: " + responseCode);
                }
            } catch (Exception e) {
                showToast("Network Error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT: return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "RecognitionService busy";
            case SpeechRecognizer.ERROR_SERVER: return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input";
            default: return "Unknown error";
        }
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
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        autoHandler.removeCallbacks(autoRunnable);
    }

    // Helper class to simplify SeekBar listeners
    private abstract class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}