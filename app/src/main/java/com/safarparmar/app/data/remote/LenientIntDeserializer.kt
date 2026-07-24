package com.safarparmar.app.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import kotlin.math.roundToInt

/**
 * Parses a whole-number field that the server may occasionally send as a decimal.
 *
 * A plan whose `dailyGoal` was stored as 6.1 (older write paths used
 * `Number(input)` with no rounding, and the web planner's free-text input allowed
 * decimals) made Gson throw:
 *
 *     java.lang.NumberFormatException: Expected an int but was 6.1 ... $[0].dailyGoal
 *
 * That aborts parsing of the ENTIRE response, so one bad row took down the whole
 * plan list on already-published apps — unrecoverable from the client. The server
 * now rounds on both read and write, but a published app cannot be fixed after the
 * fact, so tolerate the shape here too rather than trusting one side only.
 */
class LenientIntDeserializer : JsonDeserializer<Int?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): Int? {
        if (json == null || json.isJsonNull) return null
        return runCatching {
            val primitive = json.asJsonPrimitive
            when {
                primitive.isNumber -> primitive.asDouble.roundToInt()
                primitive.isString -> primitive.asString.trim().toDoubleOrNull()?.roundToInt()
                else -> null
            }
        }.getOrNull()
    }
}
