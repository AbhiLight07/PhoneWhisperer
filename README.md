# 📱 PhoneWhisperer

> *Your phone learns your life — so you don't have to manage it.*

**CLASH OF THE CLAWS Hackathon — PRISM | Tech Management, RV COLLEGE OF ENGINEERING**

---

## 🧩 Problem

Every day, you manually manage your phone settings dozens of times:
- Silence your phone before a lecture, forget to unsilence after
- Enable Focus Mode for a meeting, disable it when you're done
- Turn on DND at night, forget to turn it off in the morning

Existing solutions like **IFTTT**, **Tasker**, or **Android Focus Modes** require YOU to manually set up every rule — which defeats the purpose of automation. They don't learn. They don't adapt.

**PhoneWhisperer solves this.** It silently observes your behaviour, discovers your patterns automatically using on-device AI, and suggests automation rules for your approval — zero manual setup required.

---

## 💡 Solution

PhoneWhisperer is an Android app that works in three stages:

### 1. Observe (Passive Data Collection)
Runs silently in the background using WorkManager (no battery drain, no foreground service). Collects:
- GPS location (every 15 min, low-accuracy to save battery)
- App usage (via UsageStatsManager)
- DND / ringer mode changes
- Calendar events

All data is stored locally in a Room (SQLite) database. Nothing leaves your device.

### 2. Infer (On-Device AI)
A nightly job runs two AI layers:
- **DBSCAN Clustering** — groups your behaviour events by time + location + app usage to find recurring patterns (e.g., "every weekday 9–11am, you're at college with DND on")
- **Gemma 2B LLM** (on-device via MediaPipe/TFLite) — reads the cluster statistics and generates a human-readable automation rule

### 3. Automate (User-Approved Execution)
Generated rules appear as approval cards in the Jetpack Compose UI. You tap **Approve** or **Reject**. Approved rules execute automatically via Android OS APIs (AlarmManager, AudioManager, NotificationManager).

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Database | Room DB (SQLite) |
| Background Jobs | WorkManager |
| Location | Google FusedLocationProvider |
| AI Clustering | DBSCAN (custom Kotlin implementation) |
| On-Device LLM | Gemma 2B via MediaPipe / TFLite |
| Dependency Injection | Hilt (Dagger) |
| Minimum SDK | Android 8.0 (API 26) |

---

## ⚙️ Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android device or emulator running Android 8.0+ (API 26+)
- JDK 17

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/AbhiLight07/PhoneWhispere.git
   cd PhoneWhisperer
   ```

2. **Open in Android Studio**
   - File → Open → select the cloned folder
   - Wait for Gradle sync to complete

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use Android Studio: Build → Make Project

4. **Run on device/emulator**
   - Connect your Android device via USB (enable Developer Options + USB Debugging)
   - Click the ▶ Run button in Android Studio

5. **Grant required permissions** (on first launch)
   - Location permission → Allow
   - Usage Stats → Go to Settings → Special App Access → Usage Access → Enable PhoneWhisperer
   - DND Access → Allow (for rule execution)

---

## 🚀 Usage

1. **Install and grant permissions** as described above
2. **Let the app observe** — for best results, use your phone normally for 7–14 days. The app runs silently; you won't notice it.
3. **Check the Dashboard** — after patterns are detected (nightly job), open the app to see suggested rules
4. **Approve or Reject rules** — each rule shows what it does and why. Tap Approve to activate.
5. **Your phone manages itself** — approved rules execute automatically from now on

> **Note:** The first rule suggestions appear after approximately 3–7 days of observation, once enough data has been collected for clustering.

---

## 📁 Project Structure

```
com.phonewhisperer/
├── data/
│   ├── db/                    # Room database, DAOs, entities
│   ├── repository/            # Data access layer
│   └── collectors/            # Location, UsageStats, Calendar collectors
├── domain/
│   ├── model/                 # BehaviorPattern, AutomationRule
│   └── usecase/               # Business logic use cases
├── ai_engine/
│   ├── clustering/            # DBSCAN implementation
│   └── llm/                   # Gemma 2B rule generator
├── workers/                   # WorkManager background workers
├── presentation/
│   ├── ui/dashboard/          # Dashboard screen
│   └── ui/rules/              # Rule approval screen
└── PhoneWhispererApp.kt       # Application entry point
```

---

## 📋 Required Permissions

```xml
android.permission.ACCESS_FINE_LOCATION
android.permission.ACCESS_BACKGROUND_LOCATION
android.permission.PACKAGE_USAGE_STATS          <!-- Granted manually in Settings -->
android.permission.ACCESS_NOTIFICATION_POLICY   <!-- For DND control -->
android.permission.RECEIVE_BOOT_COMPLETED       <!-- To restart workers after reboot -->
```

---

## 🔒 Privacy

- **All data stays on your device.** No server, no cloud, no analytics.
- PhoneWhisperer never uploads your location, app usage, or behaviour data anywhere.
- The on-device LLM (Gemma 2B) runs entirely locally — no API calls to external AI services.
- You can delete all collected data from the app settings at any time.

---

## 📽️ Demo

[▶ Watch the Demo Video](https://youtu.be/[your-demo-link])

---

## 🤝 Team

| Name | Role |
|---|---|
| [Abhishek Y S] | Data Layer — Room DB, 3 entities, DAOs, AppDatabase.kt |
| [Bhuvan S G] | Data Collectors — UsageStatsWorker, LocationWorker, AndroidManifest |
| [Shiva] | AI Engine — DBSCAN clustering, Gemma 2B RuleGenerator, domain models |
| [Sushmitha M] | UI — DashboardScreen, RuleApprovalScreen, Theme, build.gradle |

---

## 📄 License

This project was built for the CLASH OF THE CLAWS Hackathon. See [AI_DISCLOSURE.md](AI_DISCLOSURE.md) for details on AI tool usage.
