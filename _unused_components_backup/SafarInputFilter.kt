package com.safarparmar.app.ui.components

import androidx.compose.foundation.text.input.InputTransformation

/** Apply a form's existing length or character rule before text reaches callback state. */
fun safarInputFilter(filter: (String) -> String): InputTransformation = InputTransformation {
    val typed = asCharSequence().toString()
    val accepted = filter(typed)
    if (accepted != typed) replace(0, length, accepted)
}
