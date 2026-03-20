# Arirang

Arirang is a powerful Xposed module for Android designed to enhance user privacy by providing fine-grained control over sensitive system information and hooks. It allows you to mock device identifiers, location, SIM information, and manage app visibility.

## 🚀 Features

- **SIM Mocking**: Customize or hide SIM card details (IMSI, operator info, etc.) from apps.
- **Location Spoofing**: Provide mock GPS coordinates to specific applications.
- **Clipboard Protection**: Monitor and intercept clipboard access requests with real-time confirmation dialogs.
- **Package List Management**: Hide specific installed applications from being detected by other apps (Invisible/Whitelist modes).
- **Device Info Masking**: Modify hardware identifiers and system properties.
- **Real-time Notifications**: Get notified or prompted when apps attempt to access sensitive data via the `HookNotifyService`.
- **Modern UI**: Built with Material Design 3 and Dynamic Colors support.
- **Multi-language Support**: Easily switch between supported languages.

## 🛠 Requirements

- A rooted Android device.
- **LSPosed** or equivalent Xposed Framework installed.
- Android 9.0+ (API 28+) recommended.

## 📦 Installation

1. Download and install the latest `Arirang` APK.
2. Open your Xposed Manager (e.g., LSPosed).
3. Enable the **Arirang** module.
4. (Optional) Select the scope of applications you wish to hook.
5. Reboot your device or restart the target applications.

## ⚙️ Configuration

Open the Arirang app from your launcher to configure global settings and specific hooks:
- **SIM Config**: Manage SIM-related hooks and whitelist/blacklist apps.
- **Location Config**: Set your preferred mock coordinates.
- **Package List**: Choose which apps should be hidden or visible.
- **Device Info**: Spoof model, manufacturer, and other hardware IDs.

## 🛡 Disclaimer

This project is for **testing and educational purposes only**. Use it responsibly. The developers are not responsible for any misuse or damage caused by this software.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](link-to-your-issues).