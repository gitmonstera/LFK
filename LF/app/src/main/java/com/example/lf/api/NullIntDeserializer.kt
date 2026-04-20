package com.example.lf.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class NullIntDeserializer : JsonDeserializer<Int?> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Int? {
        return when {
            json.isJsonNull -> null
            json.isJsonPrimitive -> json.asInt
            json.isJsonObject -> {
                val int64Element = json.asJsonObject.get("Int64")
                val validElement = json.asJsonObject.get("Valid")
                if (validElement != null && validElement.asBoolean && int64Element != null && !int64Element.isJsonNull) {
                    int64Element.asInt
                } else {
                    null
                }
            }
            else -> null
        }
    }
}