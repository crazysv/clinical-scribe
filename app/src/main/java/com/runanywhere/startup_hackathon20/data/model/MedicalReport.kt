package com.runanywhere.startup_hackathon20.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MedicalReport(
    val patientName: String,
    val symptoms: List<String>,
    val vitals: Map<String, String>,
    val diagnosis: String,
    val icd10Code: String = "Pending",
    val treatmentPlan: List<String>,
    val safetyWarning: String? = null,
    val visualEvidence: String? = null,
    // --- NEW FIELD ---
    val riskScore: Int = 0 // 0 to 100
)