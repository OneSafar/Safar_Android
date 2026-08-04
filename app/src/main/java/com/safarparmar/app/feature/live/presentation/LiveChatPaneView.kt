package com.safarparmar.app.feature.live.presentation

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The live comments pane shown beside the video inside [VideoPlayerActivity].
 *
 * Built from plain Android Views on purpose. The player Activity deliberately
 * avoids Compose because the WebView's SurfaceView compositing breaks underneath
 * it (see [VideoPlayerActivity]); keeping this pane in Views means the video and
 * the chat are plain siblings that never composite over one another.
 */
class LiveChatPaneView(context: Context) : LinearLayout(context) {

    private val header = TextView(context)
    private val viewerLabel = TextView(context)
    private val emptyLabel = TextView(context)
    private val messagesScroll = ScrollView(context)
    private val messagesColumn = LinearLayout(context)
    private val input = EditText(context)
    private val sendButton = Button(context)

    private var isChatOpen = false
    private var cooldownRemaining = 0
    private var closedReason = "Comments open when the session goes live."

    var onSend: ((String) -> Unit)? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(SURFACE)
        val pad = dp(12)
        setPadding(pad, pad, pad, pad)

        addView(buildHeaderRow())
        addView(divider())

        messagesColumn.orientation = VERTICAL
        messagesScroll.addView(
            messagesColumn,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
        addView(messagesScroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        emptyLabel.apply {
            setTextColor(TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            text = closedReason
        }
        addView(emptyLabel, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        addView(divider())
        addView(buildComposerRow())

        renderState()
    }

    private fun buildHeaderRow(): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        header.apply {
            text = "Live comments"
            setTextColor(TEXT_PRIMARY)
            setTypeface(null, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
        addView(header, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        viewerLabel.apply {
            setTextColor(TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            visibility = View.GONE
        }
        addView(viewerLabel)
    }

    private fun buildComposerRow(): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        input.apply {
            hint = "Add a comment…"
            setHintTextColor(TEXT_SECONDARY)
            setTextColor(TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            maxLines = 3
            filters = arrayOf(InputFilter.LengthFilter(MAX_MESSAGE_LENGTH))
            imeOptions = EditorInfo.IME_ACTION_SEND
            setBackgroundColor(Color.TRANSPARENT)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    submit()
                    true
                } else {
                    false
                }
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun afterTextChanged(s: Editable?) = updateSendEnabled()
            })
        }
        addView(input, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        sendButton.apply {
            text = "Send"
            isAllCaps = false
            setOnClickListener { submit() }
        }
        addView(sendButton)
    }

    private fun submit() {
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isEmpty() || !canSend()) return
        onSend?.invoke(text)
        input.setText("")
    }

    private fun canSend(): Boolean = isChatOpen && cooldownRemaining == 0

    private fun updateSendEnabled() {
        val hasText = !input.text.isNullOrBlank()
        sendButton.isEnabled = hasText && canSend()
        sendButton.alpha = if (sendButton.isEnabled) 1f else 0.4f
    }

    // ── Public state ─────────────────────────────────────────────────────────

    fun setChatOpen(open: Boolean, reason: String) {
        isChatOpen = open
        closedReason = reason
        if (!open) {
            messagesColumn.removeAllViews()
            cooldownRemaining = 0
            input.setText("")
        }
        renderState()
    }

    fun setViewerCount(count: Int, isLive: Boolean) {
        viewerLabel.visibility = if (isLive) View.VISIBLE else View.GONE
        viewerLabel.text = if (count == 1) "● 1 watching" else "● $count watching"
    }

    fun setCooldown(seconds: Int) {
        cooldownRemaining = seconds.coerceAtLeast(0)
        renderState()
    }

    fun addMessage(author: String, text: String, isMine: Boolean) {
        if (!isChatOpen) return
        messagesColumn.addView(buildBubble(author, text, isMine))
        emptyLabel.visibility = View.GONE
        messagesScroll.visibility = View.VISIBLE
        messagesScroll.post { messagesScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun renderState() {
        val hasMessages = messagesColumn.childCount > 0
        emptyLabel.visibility = if (hasMessages && isChatOpen) View.GONE else View.VISIBLE
        messagesScroll.visibility = if (hasMessages && isChatOpen) View.VISIBLE else View.GONE
        emptyLabel.text = when {
            !isChatOpen -> closedReason
            else -> "No comments yet. Say hello!"
        }

        input.isEnabled = isChatOpen && cooldownRemaining == 0
        input.hint = when {
            !isChatOpen -> "Comments are closed"
            // Stated plainly, so a disabled Send never reads as a bug.
            cooldownRemaining > 0 -> "Wait ${cooldownRemaining}s before commenting again"
            else -> "Add a comment…"
        }
        sendButton.text = if (cooldownRemaining > 0) "${cooldownRemaining}s" else "Send"
        updateSendEnabled()
    }

    private fun buildBubble(author: String, text: String, isMine: Boolean): View =
        LinearLayout(context).apply {
            orientation = VERTICAL
            val vertical = dp(6)
            setPadding(0, vertical, 0, vertical)

            addView(
                TextView(context).apply {
                    this.text = if (isMine) "You" else author
                    setTextColor(if (isMine) ACCENT else TEXT_SECONDARY)
                    setTypeface(null, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                },
            )
            addView(
                TextView(context).apply {
                    this.text = text
                    setTextColor(TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                },
            )
        }

    private fun divider(): View = View(context).apply {
        setBackgroundColor(DIVIDER)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(1)).also {
            it.topMargin = dp(8)
            it.bottomMargin = dp(8)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        // The player Activity is always a dark, full-bleed surface, so these are
        // fixed rather than theme-derived.
        const val SURFACE = 0xFF121212.toInt()
        const val TEXT_PRIMARY = 0xFFECECEC.toInt()
        const val TEXT_SECONDARY = 0xFF9E9E9E.toInt()
        const val DIVIDER = 0x33FFFFFF
        const val ACCENT = 0xFFFF6B9D.toInt()
        const val MAX_MESSAGE_LENGTH = 500
    }
}
