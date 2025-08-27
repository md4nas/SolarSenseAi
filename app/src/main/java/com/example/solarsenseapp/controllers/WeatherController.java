package com.example.solarsenseapp.controllers;

import android.widget.EditText;
import android.widget.TextView;

import com.example.solarsenseapp.MainActivity;
import com.example.solarsenseapp.controllers.ServoController;
import com.example.solarsenseapp.network.WeatherAPI;
import com.example.solarsenseapp.models.WeatherData;
import com.example.solarsenseapp.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherController {
    private static final String TAG = "WeatherController";

    private final MainActivity activity;
    private final TextView weatherDataText;
    private final ServoController servoController;
    private final WeatherAPI weatherAPI;

    public WeatherController(MainActivity activity, TextView weatherDataText,
                             ServoController servoController) {
        this.activity = activity;
        this.weatherDataText = weatherDataText;
        this.servoController = servoController;
        this.weatherAPI = new WeatherAPI(activity);
    }

    public void fetchWeatherData(String location) {
        weatherAPI.fetchWeatherData(location, new WeatherAPI.WeatherCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                parseWeatherData(jsonResponse, location);
            }

            @Override
            public void onError(String errorMessage) {
                activity.showToast("Weather fetch error: " + errorMessage);
            }
        });
    }

    public void handleWeatherVoiceCommand(String command, EditText locationInput) {
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

    private void parseWeatherData(String jsonResponse, String location) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            WeatherData weatherData = parseWeatherFromJson(jsonObject);

            updateWeatherUI(location, weatherData);
            checkWeatherAlerts(weatherData);

        } catch (Exception e) {
            activity.showToast("Error parsing weather data: " + e.getMessage());
        }
    }

    private WeatherData parseWeatherFromJson(JSONObject jsonObject) throws Exception {
        WeatherData weatherData = new WeatherData();

        // Main weather data
        JSONObject main = jsonObject.getJSONObject("main");
        weatherData.temperature = main.getDouble("temp");
        weatherData.humidity = main.getInt("humidity");

        // Wind data
        JSONObject wind = jsonObject.getJSONObject("wind");
        weatherData.windSpeed = wind.getDouble("speed");
        weatherData.windDirection = wind.has("deg") ? wind.getDouble("deg") : 0;

        // Weather conditions
        JSONArray weatherArray = jsonObject.getJSONArray("weather");
        for (int i = 0; i < weatherArray.length(); i++) {
            JSONObject weatherObj = weatherArray.getJSONObject(i);
            String mainCondition = weatherObj.getString("main");

            if (mainCondition.equalsIgnoreCase("Rain")) {
                weatherData.weatherIcon = "🌧️";
                weatherData.weatherCondition = "Rain";
            } else if (mainCondition.equalsIgnoreCase("Snow")) {
                weatherData.weatherIcon = "❄️";
                weatherData.weatherCondition = "Snow";
            } else if (mainCondition.equalsIgnoreCase("Thunderstorm")) {
                weatherData.weatherIcon = "⚡";
                weatherData.weatherCondition = "Thunderstorm";
                weatherData.isThunderstorm = true;
            } else if (mainCondition.equalsIgnoreCase("Clouds")) {
                weatherData.weatherIcon = "☁️";
                weatherData.weatherCondition = "Clouds";
            } else {
                weatherData.weatherIcon = "☀️";
                weatherData.weatherCondition = "Clear";
            }
        }

        // Precipitation data
        if (jsonObject.has("rain")) {
            weatherData.rainAmount = jsonObject.getJSONObject("rain").optDouble("1h", 0);
        }
        if (jsonObject.has("snow")) {
            weatherData.snowAmount = jsonObject.getJSONObject("snow").optDouble("1h", 0);
        }

        return weatherData;
    }

    private void updateWeatherUI(String location, WeatherData weatherData) {
        activity.runOnUiThread(() -> {
            String weatherInfo = String.format(Locale.getDefault(),
                    "%s Weather in %s\n\n" +
                            "🌡️ Temperature: %.1f°C\n" +
                            "💧 Humidity: %d%%\n" +
                            "🌬️ Wind: %.1f m/s %s\n" +
                            "%s Rain: %.1f mm\n" +
                            "%s Snow: %.1f mm\n" +
                            "%s Thunderstorm: %s",
                    weatherData.weatherIcon, location, weatherData.temperature, weatherData.humidity,
                    weatherData.windSpeed, getWindDirectionLabel(weatherData.windDirection),
                    weatherData.rainAmount > 0 ? "🌧️" : "  ", weatherData.rainAmount,
                    weatherData.snowAmount > 0 ? "❄️" : "  ", weatherData.snowAmount,
                    weatherData.isThunderstorm ? "⚡" : "  ", weatherData.isThunderstorm ? "Yes" : "No");

            weatherDataText.setText(weatherInfo);
        });
    }

    private void checkWeatherAlerts(WeatherData weatherData) {
        if (weatherData.weatherCondition.equalsIgnoreCase("Rain") ||
                weatherData.weatherCondition.equalsIgnoreCase("Snow") ||
                weatherData.weatherCondition.equalsIgnoreCase("Thunderstorm")) {

            activity.showToast("⚠️ " + weatherData.weatherCondition + " detected!");

            if (weatherData.isThunderstorm) {
                servoController.setPanelServoAuto(Constants.SERVO_MIN_ANGLE); // Flat position for safety
                activity.showToast("DANGER: Setting panel to flat position");
            }
        }
    }

    private String getWindDirectionLabel(double degrees) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return directions[(int) Math.round(((degrees % 360) / 45)) % 8];
    }
}