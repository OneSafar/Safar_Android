package com.safarparmar.app.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SafarDataStoreCorruptionTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun malformedPreferencesRecoverOnWriteAndRemainWritable() = runBlocking {
        val file = File(temporaryFolder.root, "safar_prefs.preferences_pb")
        file.writeBytes(byteArrayOf(0)) // Invalid protobuf tag, matching the production report.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(
                corruptionHandler = safarPreferencesCorruptionHandler(),
                scope = scope,
                produceFile = { file },
            )
            val key = stringPreferencesKey("recovered_setting")
            assertEquals("saved", store.edit { it[key] = "saved" }[key])
            assertEquals("saved", store.data.first()[key])
        } finally {
            scope.cancel()
        }
    }
}
