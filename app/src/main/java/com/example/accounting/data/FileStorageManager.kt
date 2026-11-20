package com.example.accounting.data

import android.content.Context
import android.graphics.Bitmap
import com.example.accounting.model.InspectionResult
import com.example.accounting.util.GsonProvider
import java.io.File

class FileStorageManager(private val context: Context) {
    private val gson = GsonProvider.gson

    private val inspectionsDir = File(context.filesDir, "inspections").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    fun saveInspection(result: InspectionResult, screenshot: Bitmap? = null): File {
        val fileName = "inspection_${result.timestamp}.json"
        val file = File(inspectionsDir, fileName)
        file.writeText(gson.toJson(result))
        if (screenshot != null) {
            saveScreenshot(result.timestamp, screenshot)
        }
        return file
    }

    private fun saveScreenshot(timestamp: Long, screenshot: Bitmap) {
        val file = File(inspectionsDir, "inspection_$timestamp.png")
        try {
            file.outputStream().use { output ->
                if (!screenshot.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
            file.delete()
        }
    }

    fun getInspectionFiles(): List<File> {
        return inspectionsDir.listFiles()
            ?.filter { it.name.startsWith("inspection_") && it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteInspection(file: File): Boolean {
        return if (file.exists() && file.parentFile == inspectionsDir) {
            file.delete()
        } else {
            false
        }
    }
}
