package com.example.lf.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class NullStringDeserializer : JsonDeserializer<String?> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): String? {
        return when {
            json.isJsonNull -> null
            json.isJsonPrimitive -> json.asString
            json.isJsonObject -> {
                val stringElement = json.asJsonObject.get("String")
                val validElement = json.asJsonObject.get("Valid")
                if (validElement != null && validElement.asBoolean && stringElement != null && !stringElement.isJsonNull) {
                    stringElement.asString
                } else {
                    null
                }
            }
            else -> null
        }
    }
}