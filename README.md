# Copi ✂️

Copi is a cross-device clipboard synchronization app. It allows users to seamlessly copy text from one device and access it instantly on another—perfect for staying productive across mobile and desktop platforms.

## Features

- 🔄 Real-time clipboard sync over local network
- 🔒 Secure WebSocket communication (TLS with optional self-signed certificates)
- 📱 Android client with foreground service to monitor clipboard
- 🖥️ Python-based WebSocket server
- 📷 QR code-based device pairing for easy setup


## Setup Instructions

### Server

1. **Install Dependencies**
   ```bash
   pip install -r requirements.txt

2. **Run server**

    ```bash
    python server.py

### Android App
1. Open the project in Android Studio

2. Build and install the app on your Android device

3. Open the app and scan the QR code shown by the Python server

The app connects to the server and begins clipboard syncing automatically

⚠️ Due to Android's background execution limits, the app uses a foreground service with a persistent notification to ensure clipboard monitoring.
