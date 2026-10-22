package com.safarparmar.app.feature.toppersbatch

import android.app.DatePickerDialog
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

class BatchPickerThemeTest {
    @Test fun datePickerInflatesWithClampedScaleAndActivityTheme() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (scale in listOf(0.7f, 1f, 1.8f)) {
                val activityTheme = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_Safar)
                activityTheme.applyOverrideConfiguration(Configuration().apply { fontScale = scale })
                val pickerContext = batchPickerContext(activityTheme)
                assertEquals(scale.coerceIn(0.85f, 1.05f), pickerContext.resources.configuration.fontScale, 0.001f)
                // Regression: this constructor threw InflateException after createConfigurationContext.
                val picker = DatePickerDialog(pickerContext, null, 2026, 9, 4)
                assertEquals(4, picker.datePicker.dayOfMonth)
            }
        }
    }
}
