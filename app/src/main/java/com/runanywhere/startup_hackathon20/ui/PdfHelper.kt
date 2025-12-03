package com.runanywhere.startup_hackathon20.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.runanywhere.startup_hackathon20.data.model.MedicalReport
import java.io.File
import java.io.FileOutputStream

object PdfHelper {

    fun generateAndSharePdf(context: Context, report: MedicalReport) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // --- DRAWING LOGIC ---
        var y = 60f // Starting Y position

        // Title
        paint.textSize = 24f
        paint.isFakeBoldText = true
        paint.color = android.graphics.Color.BLUE
        canvas.drawText("Medical Scribe Report", 40f, y, paint)
        y += 40f

        // Patient
        paint.textSize = 16f
        paint.color = android.graphics.Color.BLACK
        canvas.drawText("Patient: ${report.patientName}", 40f, y, paint)
        y += 30f

        // Diagnosis
        paint.color = android.graphics.Color.RED
        canvas.drawText("Diagnosis: ${report.diagnosis}", 40f, y, paint)
        y += 25f

        // ICD-10 Code
        paint.color = android.graphics.Color.parseColor("#2E7D32") // Green
        canvas.drawText("Billing Code: ${report.icd10Code}", 40f, y, paint)
        y += 40f

        // --- FIX: ADDED SYMPTOMS SECTION HERE ---
        if (report.symptoms.isNotEmpty()) {
            paint.color = android.graphics.Color.BLACK
            paint.isFakeBoldText = true
            canvas.drawText("Symptoms:", 40f, y, paint)
            paint.isFakeBoldText = false
            y += 25f

            report.symptoms.forEach { symptom ->
                canvas.drawText("• $symptom", 60f, y, paint)
                y += 20f
            }
            y += 20f // Extra spacing after list
        }
        // ----------------------------------------

        // Vitals
        paint.color = android.graphics.Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("Vital Signs:", 40f, y, paint)
        paint.isFakeBoldText = false
        y += 25f
        report.vitals.forEach { (key, value) ->
            canvas.drawText("- $key: $value", 60f, y, paint)
            y += 20f
        }
        y += 20f

        // Plan
        paint.isFakeBoldText = true
        canvas.drawText("Treatment Plan:", 40f, y, paint)
        paint.isFakeBoldText = false
        y += 25f
        report.treatmentPlan.forEach { plan ->
            // Simple text wrapping logic to keep it readable
            if (plan.length > 60) {
                canvas.drawText("- ${plan.take(60)}...", 60f, y, paint)
            } else {
                canvas.drawText("- $plan", 60f, y, paint)
            }
            y += 20f
        }

        doc.finishPage(page)

        // --- SAVING ---
        val file = File(context.cacheDir, "MedicalReport_${System.currentTimeMillis()}.pdf")
        try {
            val fos = FileOutputStream(file)
            doc.writeTo(fos)
            doc.close()
            fos.close()
            sharePdf(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Medical Report"))
    }
}