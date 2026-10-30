package com.safarparmar.app.feature.habits.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.emoji2.emojipicker.EmojiPickerView

/** AndroidX's full emoji palette, including categories, variants and recents. */
@Composable
internal fun HabitEmojiPicker(onDismiss: () -> Unit, onPicked: (String) -> Unit) {
    val currentOnPicked = rememberUpdatedState(onPicked)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.padding(16.dp).widthIn(max = 560.dp).fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), color = HabitColors.Background) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Choose habit emoji", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = HabitColors.TextPrimary)
                    IconButton(onClick = onDismiss, modifier = Modifier.habitPress(scaleDown = HabitMotion.ScaleIcon)) { Icon(Icons.Rounded.Close, "Close emoji palette", tint = HabitColors.TextPrimary) }
                }
                BoxWithConstraints {
                    val pickerHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * .48f).coerceIn(180.dp, 400.dp)
                    AndroidView(
                        factory = { context -> EmojiPickerView(context).apply {
                            emojiGridColumns = 8
                            setOnEmojiPickedListener { currentOnPicked.value(it.emoji) }
                        } },
                        modifier = Modifier.fillMaxWidth().height(pickerHeight)
                    )
                }
            }
        }
    }
}
