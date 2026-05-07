# 🤖 AI Disclosure

This document clearly describes how AI tools and models were used in the development of **PhoneWhisperer**, as required by the CLASH OF THE CLAWS Hackathon submission guidelines.

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
- **Why DBSCAN (not K-Means):** DBSCAN doesn't require knowing the number of clusters in advance, handles noise/outliers well, and discovers clusters of arbitrary shape — ideal for irregular daily routines

---

## AI Used DURING Development (Development Tools)

These AI tools were used to help build PhoneWhisperer — they are not part of the shipped app:

### 3. Claude (Anthropic)
- **What it is:** Anthropic's AI assistant (Claude Sonnet)
- **How it was used:**
  - Architecture brainstorming — discussing the observe → cluster → infer → execute pipeline
  - Code scaffolding — generating boilerplate for Room entities, DAOs, WorkManager workers
  - Debugging — identifying issues in Kotlin coroutine scope and WorkManager configuration
  - Documentation — helping write README, AI Disclosure, and code comments
- **Important note:** All generated code was reviewed, understood, and modified by the team. We did not blindly copy-paste code without understanding it.

---

## What AI Was NOT Used For

- No AI was used to generate the DBSCAN algorithm logic — this was implemented and understood by the team
- No AI-generated code was submitted without team review and understanding
- No user data is sent to any external AI API at runtime — all inference is on-device

---

## Summary Table

| AI Tool | Type | Used In Product? | Used In Development? |
|---|---|---|---|
| Gemma 2B (Google) | On-device LLM | ✅ Yes | ❌ No |
| DBSCAN (custom) | ML clustering algorithm | ✅ Yes | ❌ No |
| Claude (Anthropic) | AI assistant | ❌ No | ✅ Yes |

---

*PhoneWhisperer — CLASH OF THE CLAWS Hackathon, PRISM | Tech Management, SRI-B*
