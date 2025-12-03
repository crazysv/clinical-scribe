package com.runanywhere.startup_hackathon20

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ScribeViewModel(application: Application) : AndroidViewModel(application) {

    // --- UI STATES ---
    private val _report = MutableStateFlow<MedicalReport?>(null)
    val report: StateFlow<MedicalReport?> = _report.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _targetLanguage = MutableStateFlow("English")
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    private val _historyList = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyList: StateFlow<List<HistoryItem>> = _historyList.asStateFlow()

    // --- DEPENDENCIES ---
    private val dao = AppDatabase.getDatabase(application).historyDao()
    private val json = Json { ignoreUnknownKeys = true }

    // --- AI ENGINE (MediaPipe) ---
    private var llmInference: LlmInference? = null
    private val MODEL_PATH = "/data/local/tmp/llm/model.bin" // Ensure this matches your phone path

    init {
        // 1. Load History
        viewModelScope.launch {
            dao.getAll().collect { items -> _historyList.value = items }
        }

        // 2. Initialize AI Model in Background
        viewModelScope.launch(Dispatchers.IO) {
            val modelFile = File(MODEL_PATH)
            if (modelFile.exists()) {
                try {
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(MODEL_PATH)
                        .setMaxTokens(1024) // Allow long reports
                        .setTopK(40)
                        .setTemperature(0.8f)
                        .setRandomSeed(101)
                        .build()
                    llmInference = LlmInference.createFromOptions(getApplication(), options)
                    Log.d("ScribeVM", "AI Model Loaded Successfully")
                } catch (e: Exception) {
                    Log.e("ScribeVM", "Failed to load AI Model", e)
                }
            } else {
                Log.e("ScribeVM", "Model file not found at $MODEL_PATH")
            }
        }
    }

    // --- FUNCTIONS ---

    fun loadReport(medicalReport: MedicalReport) {
        _report.value = medicalReport
    }

    fun setLanguage(lang: String) {
        _targetLanguage.value = lang
    }

    // --- REAL AI GENERATION ---
    fun generateReport(spokenText: String) {
        if (llmInference == null) {
            Log.e("ScribeVM", "AI Model is not ready yet.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            try {
                val prompt = """
                    You are a medical scribe. Convert this doctor's dictation into a JSON format.
                    Dictation: "$spokenText"
                    
                    Return ONLY valid JSON with these keys: 
                    patientName (string), age (int), heartRate (int), temperature (string), spO2 (int), 
                    symptoms (list of strings), diagnosis (string), treatmentPlan (list of strings).
                    Do not add markdown formatting.
                """.trimIndent()

                val result = llmInference!!.generateResponse(prompt)
                Log.d("ScribeVM", "AI Raw Output: $result")

                // Clean up result (sometimes AI adds ```json)
                val cleanJson = result.replace("```json", "").replace("```", "").trim()

                try {
                    val newReport = json.decodeFromString<MedicalReport>(cleanJson)
                    _report.value = newReport
                    saveToHistory(newReport)
                } catch (e: Exception) {
                    Log.e("ScribeVM", "JSON Parsing Failed", e)
                    // Fallback if JSON fails
                    val fallback = MedicalReport(
                        patientName = "Parsing Error",
                        diagnosis = "Could not parse AI output",
                        treatmentPlan = listOf(result) // Show raw text
                    )
                    _report.value = fallback
                }

            } catch (e: Exception) {
                Log.e("ScribeVM", "Generation Failed", e)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    // --- NEW FEATURE: EDIT REPORT ---
    fun updateReport(oldReport: MedicalReport, newName: String, newDiagnosis: String, newPlanString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newPlanList = newPlanString.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

                val updatedReport = oldReport.copy(
                    patientName = newName,
                    diagnosis = newDiagnosis,
                    treatmentPlan = newPlanList
                )

                if (_report.value == oldReport) {
                    _report.value = updatedReport
                }

                // Update Database
                val oldJson = json.encodeToString(oldReport)
                val currentHistory = _historyList.value
                val itemToUpdate = currentHistory.find { it.fullReportJson == oldJson }

                if (itemToUpdate != null) {
                    val updatedItem = itemToUpdate.copy(
                        patientName = newName,
                        diagnosis = newDiagnosis,
                        fullReportJson = json.encodeToString(updatedReport)
                    )
                    dao.update(updatedItem)
                }
            } catch (e: Exception) {
                Log.e("ScribeVM", "Error updating report", e)
            }
        }
    }

    private fun saveToHistory(report: MedicalReport) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = HistoryItem(
                timestamp = System.currentTimeMillis(),
                patientName = report.patientName,
                diagnosis = report.diagnosis,
                fullReportJson = json.encodeToString(report)
            )
            dao.insert(item)
        }
    }
}