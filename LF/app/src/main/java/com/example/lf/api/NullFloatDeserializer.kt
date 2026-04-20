package com.example.lf.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class NullFloatDeserializer : JsonDeserializer<Float?> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Float? {
        return when {
            json.isJsonNull -> null
            json.isJsonPrimitive -> json.asFloat
            json.isJsonObject -> {
                val float64Element = json.asJsonObject.get("Float64")
                val validElement = json.asJsonObject.get("Valid")
                if (validElement != null && validElement.asBoolean && float64Element != null && !float64Element.isJsonNull) {
                    float64Element.asFloat
                } else {
                    null
                }
            }
            else -> null
        }
    }
}