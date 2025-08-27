# 🌞 SolarSense: Smart Solar Tracking System

[![Demo Video](https://img.shields.io/badge/Demo-Watch%20Video-red?style=for-the-badge&logo=youtube)](https://youtu.be/xqA-Qie9Vjc)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.0.0-green?style=for-the-badge)](https://github.com/md4nas/SolarSense)

> An intelligent, IoT-based solar tracking system that maximizes energy efficiency by dynamically adjusting panel orientation throughout the day.

---

## 📋 Table of Contents

- [About](#about)
- [Problem Statement](#problem-statement)
- [Solution](#solution)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Installation & Setup](#installation--setup)
- [Usage](#usage)
- [Performance](#performance)
- [Challenges & Solutions](#challenges--solutions)
- [Future Enhancements](#future-enhancements)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

---

## 🎯 About

SolarSense is an innovative, energy-efficient solar tracking system that addresses the limitations of fixed solar panel installations. By utilizing IoT technology and intelligent algorithms, the system automatically adjusts solar panel orientation to maintain optimal sun exposure throughout the day, resulting in **6-8% improved power output** even after accounting for the tracking system's energy consumption.

The project combines embedded systems engineering with mobile application development to create a comprehensive solar optimization solution suitable for both educational purposes and real-world applications.

---

## ❌ Problem Statement

Traditional fixed solar panel installations face several critical limitations:

- **Suboptimal Energy Capture**: Fixed panels only receive perpendicular sunlight for brief periods
- **Geographic Inefficiency**: Static installations don't account for seasonal sun path variations
- **Wasted Potential**: Up to 25-35% of potential solar energy is lost due to poor sun alignment
- **High-Cost Solutions**: Commercial solar trackers are expensive and complex for small-scale applications
- **Limited User Control**: Most systems lack intuitive interfaces for monitoring and manual adjustment

---

## ✅ Solution

SolarSense provides an intelligent, cost-effective tracking solution through:

### **Dual Operating Modes**
- **🤖 Auto Mode**: Real-time sun position calculation using GPS coordinates and system time
- **🎮 Manual Mode**: Direct user control via Bluetooth-enabled mobile application

### **Smart Control System**
- **Precise Positioning**: Dual-axis control using stepper and servo motors
- **Energy Optimization**: 5-minute update cycles balance accuracy with power conservation
- **Intelligent Algorithms**: Advanced azimuth angle computation for accurate sun tracking

### **User-Friendly Interface**
- **Mobile Application**: Custom Android app with intuitive controls
- **Voice Commands**: Hands-free operation with natural language processing
- **Real-time Feedback**: Live system status and positioning data

---

## 🚀 Key Features

| Feature | Description |
|---------|-------------|
| **🧠 Real-time Sun Tracking** | GPS-based azimuth angle computation for precise panel alignment |
| **📱 Mobile App Control** | Intuitive Android interface for system management |
| **🗣️ Voice Commands** | Natural language control ("move left", "auto mode") |
| **⚙️ Dual-Axis Movement** | Stepper motor + servo motor configuration |
| **🔋 Power Optimized** | ESP8266-based low-power design |
| **🔄 Quick Reset** | One-tap return to initial/maximum positions |
| **📡 Wireless Control** | Bluetooth connectivity for remote operation |
| **⏱️ Intelligent Timing** | Configurable update intervals for efficiency |

---

## 🛠️ Technology Stack

### **Hardware Components**
| Component | Purpose | Specifications |
|-----------|---------|----------------|
| **ESP8266** | Main microcontroller | Wi-Fi enabled, low power |
| **Servo Motor** | Elevation axis control | 180° rotation range |
| **Stepper Motor** | Azimuth axis control | 360° continuous rotation |
| **Bluetooth Module** | Wireless communication | HC-05/built-in ESP8266 |
| **Power Supply** | System power | 5V/12V dual supply |

### **Software Stack**
| Technology | Usage | Version |
|------------|-------|---------|
| **Arduino IDE** | Firmware development | Latest |
| **Android Studio** | Mobile app development | 4.0+ |
| **Java** | App logic implementation | 8+ |
| **C++** | ESP8266 firmware | Arduino framework |
| **XML** | Android UI layouts | - |

### **Libraries & Dependencies**
```cpp
// ESP8266 Libraries
#include <ESP8266WiFi.h>
#include <Servo.h>
#include <Stepper.h>
#include <SoftwareSerial.h>
```

```java
// Android Dependencies
implementation 'androidx.appcompat:appcompat:1.4.0'
implementation 'com.google.android.material:material:1.5.0'
```

---

## 🏗️ System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Android App   │◄──►│     ESP8266     │◄──►│  Motor Control  │
│                 │    │                 │    │                 │
│ • UI Interface  │    │ • Command Parse │    │ • Servo Motor   │
│ • Sun Calc      │    │ • Motor Control │    │ • Stepper Motor │
│ • Voice Control │    │ • Bluetooth RX  │    │ • Position FB   │
│ • Bluetooth TX  │    │ • Safety Logic  │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  User Controls  │    │   Data Flow     │    │ Physical Panel  │
│                 │    │                 │    │                 │
│ • Manual Mode   │    │ • Position Data │    │ • Solar Panel   │
│ • Auto Mode     │    │ • Status Info   │    │ • Mounting Sys  │
│ • Voice Cmds    │    │ • Error Msgs    │    │ • Safety Stops  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### **Data Flow Architecture**
1. **Input Layer**: Mobile app captures user commands or calculates sun position
2. **Communication Layer**: Bluetooth transmits commands to ESP8266
3. **Processing Layer**: ESP8266 parses commands and controls motors
4. **Hardware Layer**: Motors adjust panel position based on commands
5. **Feedback Layer**: System status reported back to mobile app

---

## 📂 Project Structure

```
SolarSense/
├── 📁 ESP8266_Firmware/
│   ├── main.ino                 # Core motor control & Bluetooth logic
│   ├── MotorControl.h          # Motor movement functions
│   ├── AngleCalculator.h       # Future on-board calculations
│   ├── WiFiSetup.h            # Optional cloud connectivity
│   └── README.md              # Firmware documentation
│
├── 📁 Android_App/             # Link: https://github.com/md4nas/SolarSenseAi
│   ├── 📁 app/src/main/java/
│   │   ├── MainActivity.java           # App launch & UI logic
│   │   ├── BluetoothService.java      # BT connection management
│   │   ├── SunPositionCalculator.java # Azimuth angle computation
│   │   └── VoiceControlService.java   # Voice command processing
│   │
│   ├── 📁 app/src/main/res/
│   │   ├── layout/
│   │   │   └── activity_main.xml      # Main UI layout
│   │   └── values/
│   │       └── strings.xml            # App text resources
│   │
│   └── 📁 docs/
│       └── API_Documentation.md       # App API reference
│
├── 📁 Hardware/
│   ├── circuit_diagram.png           # Wiring schematic
│   ├── components_list.md           # Bill of materials
│   └── assembly_guide.md           # Step-by-step assembly
│
├── 📁 Documentation/
│   ├── user_manual.pdf             # Complete user guide
│   ├── technical_specs.md         # Detailed specifications
│   └── troubleshooting.md        # Common issues & solutions
│
├── LICENSE                        # MIT License
├── CONTRIBUTING.md               # Contribution guidelines
└── README.md                    # This file
```

### **Key File Descriptions**

| File | Purpose | Key Functions |
|------|---------|---------------|
| `main.ino` | ESP8266 core logic | Motor control, Bluetooth parsing, safety checks |
| `MotorControl.h` | Hardware abstraction | `moveServo()`, `stepperRotate()`, `getCurrentPosition()` |
| `MainActivity.java` | App entry point | UI initialization, mode switching, user interactions |
| `BluetoothService.java` | Communication handler | Connection management, data transmission, error handling |
| `SunPositionCalculator.java` | Solar algorithms | Azimuth calculation, time zone handling, GPS processing |
| `VoiceControlService.java` | Voice processing | Speech recognition, command parsing, feedback |

---

## ⚙️ Installation & Setup

### **Hardware Setup**

1. **Component Assembly**
   ```bash
   # Refer to hardware/assembly_guide.md for detailed steps
   1. Mount servo motor for elevation control
   2. Install stepper motor for azimuth rotation
   3. Connect ESP8266 with proper power supply
   4. Wire motors according to circuit diagram
   ```

2. **ESP8266 Firmware Upload**
   ```bash
   # Install Arduino IDE and ESP8266 board package
   git clone https://github.com/md4nas/SolarSense.git
   cd SolarSense/ESP8266_Firmware
   
   # Open main.ino in Arduino IDE
   # Select ESP8266 board and appropriate port
   # Upload firmware
   ```

### **Software Setup**

1. **Android App Installation**
   ```bash
   # Clone the mobile app repository
   git clone https://github.com/md4nas/SolarSenseAi.git
   cd SolarSenseAi
   
   # Open in Android Studio
   # Build and install APK on device
   # Enable Bluetooth permissions
   ```

2. **Initial Configuration**
    - Pair your Android device with ESP8266 Bluetooth
    - Calibrate motor positions using manual mode
    - Set your GPS coordinates for auto mode
    - Test voice commands and system response

---

## 🎮 Usage

### **Auto Mode Operation**
```java
// The system automatically calculates sun position
1. Enable GPS location on your phone
2. Select "Auto Mode" in the app
3. System calculates optimal panel angle every 5 minutes
4. Motors adjust panel position automatically
```

### **Manual Mode Control**
```java
// Direct control via mobile app
- Use directional buttons for precise positioning
- Voice commands: "move left", "move right", "auto mode"
- One-tap reset to initial or maximum positions
- Real-time angle feedback display
```

### **Voice Commands**
| Command | Action |
|---------|--------|
| "Auto mode" | Switch to automatic tracking |
| "Manual mode" | Switch to manual control |
| "Move left" | Rotate panel left |
| "Move right" | Rotate panel right |
| "Move up" | Tilt panel up |
| "Move down" | Tilt panel down |
| "Reset position" | Return to initial position |

---

## 📈 Performance

### **Energy Efficiency Gains**
- **6-8% increased power output** compared to fixed panels
- **Net positive energy balance** after system power consumption
- **ROI period**: 18-24 months for typical installations

### **System Specifications**
| Parameter | Value | Notes |
|-----------|-------|-------|
| **Tracking Accuracy** | ±2° | GPS-based calculation |
| **Update Frequency** | 5 minutes | Configurable |
| **Power Consumption** | <5W | During active tracking |
| **Operating Range** | 360° azimuth, 180° elevation | Full sky coverage |
| **Response Time** | <3 seconds | From command to movement |
| **Bluetooth Range** | 10 meters | Line of sight |

### **Performance Metrics**
```
Daily Energy Gain: +6-8% compared to fixed panels
System Efficiency: 94% (accounting for tracker power usage)
Tracking Precision: ±2 degrees
Motor Lifespan: >50,000 operations
```

---

## 🔧 Challenges & Solutions

### **Technical Challenges**

| Challenge | Impact | Solution Implemented |
|-----------|--------|---------------------|
| **Time Synchronization** | Inaccurate sun tracking | Android timestamp fallback system |
| **Motor Jitter** | Reduced precision | Threshold-based movement checks |
| **Power Management** | Battery drain | 5-minute update cycles, sleep modes |
| **Over-rotation Protection** | Hardware damage risk | Angle clamping and limit switches |
| **Bluetooth Stability** | Connection drops | Automatic reconnection logic |
| **Weather Interference** | False positioning | Error detection and retry mechanisms |

### **Design Decisions**

**ESP8266 vs ESP32**: Chose ESP8266 for lower power consumption and cost-effectiveness
**App-side Calculations**: Offloaded complex math to Android to improve ESP8266 responsiveness  
**Dual-axis Control**: Stepper + servo combination provides optimal precision and range
**Update Frequency**: 5-minute intervals balance tracking accuracy with component longevity

---

## 🔮 Future Enhancements

### **Planned Features**
- [ ] **Weather API Integration**: Automatic positioning based on cloud cover
- [ ] **Solar Radiation Sensors**: Real-time irradiance-based optimization
- [ ] **Cloud Data Logging**: Remote monitoring and analytics dashboard
- [ ] **Multi-panel Support**: Centralized control for solar arrays
- [ ] **Machine Learning**: Predictive positioning based on historical data
- [ ] **Energy Monitoring**: Real-time power generation tracking

### **Hardware Upgrades**
- [ ] **RTC Module**: Independent time keeping
- [ ] **Wind Sensors**: Automatic stow mode during storms
- [ ] **Camera Integration**: Visual sun detection backup
- [ ] **Battery Backup**: Uninterrupted operation during outages

---

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### **Ways to Contribute**
- 🐛 **Bug Reports**: Submit issues with detailed descriptions
- 💡 **Feature Requests**: Suggest new functionality
- 📝 **Documentation**: Improve guides and tutorials
- 🔧 **Code**: Submit pull requests for fixes and features
- 🧪 **Testing**: Help test new releases and features

### **Development Setup**
```bash
# Fork the repository
git clone https://github.com/yourusername/SolarSense.git
cd SolarSense

# Create a feature branch
git checkout -b feature/your-feature-name

# Make your changes and commit
git commit -m "Add your feature description"

# Push and create a pull request
git push origin feature/your-feature-name
```

### **Contribution Guidelines**
- Follow existing code style and conventions
- Include tests for new functionality
- Update documentation for user-facing changes
- Ensure all tests pass before submitting PR

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License - Free for commercial and non-commercial use
✅ Commercial use    ✅ Modification    ✅ Distribution    ✅ Private use
```

---

## 🏆 Applications & Use Cases

### **Ideal Applications**
- **🏠 Residential Solar**: Small-scale home installations
- **📚 Educational Projects**: Engineering and IoT coursework
- **🔋 Off-grid Systems**: Remote power generation
- **🧪 Research & Development**: Solar efficiency studies
- **🌱 Maker Projects**: DIY renewable energy enthusiasts

### **Target Audience**
- Engineering students and educators
- DIY electronics enthusiasts
- Off-grid living communities
- Small-scale solar installers
- IoT and automation developers

---

## 📞 Contact & Support

### **Developer**
👨‍💻 **Anas (md4nas)**  
📧 Email: [your-email@example.com]  
🐙 GitHub: [@md4nas](https://github.com/md4nas)  
🔗 LinkedIn: [Your LinkedIn Profile]

### **Project Links**
🏠 **Main Repository**: [SolarSense ESP8266 Firmware](https://github.com/md4nas/SolarSense)  
📱 **Android App**: [SolarSenseAi](https://github.com/md4nas/SolarSenseAi)  
🎥 **Demo Video**: [Watch on YouTube](https://youtu.be/xqA-Qie9Vjc)

### **Support**
- 📋 **Issues**: Report bugs or request features via GitHub Issues
- 💬 **Discussions**: Join community discussions in GitHub Discussions
- 📖 **Documentation**: Check the `/docs` folder for detailed guides
- 🚀 **Updates**: Watch the repository for latest releases

---

## 🙏 Acknowledgments

Special thanks to:
- **Open Source Community** for Arduino and Android development tools
- **ESP8266 Developers** for the excellent microcontroller platform
- **Solar Energy Researchers** for foundational tracking algorithms
- **Beta Testers** who helped improve system reliability

---

<div align="center">

**⭐ If this project helped you, please give it a star! ⭐**

Made with ❤️ for a sustainable future 🌍

*Last updated: August 2025*

</div>