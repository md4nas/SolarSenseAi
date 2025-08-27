package com.example.solarsenseapp.network;

import com.example.solarsenseapp.MainActivity;
import com.example.solarsenseapp.utils.Constants;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class WeatherAPI {
    private static final String TAG = "WeatherAPI";

    private final MainActivity activity;

    public interface WeatherCallback {
        void onSuccess(String jsonResponse);
        void onError(String errorMessage);
    }

    public WeatherAPI(MainActivity activity) {
        this.activity = activity;
    }

    public void fetchWeatherData(String location, WeatherCallback callback) {
        new Thread(() -> {
            try {
                String weatherUrl = String.format(
                        "%s?q=%s&units=metric&appid=%s",
                        Constants.WEATHER_API_BASE_URL, location, Constants.WEATHER_API_KEY);

                HttpURLConnection connection = (HttpURLConnection) new URL(weatherUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(Constants.CONNECTION_TIMEOUT);
                connection.setReadTimeout(Constants.READ_TIMEOUT);
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Scanner scanner = new Scanner(connection.getInputStream());
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNext()) {
                        response.append(scanner.nextLine());
                    }
                    scanner.close();

                    callback.onSuccess(response.toString());
                } else {
                    callback.onError("Weather API error: " + connection.getResponseCode());
                }

                connection.disconnect();

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}