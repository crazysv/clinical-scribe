# 🏥 Clinical Scribe

**An Offline-First Expert System for Remote Healthcare**

Clinical Scribe is a mobile application designed to function in zero-connectivity environments (disaster zones, aviation, remote clinics). It utilizes on-device AI to convert unstructured natural language (voice/text) into rigorous, structured clinical data (JSON).

## 🚀 Key Features

* **🗣️ Voice-to-JSON Engine:** Converts rambling medical dictation into structured reports (Vitals, Diagnosis, Billing Codes).
* **✈️ 100% Offline Capability:** Functions without internet access using local processing.
* **🧠 AI Assistant (RAG):** "Chat with your data" feature allows doctors to query long reports for specific symptoms (e.g., "Did the patient mention dizziness?").
* **📸 Multimodal Analysis:** Captures images of injuries/medications and auto-populates the clinical report.
* **🔗 Peer-to-Peer Sharing:** Generates QR codes to transfer full patient records to other devices without a network.
* **🌍 Multi-Language Support:** Translates reports and reads them aloud for patient understanding.

## 📱 How It Works

1.  **Input:** Doctor dictates patient status via microphone.
2.  **Process:** The app parses speech using the "Structured Output" SDK feature.
3.  **Output:** A validated JSON object is created, rendering a UI with calculated Triage Scores.
4.  **Action:** Data can be exported as PDF or transferred via QR code.

## 🛠️ Tech Stack

* **Platform:** Android (Kotlin)
* **AI/LLM:**
* **Architecture:** MVVM
* **Data Format:** JSON Standardized Medical Records

---
*Built for The Claude Challenge 2025.*
