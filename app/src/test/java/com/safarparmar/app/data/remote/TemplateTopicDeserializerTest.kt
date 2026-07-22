package com.safarparmar.app.data.remote

import com.google.gson.GsonBuilder
import com.safarparmar.app.domain.model.studyplanner.ChapterDifficulty
import com.safarparmar.app.domain.model.studyplanner.ExamTemplate
import com.safarparmar.app.domain.model.studyplanner.TemplateTopic
import com.safarparmar.app.domain.model.studyplanner.TopicSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Exam templates come over the wire in two shapes — plain strings for unweighted
 * templates, {name, size} objects for hand-weighted ones. Parsing was previously
 * typed as List<String>, which threw on every weighted template and broke
 * template selection for JEE Main, JEE Advanced and NEET UG. Compile checks and
 * scheduling tests both missed it because neither one parses real template JSON,
 * so these tests exercise Gson directly.
 */
class TemplateTopicDeserializerTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(TemplateTopic::class.java, TemplateTopicDeserializer())
        .create()

    private fun parse(json: String): ExamTemplate = gson.fromJson(json, ExamTemplate::class.java)

    @Test
    fun `parses an unweighted template whose topics are plain strings`() {
        val template = parse(
            """
            {"id":"ssc-cgl","name":"SSC CGL","subjects":[
              {"name":"Quant","chapters":[
                {"name":"Number System","topics":["LCM & HCF","Divisibility Rules"]}
              ]}
            ]}
            """.trimIndent(),
        )

        val chapter = template.subjects.single().chapters.single()
        assertEquals(listOf("LCM & HCF", "Divisibility Rules"), chapter.topics.map { it.name })
        assertNull(chapter.topics.first().size)
        // No weights at all — nothing to pre-fill the rating step with.
        assertNull(chapter.impliedDifficulty)
    }

    @Test
    fun `parses a hand-weighted template whose topics are objects`() {
        val template = parse(
            """
            {"id":"jee-main","name":"JEE Main","subjects":[
              {"name":"Physics","chapters":[
                {"name":"Rotational Motion","topics":[
                  {"name":"Torque","size":"big"},
                  {"name":"Moment of Inertia","size":"big"}
                ]}
              ]}
            ]}
            """.trimIndent(),
        )

        val chapter = template.subjects.single().chapters.single()
        assertEquals(listOf("Torque", "Moment of Inertia"), chapter.topics.map { it.name })
        assertEquals(TopicSize.BIG, chapter.topics.first().size)
        assertEquals(ChapterDifficulty.TOUGH, chapter.impliedDifficulty)
    }

    @Test
    fun `parses a chapter that mixes both shapes`() {
        val template = parse(
            """
            {"id":"mixed","name":"Mixed","subjects":[
              {"name":"S","chapters":[
                {"name":"C","topics":["Plain topic",{"name":"Weighted topic","size":"small"}]}
              ]}
            ]}
            """.trimIndent(),
        )

        val topics = template.subjects.single().chapters.single().topics
        assertEquals("Plain topic", topics[0].name)
        assertNull(topics[0].size)
        assertEquals("Weighted topic", topics[1].name)
        assertEquals(TopicSize.SMALL, topics[1].size)
        // Partially weighted chapters must not imply a rating either way.
        assertNull(template.subjects.single().chapters.single().impliedDifficulty)
    }

    @Test
    fun `maps uniform sizes onto the matching chapter rating`() {
        fun chapterOf(size: String) = parse(
            """
            {"id":"t","name":"T","subjects":[
              {"name":"S","chapters":[
                {"name":"C","topics":[{"name":"a","size":"$size"},{"name":"b","size":"$size"}]}
              ]}
            ]}
            """.trimIndent(),
        ).subjects.single().chapters.single()

        assertEquals(ChapterDifficulty.EASY, chapterOf("small").impliedDifficulty)
        assertEquals(ChapterDifficulty.NORMAL, chapterOf("medium").impliedDifficulty)
        assertEquals(ChapterDifficulty.TOUGH, chapterOf("big").impliedDifficulty)
    }

    @Test
    fun `ignores an unknown size instead of crashing`() {
        val topics = parse(
            """
            {"id":"t","name":"T","subjects":[
              {"name":"S","chapters":[{"name":"C","topics":[{"name":"a","size":"gigantic"}]}]}
            ]}
            """.trimIndent(),
        ).subjects.single().chapters.single().topics

        assertEquals("a", topics.single().name)
        assertNull(topics.single().size)
    }
}
