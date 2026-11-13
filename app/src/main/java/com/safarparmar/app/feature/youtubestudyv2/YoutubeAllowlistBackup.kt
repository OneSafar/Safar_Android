package com.safarparmar.app.feature.youtubestudyv2

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import java.io.File

/**
 * A copy of the user's channel decisions kept outside the Room database.
 * If the database is ever recreated empty (a downgrade, a corrupt file or a
 * missing migration), the copy is written back when the database opens.
 *
 * It lives in app-private storage. The app sets allowBackup=false, so this
 * protects against in-app database loss, not against an uninstall.
 */
data class YoutubeAllowlistSnapshot(
    val version: Int = 1,
    val channels: List<Channel> = emptyList(),
) {
    data class Channel(
        val channelId: String,
        val handle: String,
        val displayName: String,
        val thumbnailUrl: String?,
        val resolvedAtMs: Long,
        val classification: String,
        val classifiedAtMs: Long,
        val allowedAtMs: Long?,
        val aliases: List<Alias>,
    )

    data class Alias(val type: String, val normalized: String, val display: String)

    companion object {
        fun from(
            identities: List<YoutubeV2IdentityEntity>,
            classifications: List<YoutubeV2ClassificationEntity>,
            allowlist: List<YoutubeV2AllowlistEntity>,
            aliases: List<YoutubeV2AliasEntity>,
        ): YoutubeAllowlistSnapshot {
            val identityById = identities.associateBy { it.channelId }
            val allowedAt = allowlist.associate { it.channelId to it.addedAtMs }
            val aliasesById = aliases.groupBy { it.channelId }
            // A channel is the user's decision only once it has a classification.
            val channels = classifications.mapNotNull { rule ->
                val identity = identityById[rule.channelId] ?: return@mapNotNull null
                Channel(
                    channelId = rule.channelId,
                    handle = identity.handle,
                    displayName = identity.displayName,
                    thumbnailUrl = identity.thumbnailUrl,
                    resolvedAtMs = identity.resolvedAtMs,
                    classification = rule.classification,
                    classifiedAtMs = rule.updatedAtMs,
                    allowedAtMs = allowedAt[rule.channelId],
                    aliases = aliasesById[rule.channelId].orEmpty()
                        .map { Alias(it.aliasType, it.normalizedAlias, it.displayValue) },
                )
            }.sortedBy { it.channelId }
            return YoutubeAllowlistSnapshot(channels = channels)
        }
    }
}

class YoutubeAllowlistBackupStore(private val file: File) {
    private val gson = Gson()
    private val lock = Any()

    fun exists(): Boolean = file.exists()

    fun save(snapshot: YoutubeAllowlistSnapshot) = synchronized(lock) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(gson.toJson(snapshot))
        if (!temp.renameTo(file)) {
            file.writeText(temp.readText())
            temp.delete()
        }
    }

    fun load(): YoutubeAllowlistSnapshot? = synchronized(lock) {
        if (!file.exists()) return null
        runCatching { gson.fromJson(file.readText(), YoutubeAllowlistSnapshot::class.java) }
            .getOrNull()
            // Gson bypasses Kotlin defaults. Treat a malformed file as no backup.
            ?.takeIf { snapshot -> (snapshot.channels as List<*>?) != null }
    }

    companion object {
        const val FILE_NAME = "youtube_allowlist_backup.json"
    }
}

/** Restores the snapshot when the database opens with no channel decisions. */
class YoutubeAllowlistRestoreCallback(
    private val store: YoutubeAllowlistBackupStore,
) : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        runCatching { restoreIfEmpty(db) }
    }

    fun restoreIfEmpty(db: SupportSQLiteDatabase): Boolean {
        if (classificationCount(db) > 0) {
            // Existing installs get a first copy without waiting for a change.
            if (!store.exists()) store.save(readSnapshot(db))
            return false
        }
        val snapshot = store.load() ?: return false
        if (snapshot.channels.isEmpty()) return false
        db.beginTransaction()
        try {
            snapshot.channels.forEach { channel -> insert(db, channel) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return true
    }

    private fun insert(db: SupportSQLiteDatabase, channel: YoutubeAllowlistSnapshot.Channel) {
        db.execSQL(
            "INSERT OR IGNORE INTO youtube_v2_identity (channelId, handle, displayName, thumbnailUrl, resolvedAtMs) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(channel.channelId, channel.handle, channel.displayName, channel.thumbnailUrl, channel.resolvedAtMs),
        )
        db.execSQL(
            "INSERT OR IGNORE INTO youtube_v2_classification (channelId, classification, updatedAtMs) VALUES (?, ?, ?)",
            arrayOf<Any?>(channel.channelId, channel.classification, channel.classifiedAtMs),
        )
        channel.allowedAtMs?.let { addedAt ->
            db.execSQL(
                "INSERT OR IGNORE INTO youtube_v2_allowlist (channelId, addedAtMs) VALUES (?, ?)",
                arrayOf<Any?>(channel.channelId, addedAt),
            )
        }
        channel.aliases.forEach { alias ->
            db.execSQL(
                "INSERT OR IGNORE INTO youtube_v2_alias (aliasType, normalizedAlias, channelId, displayValue) VALUES (?, ?, ?, ?)",
                arrayOf<Any?>(alias.type, alias.normalized, channel.channelId, alias.display),
            )
        }
    }

    private fun classificationCount(db: SupportSQLiteDatabase): Long =
        db.query("SELECT COUNT(*) FROM youtube_v2_classification").use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }

    private fun readSnapshot(db: SupportSQLiteDatabase): YoutubeAllowlistSnapshot {
        val identities = db.query("SELECT channelId, handle, displayName, thumbnailUrl, resolvedAtMs FROM youtube_v2_identity").use { c ->
            buildList {
                while (c.moveToNext()) add(
                    YoutubeV2IdentityEntity(c.getString(0), c.getString(1), c.getString(2), if (c.isNull(3)) null else c.getString(3), c.getLong(4)),
                )
            }
        }
        val classifications = db.query("SELECT channelId, classification, updatedAtMs FROM youtube_v2_classification").use { c ->
            buildList { while (c.moveToNext()) add(YoutubeV2ClassificationEntity(c.getString(0), c.getString(1), c.getLong(2))) }
        }
        val allowlist = db.query("SELECT channelId, addedAtMs FROM youtube_v2_allowlist").use { c ->
            buildList { while (c.moveToNext()) add(YoutubeV2AllowlistEntity(c.getString(0), c.getLong(1))) }
        }
        val aliases = db.query("SELECT aliasType, normalizedAlias, channelId, displayValue FROM youtube_v2_alias").use { c ->
            buildList { while (c.moveToNext()) add(YoutubeV2AliasEntity(c.getString(0), c.getString(1), c.getString(2), c.getString(3))) }
        }
        return YoutubeAllowlistSnapshot.from(identities, classifications, allowlist, aliases)
    }
}
