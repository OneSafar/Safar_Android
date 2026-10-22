package com.safarparmar.app.feature.toppersbatch

import android.content.Context
import android.content.SharedPreferences
import com.safarparmar.app.data.local.SafarDataStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BatchOverviewCacheTest {
    private val user = MutableStateFlow<String?>("alice")
    private val saved = mutableMapOf<String, String>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val prefs = mockk<SharedPreferences>()
    private val context = mockk<Context>()
    private val accounts = mockk<SafarDataStore>()
    init {
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { accounts.userId } returns user
        every { prefs.getString(any(), any()) } answers { saved[firstArg()] }
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers { saved[firstArg()] = secondArg(); editor }
        every { editor.remove(any()) } answers { saved.remove(firstArg<String>()); editor }
    }
    @Test fun snapshotsNeverCrossAccountsOrLogout() = runBlocking {
        val cache = BatchOverviewCache(context, accounts)
        cache.save("alice", BatchOverview(), cache.version())
        assertNotNull(cache.load())
        user.value = "bob"
        assertNull(cache.load())
        cache.save("alice", BatchOverview(), cache.version())
        assertFalse(saved.containsKey("bob"))
        user.value = null
        assertNull(cache.load())
    }
    @Test fun editsInvalidateCacheAndPreventAnOlderRefreshFromSaving() = runBlocking {
        val cache = BatchOverviewCache(context, accounts)
        val version = cache.version()
        cache.save("alice", BatchOverview(), version)
        cache.clear()
        cache.save("alice", BatchOverview(), version)
        assertNull(cache.load())
        cache.save("alice", BatchOverview(), cache.version())
        assertNotNull(cache.load())
    }
    @Test fun corruptAndExpiredSnapshotsAreIgnored() = runBlocking {
        val cache = BatchOverviewCache(context, accounts)
        saved["alice"] = """{"savedAt":${System.currentTimeMillis()},"overview":{"course":null}}"""
        assertNull(cache.load())
        saved["alice"] = "not json"
        assertNull(cache.load())
        saved["alice"] = """{"savedAt":1,"overview":{}}"""
        assertNull(cache.load())
    }
    @Test fun snapshotsAndStudyModesStaySeparateBetweenBatches() = runBlocking {
        val cache = BatchOverviewCache(context, accounts)
        val toppers = BatchOverview(course = BatchCourse(id = TOPPERS_COURSE_ID))
        val gk5 = BatchOverview(course = BatchCourse(id = GK5_COURSE_ID))
        cache.save("alice", toppers, cache.version())
        cache.save("alice", gk5, cache.version())
        cache.saveStudyMode(BatchStudyMode.OFFICIAL)
        cache.saveStudyMode(BatchStudyMode.PERSONAL, GK5_COURSE_ID)
        assertEquals(TOPPERS_COURSE_ID, cache.load()!!.course.id)
        assertEquals(GK5_COURSE_ID, cache.load(GK5_COURSE_ID)!!.course.id)
        assertEquals(BatchStudyMode.OFFICIAL, cache.studyMode())
        assertEquals(BatchStudyMode.PERSONAL, cache.studyMode(GK5_COURSE_ID))
        cache.clear(GK5_COURSE_ID)
        assertNull(cache.load(GK5_COURSE_ID))
        assertNotNull(cache.load())
        assertEquals(BatchStudyMode.PERSONAL, cache.studyMode(GK5_COURSE_ID))
    }
    @Test fun todayEventsComeFromTheOverviewWithoutMixingDisabledSubjects() {
        val subject = BatchSubject(id = "english", key = "english", enabled = true)
        val row = BatchLecture(id = "one", subjectId = "english", subjectKey = "english", scheduledFor = "2026-10-05")
        val overview = BatchOverview(subjects = listOf(subject, subject.copy(id = "off", enabled = false)),
            lectures = listOf(row, row.copy(id = "disabled", subjectId = "off"), row.copy(id = "tomorrow", scheduledFor = "2026-10-06")))
        assertEquals(listOf("one"), overview.todayEvents("2026-10-05").lectures.map { it.id })
    }
}
