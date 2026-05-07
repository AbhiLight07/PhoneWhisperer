# 🤖 AI Disclosure

This document clearly describes how AI tools and models were used in the development .

---

## AI Used IN the Product (Part of the App)

These AI models are embedded in PhoneWhisperer and run on the user's device:

### 1. Gemma 2B — On-Device LLM
- **What it is:** Google's open-source large language model, 2 billion parameters
- **How it's used:** After DBSCAN clustering identifies a behavioural pattern, Gemma 2B reads the cluster statistics and generates a plain-English automation rule (e.g., "Enable DND on weekdays 9–11am when at your college location")
- **Where it runs:** Entirely on the user's Android device via MediaPipe / TFLite — no data sent to any server
- **Why this model:** Small enough to run on mid-range Android devices, capable enough to produce readable rule descriptions

### 2. DBSCAN (Density-Based Spatial Clustering of Applications with Noise)
- **What it is:** An unsupervised machine learning clustering algorithm
- **How it's used:** Groups behavioural events (GPS coordinates × time-of-day × day-of-week × app usage) into clusters that represent recurring patterns in the user's life
- **Implementation:** Custom Kotlin implementation built from scratch for this project — not a third-party library
- **Why DBSCAN (not K-Means):** Does not require knowing the number of clusters in advance, handles noise/outliers well, and discovers clusters of arbitrary shape — ideal for irregular daily routines

---

## AI Tools Used DURING Development (Development Tools)

These AI tools were used to help build PhoneWhisperer — they are **not** part of the shipped app:

### 3. Claude Opus (Anthropic)
- **What it is:** Anthropic's most capable AI assistant model
- **How it was used:**
  - Designing the overall app architecture (observe → cluster → infer → execute pipeline)
  - Generating boilerplate Kotlin code for Room entities, DAOs, and WorkManager workers
  - Reviewing and debugging coroutine scope and WorkManager configuration issues
  - Writing documentation including README and AI Disclosure

### 4. ChatGPT (OpenAI)
- **What it is:** OpenAI's conversational AI assistant
- **How it was used:**
  - Brainstorming feature ideas and user flow design
  - Explaining Android concepts (UsageStatsManager, FusedLocationProvider)
  - Helping debug Gradle build errors and dependency conflicts
  - Reviewing Kotlin code logic and suggesting improvements

### 5. Gemini (Google)
- **What it is:** Google's multimodal AI assistant
- **How it was used:**
  - Research on DBSCAN algorithm parameters (epsilon and minPts tuning)
  - Understanding MediaPipe and TFLite integration for on-device LLM
  - Exploring Gemma 2B model capabilities and limitations
  - Assistance with Jetpack Compose UI component design

### 6. AntiGravity
- **What it is:** AI-powered development assistant tool
- **How it was used:**
  - Code scaffolding and project structure setup
  - Generating initial implementations for data layer components
  - Assisting with Android-specific boilerplate and best practices

---




