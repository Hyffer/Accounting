package com.example.accounting.data

import android.content.Context
import android.graphics.Rect
import com.example.accounting.model.InspectionResult
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.File

class FileStorageManager(private val context: Context) {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Rect::class.java, object : TypeAdapter<Rect>() {
            override fun write(out: JsonWriter, value: Rect?) {
                if (value == null) {
                    out.nullValue()
                } else {
                    out.beginArray()
                    out.value(value.left)
                    out.value(value.top)
                    out.value(value.right)
                    out.value(value.bottom)
                    out.endArray()
                }
            }

            override fun read(reader: JsonReader): Rect? {
                return if (reader.peek() == null) {
                    reader.nextNull()
                    null
                } else {
                    reader.beginArray()
                    val left = reader.nextInt()
                    val top = reader.nextInt()
                    val right = reader.nextInt()
                    val bottom = reader.nextInt()
                    reader.endArray()
                    Rect(left, top, right, bottom)
                }
            }
        })
        .setPrettyPrinting()
        .create()

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
