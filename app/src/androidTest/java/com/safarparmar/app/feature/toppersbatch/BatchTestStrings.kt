package com.safarparmar.app.feature.toppersbatch

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.platform.app.InstrumentationRegistry

internal fun batchTestStrings(): BatchStrings {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val configuration = Configuration(context.resources.configuration)
    AppCompatDelegate.getApplicationLocales()[0]?.let(configuration::setLocale)
    return BatchStrings(context.createConfigurationContext(configuration).resources)
}
internal fun batchTestText(id: Int, vararg arguments: Any?) = batchTestStrings().text(id, *arguments)
internal fun batchTestQuantity(id: Int, count: Int, vararg arguments: Any?) = batchTestStrings().quantity(id, count, *arguments)
