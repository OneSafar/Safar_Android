package com.safarparmar.app.feature.youtubestudyv2

import androidx.sqlite.db.SupportSQLiteDatabase
import android.database.Cursor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeAllowlistBackupTest {
    private val dir: File = createTempDirectory("yt-backup").toFile()
    private val store = YoutubeAllowlistBackupStore(File(dir, YoutubeAllowlistBackupStore.FILE_NAME))

    private val channelId = "accessibility:handle:@ezval789"
    private val snapshot = YoutubeAllowlistSnapshot.from(
        identities = listOf(
            YoutubeV2IdentityEntity(channelId, "@ezval789", "EZVAL", null, 10L),
            YoutubeV2IdentityEntity("visited:only", "@visited", "Visited", null, 11L),
        ),
        classifications = listOf(YoutubeV2ClassificationEntity(channelId, "productive", 20L)),
        allowlist = listOf(YoutubeV2AllowlistEntity(channelId, 30L)),
        aliases = listOf(
            YoutubeV2AliasEntity("handle", "@ezval789", channelId, "EZVAL"),
            YoutubeV2AliasEntity("display", "ezval", channelId, "EZVAL"),
        ),
    )

    @Test fun `snapshot keeps only channels the user classified`() {
        assertEquals(listOf(channelId), snapshot.channels.map { it.channelId })
        assertEquals(30L, snapshot.channels.single().allowedAtMs)
        assertEquals(2, snapshot.channels.single().aliases.size)
    }

    @Test fun `store round trips and ignores a corrupt file`() {
        assertNull(store.load())
        store.save(snapshot)
        assertEquals(snapshot, store.load())
        File(dir, YoutubeAllowlistBackupStore.FILE_NAME).writeText("{not json")
        assertNull(store.load())
    }

    private fun countCursor(count: Long): Cursor = mockk(relaxed = true) {
        every { moveToFirst() } returns true
        every { getLong(0) } returns count
    }

    @Test fun `empty database is restored from the backup`() {
        store.save(snapshot)
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query("SELECT COUNT(*) FROM youtube_v2_classification") } returns countCursor(0)

        assertTrue(YoutubeAllowlistRestoreCallback(store).restoreIfEmpty(db))

        verify { db.execSQL(match { it.contains("INTO youtube_v2_identity") }, any()) }
        verify { db.execSQL(match { it.contains("INTO youtube_v2_classification") }, any()) }
        verify { db.execSQL(match { it.contains("INTO youtube_v2_allowlist") }, any()) }
        verify(exactly = 2) { db.execSQL(match { it.contains("INTO youtube_v2_alias") }, any()) }
        verify { db.setTransactionSuccessful() }
    }

    @Test fun `database with decisions is left alone`() {
        store.save(snapshot)
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query("SELECT COUNT(*) FROM youtube_v2_classification") } returns countCursor(3)

        assertFalse(YoutubeAllowlistRestoreCallback(store).restoreIfEmpty(db))
        verify(exactly = 0) { db.execSQL(any(), any()) }
    }

    @Test fun `no backup means nothing to restore`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query("SELECT COUNT(*) FROM youtube_v2_classification") } returns countCursor(0)
        assertFalse(YoutubeAllowlistRestoreCallback(store).restoreIfEmpty(db))
    }

    @Test fun `learning an alias refreshes the backup`() = runTest {
        val database = mockk<YoutubeStudyV2Database>(relaxed = true)
        val dao = mockk<YoutubeStudyV2Dao>(relaxed = true)
        coEvery { dao.channelIdsForHandle("@ezval789") } returns listOf(channelId)
        coEvery { dao.channelIdsForDisplay("ezval") } returns emptyList()
        coEvery { dao.isAllowed(channelId) } returns true
        coEvery { dao.allIdentities() } returns listOf(YoutubeV2IdentityEntity(channelId, "@ezval789", "EZVAL", null, 1L))
        coEvery { dao.allClassifications() } returns listOf(YoutubeV2ClassificationEntity(channelId, "productive", 2L))
        coEvery { dao.allAllowed() } returns listOf(YoutubeV2AllowlistEntity(channelId, 3L))
        coEvery { dao.allAliases() } returns emptyList()

        YoutubeStudyV2Repository(database, dao, store).evaluate("@EZVAL789", "EZVAL")

        assertEquals(listOf(channelId), store.load()?.channels?.map { it.channelId })
    }

    @Test fun `upgrade path covers every version without gaps`() {
        val migrations = YoutubeStudyV2Module.ALL_MIGRATIONS
        assertEquals(1, migrations.first().startVersion)
        migrations.toList().zipWithNext().forEach { (a, b) -> assertEquals(a.endVersion, b.startVersion) }
        // Keep in sync with @Database(version = ...) in YoutubeStudyV2Data.kt.
        assertEquals(9, migrations.last().endVersion)
    }
}
