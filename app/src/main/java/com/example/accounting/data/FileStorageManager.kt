package com.example.accounting.data

import android.content.Context
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

    fun saveInspection(result: InspectionResult): File {
        val fileName = "inspection_${result.timestamp}.json"
        val file = File(inspectionsDir, fileName)
        file.writeText(gson.toJson(result))
        return file
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
