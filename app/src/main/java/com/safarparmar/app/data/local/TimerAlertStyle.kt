package com.safarparmar.app.data.local

enum class TimerAlertStyle(val storedValue: String) {
    SOUND("sound"),
    VIBRATE("vibrate"),
    OFF("off");

    companion object {
        fun fromStoredValue(value: String?): TimerAlertStyle =
            entries.firstOrNull { it.storedValue == value } ?: SOUND
    }
}
