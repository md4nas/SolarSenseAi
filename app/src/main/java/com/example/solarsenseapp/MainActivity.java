package com.example.solarsenseapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.graphics.Color;
import android.content.pm.PackageManager;
import android.widget.EditText;


import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {

    private EditText locationInput;
    private Button btnFetchWeather;
    private TextView weatherDataText;

    private final String API_KEY = "4caa58a2c8523065469f2b1e7bd4c050"; // open Wheteher API

    private SeekBar baseServoSeekBar, panelServoSeekBar;
    private TextView baseServoValue, panelServoValue;
    private Button btnBaseClockwise, btnBaseCounterClockwise;
    private Button btnPanelToZero, btnPanelToMax, btnVoice;
    private ProgressBar voiceProgressBar;

    private final String espIp = "http://192.168.63.219"; // Replace with your ESP8266 IP

    private int baseCurrentAngle = 90;
    private int panelCurrentAngle = 90;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request microphone permission
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
        }

        // Initialize UI
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

        // Base Servo SeekBar
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

        // Panel Servo SeekBar
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

        // Base Rotation Buttons
        btnBaseClockwise.setOnClickListener(v -> {
            baseCurrentAngle = Math.min(baseCurrentAngle + 15, 180);
            baseServoSeekBar.setProgress(baseCurrentAngle);
            sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
        });

        btnBaseCounterClockwise.setOnClickListener(v -> {
            baseCurrentAngle = Math.max(baseCurrentAngle - 15, 0);
            baseServoSeekBar.setProgress(baseCurrentAngle);
            sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
        });

        // Panel to Zero and Max
        btnPanelToZero.setOnClickListener(v -> {
            panelCurrentAngle = 0;
            panelServoSeekBar.setProgress(panelCurrentAngle);
            sendRequest(espIp + "/panelServo?angle=0");
        });

        btnPanelToMax.setOnClickListener(v -> {
            panelCurrentAngle = 180;
            panelServoSeekBar.setProgress(panelCurrentAngle);
            sendRequest(espIp + "/panelServo?angle=180");
        });

        // === VOICE CONTROL ===
        SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        if (recognizer == null) {
            Toast.makeText(this, "Speech recognition not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }

        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onResults(Bundle results) {
                voiceProgressBar.setVisibility(View.GONE);
                btnVoice.setText("🎤 Voice Command");
                btnVoice.setBackgroundColor(Color.parseColor("#6200EE"));

                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    String command = matches.get(0).toLowerCase();
                    handleVoiceCommand(command);
                }
            }

            @Override public void onError(int error) {
                voiceProgressBar.setVisibility(View.GONE);
                btnVoice.setText("🎤 Voice Command");
                btnVoice.setBackgroundColor(Color.parseColor("#6200EE"));
                Toast.makeText(MainActivity.this, "Voice error. Try again.", Toast.LENGTH_SHORT).show();
            }

            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        btnVoice.setOnClickListener(v -> {
            voiceProgressBar.setVisibility(View.VISIBLE);
            btnVoice.setText("Listening...");
            btnVoice.setBackgroundColor(Color.RED);
            recognizer.startListening(speechIntent);
        });

        Button btnShowCommands = findViewById(R.id.btnShowCommands);
        TextView txtVoiceCommands = findViewById(R.id.txtVoiceCommands);

        btnShowCommands.setOnClickListener(v -> {
            if (txtVoiceCommands.getVisibility() == View.GONE) {
                txtVoiceCommands.setVisibility(View.VISIBLE);
                btnShowCommands.setText("⬆️ Hide Voice Commands");
            } else {
                txtVoiceCommands.setVisibility(View.GONE);
                btnShowCommands.setText("❓ Voice Commands");
            }
        });

        locationInput = findViewById(R.id.locationInput);
        btnFetchWeather = findViewById(R.id.btnFetchWeather);
        weatherDataText = findViewById(R.id.weatherDataText);

        btnFetchWeather.setOnClickListener(v -> {
            String location = locationInput.getText().toString().trim();
            if (!location.isEmpty()) {
                fetchWeatherData(location);
            } else {
                Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void sendRequest(String urlStr) {
        new Thread(() -> {
            try {
                System.out.println("Sending request: " + urlStr);  // Debugging line
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    showErrorToast("Failed to connect to ESP!");
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                showErrorToast("Network Error: " + e.getMessage());
            }
        }).start();
    }


    private void showErrorToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void handleVoiceCommand(String command) {
        command = command.toLowerCase();

        if (command.contains("clockwise")) {
            baseCurrentAngle = Math.min(baseCurrentAngle + 100, 180);
            baseServoSeekBar.setProgress(baseCurrentAngle);
            sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
        } else if (command.contains("counter")) {
            baseCurrentAngle = Math.max(baseCurrentAngle - 100, 0);
            baseServoSeekBar.setProgress(baseCurrentAngle);
            sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
        } else if (command.contains("panel zero") || command.contains("panel angle zero") || command.contains("panel angle 0")|| command.contains("panel 0")) {
            panelCurrentAngle = 0;
            panelServoSeekBar.setProgress(panelCurrentAngle);
            sendRequest(espIp + "/panelServo?angle=0");
        } else if (command.contains("panel max") || command.contains("panel angle max")) {
            panelCurrentAngle = 180;
            panelServoSeekBar.setProgress(panelCurrentAngle);
            sendRequest(espIp + "/panelServo?angle=180");
        }
        else if (command.contains("panel to") || command.contains("panel angle")) {
            try {
                int angle = Integer.parseInt(command.replaceAll("\\D+", ""));
                if (angle >= 0 && angle <= 180) {
                    panelCurrentAngle = angle;
                    panelServoSeekBar.setProgress(panelCurrentAngle);
                    sendRequest(espIp + "/panelServo?angle=" + panelCurrentAngle);
                }
            } catch (Exception ignored) {}
        }
        else if (command.contains("base to") || command.contains("base angle")) {
            try {
                int angle = Integer.parseInt(command.replaceAll("\\D+", ""));
                if (angle >= 0 && angle <= 180) {
                    baseCurrentAngle = angle;
                    baseServoSeekBar.setProgress(baseCurrentAngle);
                    sendRequest(espIp + "/baseServo?angle=" + baseCurrentAngle);
                }
            } catch (Exception ignored) {}
        } else {
            Toast.makeText(this, "Unknown command: " + command, Toast.LENGTH_SHORT).show();
        }
    }


    private void fetchWeatherData(String location) {
        new Thread(() -> {
            try {
                String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + location +
                        "&units=metric&appid=" + API_KEY;

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    showErrorToast("Error fetching weather: " + responseCode);
                    return;
                }

                Scanner sc = new Scanner(conn.getInputStream());
                StringBuilder response = new StringBuilder();
                while (sc.hasNext()) {
                    response.append(sc.nextLine());
                }

                JSONObject jsonObject = new JSONObject(response.toString());

                // Main weather data
                JSONObject main = jsonObject.getJSONObject("main");
                double temp = main.getDouble("temp");
                int humidity = main.getInt("humidity");

                // Wind data
                JSONObject wind = jsonObject.getJSONObject("wind");
                double windSpeed = wind.getDouble("speed");
                double windDeg = wind.has("deg") ? wind.getDouble("deg") : 0;
                String windDirectionLabel = getWindDirectionLabel(windDeg);

                // Optional rain
                String rain = "N/A";
                if (jsonObject.has("rain")) {
                    JSONObject rainObj = jsonObject.getJSONObject("rain");
                    rain = rainObj.has("1h") ? rainObj.getDouble("1h") + " mm" : "N/A";
                }

                // Optional snow
                String snow = "N/A";
                if (jsonObject.has("snow")) {
                    JSONObject snowObj = jsonObject.getJSONObject("snow");
                    snow = snowObj.has("1h") ? snowObj.getDouble("1h") + " mm" : "N/A";
                }

                // Weather conditions
                String thunderstorm = "No";
                String conditionName = "";
                JSONArray weatherArray = jsonObject.getJSONArray("weather");
                for (int i = 0; i < weatherArray.length(); i++) {
                    conditionName = weatherArray.getJSONObject(i).getString("main");
                    if (conditionName.toLowerCase().contains("thunderstorm")) {
                        thunderstorm = "Yes";
                        break;
                    }
                }

                // Final formatted string
                String weatherInfo = "📍 Location: " + location + "\n" +
                        "🌤️ Condition: " + conditionName + "\n" +
                        "🌡️ Temp: " + temp + "°C\n" +
                        "💧 Humidity: " + humidity + "%\n" +
                        "🌬️ Wind Speed: " + windSpeed + " m/s\n" +
                        "🧭 Wind Direction: " + windDeg + "° (" + windDirectionLabel + ")\n" +
                        "🌧️ Rain: " + rain + "\n" +
                        "❄️ Snow: " + snow + "\n" +
                        "⚡ Thunderstorm: " + thunderstorm;

                runOnUiThread(() -> weatherDataText.setText(weatherInfo));

            } catch (Exception e) {
                e.printStackTrace();
                showErrorToast("Exception: " + e.getMessage());
            }
        }).start();
    }

    // Helper method to convert degree to direction
    private String getWindDirectionLabel(double degrees) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return directions[(int) Math.round(((degrees % 360) / 45)) % 8];
    }

}
