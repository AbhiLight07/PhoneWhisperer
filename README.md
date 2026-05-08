<div align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/MediaPipe-00B0FF?style=for-the-badge&logo=google&logoColor=white" />
  
  <h1>🤖 PhoneWhisperer</h1>
  <p><b>A privacy-first, fully on-device AI agent that learns your behavioral routines and automates your phone.</b></p>
</div>

---

## 🌟 The Vision

Modern smartphones are smart, but they don't *learn*. You still manually put your phone on silent at work, manually turn on DND when sleeping, and manually clear distracting notifications when studying. 

**PhoneWhisperer** runs silently in the background, observing your behavior (app usage, screen time, locations, ringer modes). Using on-device machine learning (DBSCAN clustering), it finds your hidden routines and uses a **3-Tier LLM Pipeline** to suggest intelligent automation rules.

> **"Auto-mute your phone during college hours. Detected from your daily pattern."**

## ✨ Key Features

- 🕵️ **Silent Observation:** Tracks App Usage (120+ apps categorized), Screen States, Locations, and Notifications with zero battery drain using Android `WorkManager`.
- 🧠 **On-Device DBSCAN Clustering:** Analyzes data across time (cyclic sin/cos temporal encoding) and space (Haversine distance) to find clusters of repetitive behavior.
- 💬 **3-Tier AI Rule Generation:** Automatically turns raw data into natural language rules using Google's Gemma 2B or Gemini Cloud API.
- 🛡️ **Notification Interceptor:** Automatically auto-dismisses notifications from specific apps based on your AI-generated focus rules.
- 🔒 **100% Privacy Preserving:** Raw GPS coordinates and sensitive data **never** leave the device. Everything is converted to abstract semantic labels (e.g., `HOME`, `WORK`, `SOCIAL`).

## 🏗️ The 3-Tier LLM Architecture

To ensure the app never crashes due to network failures or missing API keys, we built a robust fallback pipeline:

1. **Tier 1: MediaPipe + Gemma 2B (Fully Offline)**
   If the user has the 1.3GB Gemma model installed locally, the app runs 100% offline, air-gapped inference using MediaPipe.
2. **Tier 2: Gemini 2.0 Flash (Cloud API)**
   If the local model isn't found, the app securely queries the Gemini API using anonymized metadata (no raw GPS/package names).
3. **Tier 3: Heuristic Engine**
   If there is no internet and no local model, the app instantly generates rules using an onboard heuristic template engine.

## 🚀 Quick Install (For Evaluators)

1. Go to the **[Releases](../../releases)** page and download the latest `app-debug.apk`.
2. Install the APK on any Android 9.0+ device (accept the Google Play Protect warning if prompted).
3. Open the app and grant the requested permissions on the Onboarding Screen (Usage Access, Notification Access, Location).
4. **To Demo Instantly:** Tap the word "PhoneWhisperer" at the top of the Dashboard. This injects mock behavioral data and instantly triggers the AI pattern detection.

## 📦 Enabling "Fully Offline Mode" (Gemma 2B)

For maximum privacy, PhoneWhisperer can run entirely without internet.
1. Download the **Gemma 2B INT4 GPU** model from [Kaggle](https://www.kaggle.com/models/google/gemma/tfLite/gemma-2b-it-gpu-int4).
2. Extract the `.bin` file.
3. Rename it to `gemma-2b-it-gpu-int4.bin` (if it isn't already).
4. Place it directly in your Android phone's **Download** folder (`/sdcard/Download/`).
5. Open the app's Settings tab. The AI Engine status will instantly detect the model and switch to **"🔒 On-Device Gemma 2B Active"**.

## 💻 Setup for Developers

1. Clone the repository.
2. Get a free Gemini API Key from [Google AI Studio](https://aistudio.google.com/).
3. Create a `local.properties` file in the project root (this file is git-ignored for safety).
4. Add your key: `GEMINI_API_KEY=your_api_key_here`
5. Open the project in Android Studio Ladybug (or newer) and hit **Run**.

## 🛠️ Tech Stack

- **Architecture:** Clean Architecture, MVVM, Repository Pattern
- **UI:** Jetpack Compose, Material 3
- **Local Database:** Room Database (Reactive flows)
- **Dependency Injection:** Dagger Hilt
- **Background Work:** WorkManager, NotificationListenerService, UsageStatsManager
- **AI Models:** MediaPipe GenAI (Gemma), Google Cloud Generative AI SDK
- **Algorithms:** Custom DBSCAN implementation with cyclic temporal weighting
---
[▶ Watch the Demo Video]([https://youtu.be/8WLLRvkJbv4])
---
<p align="center"><i>Built with ☕ for the Hackathon</i></p>
---
