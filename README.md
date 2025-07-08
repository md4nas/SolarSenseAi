🌞 SolarSense: Smart Solar Tracking System

🎥 Video Demo: Watch Here

🧾 Description

SolarSense is an energy-efficient, IoT-based solar tracking system powered by the ESP8266 microcontroller. It addresses the inefficiencies of fixed solar panels by dynamically adjusting panel orientation to keep it perpendicular to the sun throughout the day, improving power output by 6–8%, even after factoring in the tracking system's own energy consumption.

This project is divided into two key components:

🔧 A hardware control system using ESP8266, servo motors, and stepper motors.

📱 A custom Android application for user interaction and manual control.

🎯 Project Objective

The objective of SolarSense is to provide a low-cost, low-power solar tracking solution using common microcontrollers and motors. It operates in two intelligent modes:

🔁 Auto Mode – Calculates real-time azimuth angles using GPS coordinates and system time to align the panel.

🎮 Manual Mode – Offers direct user control via Bluetooth and a mobile app.

📂 Project Repositories

🔌 ESP8266 Firmware + Hardware Control:github.com/md4nas/SolarSenseEsp8266

📲 Android Application & Sun Position Logic:github.com/md4nas/SolarSenseAi

🚀 Key Features

🧠 Real-time sun tracking via azimuth angle computation

📱 Android app with intuitive UI for manual and auto modes

🗣️ Voice command support (e.g., "move left", "auto mode")

⚙️ Dual-axis control using stepper + servo motors

🔋 Highly optimized for low power using ESP8266

🔄 One-tap reset to Initial and Max angles

🧱 File Structure and Functionality

📁 SolarSenseEsp8266

main.ino – Core logic for motor control and Bluetooth command parsing

WiFiSetup.h – (Optional) Wi-Fi credentials if future cloud support is added

MotorControl.h – Functions for controlling servo and stepper motors

AngleCalculator.h – Reserved for future on-board azimuth calculations

📁 SolarSenseAi

MainActivity.java – App launch logic and UI interaction

BluetoothService.java – Handles stable Bluetooth connection and data transfer

SunPositionCalculator.java – Computes sun position using azimuth angle formulas

VoiceControlService.java – Enables basic voice-based commands

activity_main.xml – XML layout for user-friendly interface

🛠️ Design Decisions

✅ ESP8266 was selected over ESP32 for its cost-effectiveness and lower power draw

✅ Manual override mode was included to allow precise user experimentation

✅ 5-minute update cycle balances tracking accuracy and power/motor conservation

✅ App-side azimuth calculation offloads math from ESP8266 to improve responsiveness

⚙️ Challenges & Optimizations

🕒 Handling time sync without RTC or internet using Android timestamp fallback

🔧 Reducing servo jitter with threshold-based movement checks

🎯 Preventing panel over-rotation via motor calibration and angle clamping

💬 Final Thoughts

SolarSense is a compact, scalable, and budget-friendly solar tracker built for real-world applications. It’s ideal for:

🔋 Off-grid use cases

📚 Engineering/IoT education

🌱 Makers interested in clean tech and automation

Future upgrades may include weather APIs, solar radiation sensors, or remote cloud logging.

👨‍💻 Developed by: Anas (md4nas)
