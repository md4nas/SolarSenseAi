package com.example.solarsenseapp.models;

public class WeatherData {
    public double temperature;
    public int humidity;
    public double windSpeed;
    public double windDirection;
    public String weatherCondition = "Clear";
    public String weatherIcon = "☀️";
    public double rainAmount = 0;
    public double snowAmount = 0;
    public boolean isThunderstorm = false;

    public WeatherData() {
        // Default constructor
    }

    public WeatherData(double temperature, int humidity, double windSpeed,
                       double windDirection, String weatherCondition, String weatherIcon) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.weatherCondition = weatherCondition;
        this.weatherIcon = weatherIcon;
    }

    @Override
    public String toString() {
        return String.format("WeatherData{temp=%.1f°C, humidity=%d%%, wind=%.1fm/s, condition='%s'}",
                temperature, humidity, windSpeed, weatherCondition);
    }
}