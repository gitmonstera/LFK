package com.example.lfk.api

import com.example.lfk.models.User
import com.google.gson.*
import java.lang.reflect.Type

class UserDeserializer : JsonDeserializer<User> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): User {
        val jsonObject = json.asJsonObject

        return User(
            id = jsonObject.get("id")?.asString ?: "",
            username = jsonObject.get("username")?.asString ?: "",
            email = jsonObject.get("email")?.asString ?: "",
            first_name = extractStringValue(jsonObject, "first_name"),
            last_name = extractStringValue(jsonObject, "last_name"),
            created_at = jsonObject.get("created_at")?.asString ?: ""
        )
    }

    private fun extractStringValue(jsonObject: JsonObject, fieldName: String): String? {
        val element = jsonObject.get(fieldName) ?: return null

        return if (element.isJsonObject) {
            // Если это объект с полями String и Valid
            val obj = element.asJsonObject
            obj.get("String")?.asString
        } else {
            // Если это простая строка
            element.asString
        }
    }
}