package com.safarparmar.app.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.safarparmar.app.domain.model.studyplanner.TemplateTopic
import com.safarparmar.app.domain.model.studyplanner.TopicSize
import java.lang.reflect.Type

/**
 * Exam-template topics arrive in two shapes and both are valid:
 *
 *   "Kinematics"                              — unweighted template
 *   { "name": "Kinematics", "size": "big" }   — hand-weighted template
 *
 * The string form is the original wire format and is still used by most
 * templates (and by the web client), so it must keep working. Without this
 * adapter Gson throws "Expected a string but was BEGIN_OBJECT" on the weighted
 * ones (JEE Main, JEE Advanced, NEET UG), which breaks template selection
 * entirely for those exams.
 */
class TemplateTopicDeserializer : JsonDeserializer<TemplateTopic> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): TemplateTopic {
        if (json.isJsonPrimitive) {
            return TemplateTopic(name = json.asString.orEmpty())
        }
        if (!json.isJsonObject) return TemplateTopic()

        val obj = json.asJsonObject
        val name = obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
        val size = obj.get("size")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?.let { raw -> TopicSize.entries.firstOrNull { it.wireValue == raw } }
        return TemplateTopic(name = name, size = size)
    }
}
