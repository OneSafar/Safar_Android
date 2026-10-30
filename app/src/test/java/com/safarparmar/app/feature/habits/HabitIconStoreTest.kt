package com.safarparmar.app.feature.habits

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.safarparmar.app.feature.habits.data.HabitIconStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class HabitIconStoreTest {
    @Test fun fullEmojiSequencesSurviveDiskReloadAndStayAccountScoped() = runBlocking {
        val directory = Files.createTempDirectory("habit-icons").toFile()
        val file = directory.resolve("icons.preferences_pb")
        val owner = MutableStateFlow<String?>("student-a")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = HabitIconStore(PreferenceDataStoreFactory.create(scope = scope) { file }, owner)
        try {
            store.save("student-a", "stable-habit-id", "👩🏽‍💻")
            assertEquals("👩🏽‍💻", store.icons.first()["stable-habit-id"])
            owner.value = "student-b"
            assertTrue(store.icons.first().isEmpty())
            assertTrue(runCatching { store.save("student-a", "stable-habit-id", "📖") }.isFailure)
            store.save("student-b", "stable-habit-id", "🏃🏾‍♀️")
            owner.value = null
            assertTrue(store.icons.first().isEmpty())
            owner.value = "student-a"
            scope.cancel(); scope.coroutineContext[Job]!!.join()
            val reopenedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val reopened = HabitIconStore(PreferenceDataStoreFactory.create(scope = reopenedScope) { file }, owner)
                assertEquals("👩🏽‍💻", reopened.icons.first()["stable-habit-id"])
                reopened.save("student-a", "stable-habit-id", "🇮🇳")
                assertEquals("🇮🇳", reopened.icons.first()["stable-habit-id"])
                owner.value = "student-b"
                assertEquals("🏃🏾‍♀️", reopened.icons.first()["stable-habit-id"])
            } finally { reopenedScope.cancel(); reopenedScope.coroutineContext[Job]!!.join() }
        } finally { scope.cancel(); directory.deleteRecursively() }
    }
}
