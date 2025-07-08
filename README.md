SolarSense: Smart Solar Tracking System

Video Demo:  https://youtu.be/xqA-Qie9Vjc

Description:

SolarSense is an energy-efficient, IoT-based solar tracking system powered by the ESP8266 microcontroller. Designed to address the inefficiencies of fixed solar panels, SolarSense dynamically adjusts the panel orientation throughout the day to maintain a perpendicular angle to the sun, optimizing sunlight capture and improving power output by 6–8% even after accounting for the tracking system's own power consumption.

The project comprises two major components:

A hardware control system using ESP8266, servo motors, and stepper motors.

A custom Android application for user interface and manual control.

Project Objective

The main goal of this project is to create a low-cost and energy-efficient solar tracking solution that can increase energy yield using basic microcontrollers and motors. It should be capable of operating in both manual and auto modes:

In Auto Mode, the system calculates the sun’s position using azimuth angle formulas based on GPS coordinates and time.

In Manual Mode, the user can directly control the panel orientation using a Bluetooth-enabled Android app.

Project Repositories

ESP8266 Firmware + Hardware Control:https://github.com/md4nas/SolarSenseEsp8266

Android Application & Sun Position Logic:https://github.com/md4nas/SolarSenseAi

Key Features

🧠 Real-time Sun Tracking using azimuth angle

📱 Android App with Manual & Auto Mode toggle

🗣️ Voice command support for hands-free control

⚙️ Servo + Stepper Motor control for dual-axis movement

🔋 Low-power optimization of ESP8266 & peripherals

🔄 Initial and Max Position buttons for reset

File Structure and Functionality

SolarSenseEsp8266 Repository

main.ino — Core firmware for ESP8266 that reads data from the app and drives the servo and stepper motors.

WiFiSetup.h — Stores Wi-Fi configuration details (if cloud extension is added).

MotorControl.h — Contains functions for moving servos and stepper motors.

AngleCalculator.h — Placeholder to integrate sun position formula if logic is moved to hardware in future.

SolarSenseAi Repository

MainActivity.java — Android main activity managing the UI, Bluetooth service, and button click events.

BluetoothService.java — Responsible for establishing and maintaining a Bluetooth connection.

SunPositionCalculator.java — Computes azimuth angle based on user’s location and current time.

VoiceControlService.java — Handles basic voice commands like "move left", "auto mode", etc.

activity_main.xml — Layout XML defining the Android app interface.

Design Decisions

ESP8266 was chosen over ESP32 to keep costs low and reduce power usage. It also simplifies the Bluetooth setup using HC-05 modules.

Manual vs Auto Mode: Users may prefer direct control for experimentation or testing; hence, manual override was necessary.

Low Update Frequency: In Auto Mode, azimuth angle is updated every 5 minutes to reduce motor wear and power draw.

Decentralized Logic: The Android app performs sun position calculations to offload computation from ESP8266.

Challenges & Optimizations

Accurately syncing time without an RTC or internet required creative fallback logic.

Servo jittering was reduced by applying conditional movement only when deviation exceeded a certain angle threshold.

Motor position tracking required careful calibration to avoid panel over-rotation.

Final Thoughts

SolarSense is a compact, affordable solution for real-time solar panel tracking, perfect for educational projects, off-grid systems, or makers experimenting with green tech. With further enhancements like weather API integration, battery monitoring, or solar radiation sensors, this project could be production-grade.

Developed by: Anas (md4nas)
