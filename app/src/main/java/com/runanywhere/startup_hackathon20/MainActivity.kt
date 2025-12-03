package com.runanywhere.startup_hackathon20

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // <--- FIXES "getValue" ERRORS
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

// --- COLORS ---
object MedColors {
    val Primary = Color(0xFF2563EB)
    val Background = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)
    val TextDark = Color(0xFF1E293B)
    val TextGray = Color(0xFF64748B)
}

class MainActivity : ComponentActivity() {
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ScribeScreen(cameraLauncher)
            }
        }
    }
}

@Composable
fun ScribeScreen(cameraLauncher: androidx.activity.result.ActivityResultLauncher<Void?>) {
    val viewModel: ScribeViewModel = viewModel()

    // Collecting State
    val report by viewModel.report.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val currentLanguage by viewModel.targetLanguage.collectAsState()

    var showHistory by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MedColors.Background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SCRIBE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MedColors.Primary, letterSpacing = 2.sp)
                    Text("AI Medical Assistant", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MedColors.TextDark)
                }
                IconButton(onClick = { showHistory = true }) {
                    Icon(Icons.Rounded.History, "History", tint = MedColors.TextDark)
                }
            }

            // Language
            Row(modifier = Modifier.padding(horizontal = 24.dp)) {
                listOf("English", "Spanish", "Hindi").forEach { lang ->
                    FilterChip(
                        selected = currentLanguage == lang,
                        onClick = { viewModel.setLanguage(lang) },
                        label = { Text(lang) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Record Button
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.generateReport("Dummy Audio") },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isGenerating) Color.Gray else MedColors.Primary)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Rounded.Mic, "Record", modifier = Modifier.size(32.dp))
                    }
                }
            }
            Text("Tap to Record Consultation", modifier = Modifier.fillMaxWidth().padding(top = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MedColors.TextGray)

            Spacer(modifier = Modifier.height(32.dp))

            // Report Card
            AnimatedVisibility(visible = report != null) {
                report?.let { safeReport ->
                    ResultCard(
                        report = safeReport,
                        viewModel = viewModel,
                        onOpenCamera = { cameraLauncher.launch(null) },
                        onEditClick = { showEditDialog = true }
                    )
                }
            }
        }

        // History Dialog
        if (showHistory) {
            HistoryWindow(
                viewModel = viewModel,
                onDismiss = { showHistory = false },
                onSelectReport = { selectedReport ->
                    viewModel.loadReport(selectedReport)
                    showHistory = false
                }
            )
        }

        // Edit Dialog
        if (showEditDialog && report != null) {
            EditReportDialog(
                report = report!!,
                onDismiss = { showEditDialog = false },
                onSave = { name, diag, plan ->
                    viewModel.updateReport(report!!, name, diag, plan)
                }
            )
        }
    }
}

@Composable
fun EditReportDialog(
    report: MedicalReport,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(report.patientName) }
    var diagnosis by remember { mutableStateOf(report.diagnosis) }
    var plan by remember { mutableStateOf(report.treatmentPlan.joinToString("\n")) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Edit Report", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MedColors.TextDark)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Patient Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it }, label = { Text("Diagnosis") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = plan, onValueChange = { plan = it }, label = { Text("Plan (Lines)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onSave(name, diagnosis, plan); onDismiss() }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun ResultCard(
    report: MedicalReport,
    viewModel: ScribeViewModel,
    onOpenCamera: () -> Unit,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.targetLanguage.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(UiTranslations.get("title", currentLanguage).uppercase(), fontSize = 12.sp, color = MedColors.TextGray, fontWeight = FontWeight.Bold)
                    Text(report.patientName, fontSize = 24.sp, fontWeight = FontWeight.Black, color = MedColors.TextDark)
                }
                IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Edit", tint = MedColors.Primary) }
                IconButton(onClick = onOpenCamera) { Icon(Icons.Rounded.CameraAlt, "Photo", tint = MedColors.Primary) }
                IconButton(onClick = { PdfHelper.generateAndSharePdf(context, report) }) { Icon(Icons.Rounded.Share, "PDF", tint = MedColors.Primary) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Vitals
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                InfoChip(Icons.Rounded.Favorite, "${report.heartRate} BPM", Color(0xFFFFEBEE), Color(0xFFD32F2F))
                Spacer(modifier = Modifier.width(8.dp))
                InfoChip(Icons.Rounded.Thermostat, report.temperature, Color(0xFFFFF3E0), Color(0xFFF57C00))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(UiTranslations.get("diagnosis", currentLanguage), fontWeight = FontWeight.Bold, color = MedColors.Primary)
            Text(report.diagnosis, color = MedColors.TextDark)

            Spacer(modifier = Modifier.height(16.dp))
            Text(UiTranslations.get("plan", currentLanguage), fontWeight = FontWeight.Bold, color = MedColors.Primary)
            report.treatmentPlan.forEach { item ->
                Text("• $item", color = MedColors.TextDark, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String, bgColor: Color, iconColor: Color) {
    Surface(color = bgColor, shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, fontWeight = FontWeight.Bold, color = iconColor)
        }
    }
}

@Composable
fun HistoryWindow(viewModel: ScribeViewModel, onDismiss: () -> Unit, onSelectReport: (MedicalReport) -> Unit) {
    val historyList by viewModel.historyList.collectAsState()
    val json = Json { ignoreUnknownKeys = true }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().height(500.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("History", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                LazyColumn {
                    items(historyList) { item ->
                        val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(4.dp).clickable {
                                try {
                                    val report = json.decodeFromString<MedicalReport>(item.fullReportJson)
                                    onSelectReport(report)
                                } catch (e: Exception) { e.printStackTrace() }
                            },
                            colors = CardDefaults.cardColors(containerColor = MedColors.Background)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(item.patientName, fontWeight = FontWeight.Bold)
                                Text(item.diagnosis, fontSize = 12.sp)
                                Text(date, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}