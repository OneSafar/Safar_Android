package com.safar.app.ui.studyplanner.importexport

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.TopicStatus
import com.safar.app.ui.studyplanner.logic.readableDate
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object StudyPlannerExportUtils {

    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 45f
    private const val LINE_HEIGHT = 20f
    private const val DATE_COLUMN_WIDTH = 90f
    private const val INDENT_STEP = 18f

    fun generateStudyPlanPdf(plan: StudyPlan, outputStream: OutputStream) {
        val document = PdfDocument()
        val writer = PdfWriter(document)

        // ---- Header ----
        writer.drawText(plan.title, size = 22f, isBold = true, color = Color.parseColor("#1F2937"))
        writer.skipLines(0.4f)

        val examInfo = buildString {
            append("Exam: ${plan.examType ?: "Study Plan"}")
            if (!plan.examDate.isNullOrBlank()) {
                append(" • Date: ${readableDate(plan.examDate)}")
            }
        }
        writer.drawText(examInfo, size = 11f, color = Color.GRAY)
        
        val goalInfo = "Daily Goal: ${plan.dailyGoal ?: 0} topics"
        writer.drawText(goalInfo, size = 11f, color = Color.GRAY)
        
        writer.skipLines(0.5f)
        writer.drawHorizontalLine(Color.parseColor("#E5E7EB"))
        writer.skipLines(1.2f)

        // ---- Content ----
        if (plan.subjects.isEmpty()) {
            writer.drawText("No syllabus content added yet.", size = 12f, color = Color.GRAY)
        } else {
            plan.subjects.forEach { subject ->
                val subjectColor = parseHexColor(subject.color)
                
                // Subject
                writer.drawHierarchyItem(
                    text = subject.name.uppercase(),
                    depth = 0,
                    size = 13f,
                    isBold = true,
                    color = subjectColor,
                    dotColor = subjectColor
                )
                
                subject.chapters.forEachIndexed { cIdx, chapter ->
                    val isLastChapter = cIdx == subject.chapters.size - 1
                    
                    // Chapter
                    writer.drawHierarchyItem(
                        text = chapter.name,
                        depth = 1,
                        size = 11f,
                        isBold = true,
                        color = Color.parseColor("#374151"),
                        dotColor = Color.parseColor("#374151")
                    )
                    
                    chapter.topics.forEachIndexed { tIdx, topic ->
                        val statusColor = when (topic.status) {
                            TopicStatus.DONE -> Color.parseColor("#10B981")
                            TopicStatus.REVISION_NEEDED -> Color.parseColor("#F59E0B")
                            else -> Color.parseColor("#9CA3AF")
                        }
                        
                        // Topic
                        writer.drawHierarchyItem(
                            text = topic.name,
                            depth = 2,
                            size = 10f,
                            color = Color.parseColor("#4B5563"),
                            dateStr = topic.plannedDate?.let { readableDate(it) },
                            dotColor = statusColor
                        )
                    }
                    writer.skipLines(0.3f)
                }
                writer.skipLines(0.7f)
            }
        }

        writer.finishPage()
        document.writeTo(outputStream)
        document.close()
    }

    private class PdfWriter(private val document: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var currentY = MARGIN
        private var pageNumber = 0

        private val textPaint = Paint().apply { isAntiAlias = true }
        private val linePaint = Paint().apply { isAntiAlias = true; strokeWidth = 1f; color = Color.parseColor("#D1D5DB") }

        fun drawText(text: String, size: Float = 10f, isBold: Boolean = false, color: Int = Color.BLACK) {
            checkPage(LINE_HEIGHT)
            textPaint.textSize = size
            textPaint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            textPaint.color = color
            textPaint.textAlign = Paint.Align.LEFT
            canvas?.drawText(text, MARGIN, currentY, textPaint)
            currentY += LINE_HEIGHT
        }

        fun drawHierarchyItem(
            text: String,
            depth: Int,
            size: Float = 10f,
            isBold: Boolean = false,
            color: Int = Color.BLACK,
            dotColor: Int? = null,
            dateStr: String? = null
        ) {
            checkPage(LINE_HEIGHT)
            
            val indent = depth * INDENT_STEP
            val textX = MARGIN + indent + 12f
            
            // Draw Vertical Lines for Hierarchy
            for (i in 0 until depth) {
                val lineX = MARGIN + (i * INDENT_STEP) + 4f
                canvas?.drawLine(lineX, currentY - LINE_HEIGHT, lineX, currentY, linePaint)
            }

            // Draw Dot (Bullet Point)
            if (dotColor != null) {
                val dotPaint = Paint().apply { this.color = dotColor; style = Paint.Style.FILL; isAntiAlias = true }
                val dotRadius = when(depth) {
                    0 -> 3.5f
                    1 -> 2.5f
                    else -> 2f
                }
                canvas?.drawCircle(MARGIN + indent + 4f, currentY - 4f, dotRadius, dotPaint)
            }

            // Draw Text
            textPaint.textSize = size
            textPaint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            textPaint.color = color
            textPaint.textAlign = Paint.Align.LEFT

            val availableWidth = PAGE_WIDTH - textX - MARGIN - (if (dateStr != null) DATE_COLUMN_WIDTH else 0f)
            var displayText = text
            if (textPaint.measureText(displayText) > availableWidth) {
                while (textPaint.measureText("$displayText...") > availableWidth && displayText.length > 5) {
                    displayText = displayText.dropLast(1)
                }
                displayText = "$displayText..."
            }
            canvas?.drawText(displayText, textX, currentY, textPaint)

            // Draw Date
            if (dateStr != null) {
                textPaint.textSize = size * 0.9f
                textPaint.typeface = Typeface.DEFAULT
                textPaint.color = Color.GRAY
                textPaint.textAlign = Paint.Align.RIGHT
                canvas?.drawText(dateStr, PAGE_WIDTH - MARGIN, currentY, textPaint)
            }

            currentY += LINE_HEIGHT
        }

        fun drawHorizontalLine(color: Int) {
            checkPage(10f)
            linePaint.color = color
            canvas?.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
            currentY += 5f
        }

        fun skipLines(count: Float) {
            currentY += LINE_HEIGHT * count
            checkPage(0f)
        }

        fun finishPage() {
            page?.let {
                drawFooter(it.canvas)
                document.finishPage(it)
            }
            page = null
            canvas = null
        }

        private fun startNewPage() {
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val newPage = document.startPage(pageInfo)
            page = newPage
            canvas = newPage.canvas
            currentY = MARGIN + 25f
        }

        private fun checkPage(neededHeight: Float) {
            if (page == null || currentY + neededHeight > PAGE_HEIGHT - MARGIN * 1.5f) {
                finishPage()
                startNewPage()
            }
        }

        private fun drawFooter(canvas: Canvas) {
            val footerPaint = Paint().apply {
                color = Color.LTGRAY
                textSize = 9f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            canvas.drawText("Generated by Safar Study Planner • $timestamp • Page $pageNumber", PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN + 15f, footerPaint)
        }
    }

    private fun parseHexColor(hex: String): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.BLACK
        }
    }
}
