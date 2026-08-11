package com.example.accounting.util

import com.example.accounting.model.Rect
import com.example.accounting.model.PaymentViewType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

object GsonProvider {
    val gson: Gson = GsonBuilder()
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
                return if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
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
        .registerTypeAdapter(PaymentViewType::class.java, object : TypeAdapter<PaymentViewType>() {
            override fun write(out: JsonWriter, value: PaymentViewType?) {
                if (value == null) {
                    out.nullValue()
                } else {
                    out.value(value::class.java.simpleName)
                }
            }

            override fun read(reader: JsonReader): PaymentViewType? {
                return if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
                    reader.nextNull()
                    null
                } else {
                    when (reader.nextString()) {
                        "Request" -> PaymentViewType.Request
                        "Success" -> PaymentViewType.Success
                        else -> PaymentViewType.None
                    }
                }
            }
        })
        .setPrettyPrinting()
        .create()
}