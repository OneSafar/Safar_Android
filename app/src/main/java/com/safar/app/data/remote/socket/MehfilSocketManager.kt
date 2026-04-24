package com.safar.app.data.remote.socket

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.safar.app.BuildConfig
import com.safar.app.domain.model.MehfilPost
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class ThoughtsPayload(
    @SerializedName("thoughts") val thoughts: List<ThoughtDto>? = null,
    @SerializedName("room")     val room: String? = null,
    @SerializedName("page")     val page: Int = 1,
    @SerializedName("hasMore")  val hasMore: Boolean = false,
)

data class ThoughtDto(
    @SerializedName("_id")            val mongoId: String? = null,
    @SerializedName("id")             val id: String? = null,
    @SerializedName("content")        val content: String? = null,
    @SerializedName("space")          val space: String? = null,
    @SerializedName("category")       val category: String? = null,
    @SerializedName("isAnonymous")    val isAnonymous: Boolean? = null,
    @SerializedName("author")         val author: ThoughtAuthorDto? = null,
    // Flat fields returned by the API at the top level
    @SerializedName("authorName")     val authorName: String? = null,
    @SerializedName("authorAvatar")   val authorAvatar: String? = null,
    @SerializedName("userId")         val userId: String? = null,
    @SerializedName("createdAt")      val createdAt: String? = null,
    // API returns relatableCount and commentsCount
    @SerializedName("relatableCount") val relatableCount: Int? = null,
    @SerializedName("reactionCount")  val reactionCount: Int? = null,
    @SerializedName("commentsCount")  val commentsCount: Int? = null,
    @SerializedName("commentCount")   val commentCount: Int? = null,
    @SerializedName("hasReacted")     val hasReacted: Boolean? = null,
    @SerializedName("userLiked")      val userLiked: Boolean? = null,
)

data class ThoughtAuthorDto(
    @SerializedName("name")   val name: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
)

fun ThoughtDto.toDomain(): MehfilPost {
    // API may return name/avatar flat at top level OR nested under author
    val resolvedName   = authorName?.takeIf { it.isNotBlank() }
        ?: author?.name?.takeIf { it.isNotBlank() }
        ?: "Anonymous"
    val resolvedAvatar = authorAvatar ?: author?.avatar
    val resolvedUserId = userId ?: ""
    // Resolved display name: respect isAnonymous flag
    val displayName = if (isAnonymous == true) "Anonymous" else resolvedName
    // API uses relatableCount / commentsCount; fallback to reactionCount / commentCount
    val resolvedReactions = relatableCount ?: reactionCount ?: 0
    val resolvedComments  = commentsCount  ?: commentCount  ?: 0
    // API uses hasReacted; fallback to userLiked
    val resolvedLiked     = hasReacted ?: userLiked ?: false
    // space may come from category field
    val resolvedSpace     = space?.takeIf { it.isNotBlank() } ?: category ?: ""
    return MehfilPost(
        id            = id ?: mongoId ?: "",
        content       = content ?: "",
        space         = resolvedSpace,
        authorName    = displayName,
        authorAvatar  = resolvedAvatar,
        userId        = resolvedUserId,
        createdAt     = createdAt ?: "",
        reactionCount = resolvedReactions,
        commentCount  = resolvedComments,
        userLiked     = resolvedLiked,
    )
}

@Singleton
class MehfilSocketManager @Inject constructor(
    private val gson: Gson,
) {
    private var socket: Socket? = null
    private var pendingRoom: String = "ALL"

    // ALL flows declared FIRST — before any usage in connect()
    private val _onlineCount    = MutableStateFlow(0)
    val onlineCount = _onlineCount.asStateFlow()

    private val _thoughtsEvent  = MutableSharedFlow<ThoughtsPayload>(extraBufferCapacity = 8)
    val thoughtsEvent = _thoughtsEvent.asSharedFlow()

    private val _thoughtCreated = MutableSharedFlow<MehfilPost>(extraBufferCapacity = 8)
    val thoughtCreated = _thoughtCreated.asSharedFlow()

    private val _reactionUpdated = MutableSharedFlow<Triple<String, Int, Boolean>>(extraBufferCapacity = 8)
    val reactionUpdated = _reactionUpdated.asSharedFlow()

    // DM events
    data class DmEvent(val type: String, val fromUserId: String = "", val fromUserName: String = "", val roomId: String = "", val requestId: String = "", val message: String = "", val pendingList: List<String> = emptyList())
    private val _dmEvent = MutableSharedFlow<DmEvent>(extraBufferCapacity = 16)
    val dmEvent = _dmEvent.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected = _connected.asStateFlow()

    fun connect(token: String, userId: String, userName: String, userAvatar: String?, initialRoom: String = "ALL") {
        if (socket?.connected() == true) {
            joinRoomAndLoad(initialRoom)
            return
        }

        pendingRoom = initialRoom

        val serverRoot = BuildConfig.BASE_URL
            .removeSuffix("/")
            .let { if (it.endsWith("/api")) it.dropLast(4) else it }

        val opts = IO.Options.builder()
            .setPath("/socket.io")
            .setExtraHeaders(mapOf("Authorization" to listOf("Bearer $token")))
            .setAuth(mapOf("token" to token))
            .setReconnection(true)
            .setReconnectionAttempts(10)
            .setReconnectionDelay(2000L)
            .build()

        try {
            android.util.Log.d("MehfilSocket", "Connecting to $serverRoot/mehfil  user=$userId  room=$initialRoom")
            socket = IO.socket(URI.create("$serverRoot/mehfil"), opts).apply {

                on(Socket.EVENT_CONNECT) {
                    _connected.tryEmit(true)
                    android.util.Log.d("MehfilSocket", "Connected ✓  emitting register → joinRoom($pendingRoom) → loadThoughts")
                    val reg = JSONObject().apply {
                        put("id", userId)
                        put("name", userName)
                        put("avatar", userAvatar ?: "")
                    }
                    emit("register", reg)
                    val room = pendingRoom
                    emit("joinRoom", JSONObject().put("room", room))
                    emit("loadThoughts", JSONObject().apply {
                        put("page", 1)
                        put("limit", 50)
                        put("room", room)
                    })
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    _connected.tryEmit(false)
                    android.util.Log.e("MehfilSocket", "Connect error: ${args.firstOrNull()}")
                }
                on(Socket.EVENT_DISCONNECT) { args ->
                    _connected.tryEmit(false)
                    android.util.Log.w("MehfilSocket", "Disconnected: ${args.firstOrNull()}")
                }

                on("onlineCount") { args ->
                    try {
                        val count = when (val raw = args.firstOrNull()) {
                            is Int  -> raw
                            is Long -> raw.toInt()
                            else    -> raw?.toString()?.toIntOrNull() ?: 0
                        }
                        android.util.Log.d("MehfilSocket", "onlineCount → $count")
                        _onlineCount.tryEmit(count)
                    } catch (_: Exception) {}
                }

                on("thoughts") { args ->
                    try {
                        val raw = args.firstOrNull()?.toString() ?: return@on
                        val payload = gson.fromJson(raw, ThoughtsPayload::class.java)
                        android.util.Log.d("MehfilSocket", "thoughts ← page=${payload.page}  count=${payload.thoughts?.size ?: 0}  hasMore=${payload.hasMore}")
                        _thoughtsEvent.tryEmit(payload)
                    } catch (_: Exception) {}
                }

                on("thoughtCreated") { args ->
                    try {
                        val raw = args.firstOrNull()?.toString() ?: return@on
                        val dto = gson.fromJson(raw, ThoughtDto::class.java)
                        android.util.Log.d("MehfilSocket", "thoughtCreated ← id=${dto.id ?: dto.mongoId}")
                        _thoughtCreated.tryEmit(dto.toDomain())
                    } catch (_: Exception) {}
                }

                on("reactionUpdated") { args ->
                    try {
                        val raw = args.firstOrNull()?.toString() ?: return@on
                        val obj = JSONObject(raw)
                        val thoughtId     = obj.optString("thoughtId")
                        // API emits relatableCount; fallback to reactionCount
                        val reactionCount = if (obj.has("relatableCount")) obj.optInt("relatableCount") else obj.optInt("reactionCount")
                        // API emits hasReacted; fallback to userLiked
                        val userLiked     = if (obj.has("hasReacted")) obj.optBoolean("hasReacted") else obj.optBoolean("userLiked")
                        android.util.Log.d("MehfilSocket", "reactionUpdated ← thoughtId=$thoughtId  count=$reactionCount")
                        _reactionUpdated.tryEmit(Triple(thoughtId, reactionCount, userLiked))
                    } catch (_: Exception) {}
                }

                // DM events
                on("dm:request_sent") { args ->
                    try {
                        val raw = args.firstOrNull()?.toString() ?: return@on
                        val obj = JSONObject(raw)
                        val toUserId  = obj.optString("toUserId")
                        val requestId = obj.optString("requestId")
                        val queued    = obj.optBoolean("queued", false)
                        android.util.Log.d("MehfilSocket", "dm:request_sent ← toUserId=$toUserId requestId=$requestId queued=$queued")
                        _dmEvent.tryEmit(DmEvent("request_sent", fromUserId = toUserId, message = requestId))
                    } catch (_: Exception) {}
                }
                on("dm:incoming_request") { args ->
                    try {
                        val raw = args.firstOrNull()?.toString() ?: return@on
                        val obj = JSONObject(raw)
                        val fromUserId   = obj.optString("fromUserId")
                        val fromUserName = obj.optString("fromUserName").ifBlank { fromUserId }
                        val requestId    = obj.optString("requestId")
                        android.util.Log.d("MehfilSocket", "dm:incoming_request ← from=$fromUserId name=$fromUserName requestId=$requestId")
                        _dmEvent.tryEmit(DmEvent("incoming_request", fromUserId = fromUserId, fromUserName = fromUserName, requestId = requestId))
                    } catch (_: Exception) {}
                }
                on("dm:opened") { args ->
                    try {
                        val raw = args.firstOrNull()?.toString() ?: return@on
                        val obj = JSONObject(raw)
                        val roomId        = obj.optString("roomId")
                        val otherUserId   = obj.optString("otherUserId")
                        val otherUserName = obj.optString("otherUserName").ifBlank { otherUserId }
                        android.util.Log.d("MehfilSocket", "dm:opened ← roomId=$roomId other=$otherUserName")
                        _dmEvent.tryEmit(DmEvent("opened", fromUserId = otherUserId, fromUserName = otherUserName, roomId = roomId))
                    } catch (_: Exception) {}
                }
                on("dm:sync_pending") { args ->
                    try {
                        val raw = args.firstOrNull()?.toString() ?: return@on
                        val arr = JSONObject(raw).optJSONArray("pending")
                        val list = mutableListOf<String>()
                        if (arr != null) for (i in 0 until arr.length()) list.add(arr.optString(i))
                        android.util.Log.d("MehfilSocket", "dm:sync_pending ← ${list.size} pending")
                        _dmEvent.tryEmit(DmEvent("sync_pending", pendingList = list))
                    } catch (_: Exception) {}
                }
                on("dm:accepted") { args ->
                    try {
                        val raw = args.firstOrNull()?.toString() ?: return@on
                        val obj           = JSONObject(raw)
                        // Server sends otherUserId/otherUserName/roomId on the requester side
                        val otherUserId   = obj.optString("otherUserId").ifBlank { obj.optString("fromUserId") }
                        val otherUserName = obj.optString("otherUserName").ifBlank { otherUserId }
                        val roomId        = obj.optString("roomId")
                        android.util.Log.d("MehfilSocket", "dm:accepted ← other=$otherUserId name=$otherUserName roomId=$roomId")
                        _dmEvent.tryEmit(DmEvent("accepted", fromUserId = otherUserId, fromUserName = otherUserName, roomId = roomId))
                    } catch (_: Exception) {}
                }
                on("dm:declined") { args ->
                    try {
                        val raw = args.firstOrNull()?.toString() ?: return@on
                        val fromUserId = JSONObject(raw).optString("fromUserId")
                        _dmEvent.tryEmit(DmEvent("declined", fromUserId = fromUserId))
                    } catch (_: Exception) {}
                }
                on("dm:message") { args ->
                    try {
                        val raw = args.firstOrNull()?.toString() ?: return@on
                        val obj = JSONObject(raw)
                        val fromUserId   = obj.optString("fromUserId")
                        val fromUserName = obj.optString("fromUserName").ifBlank { fromUserId }
                        val roomId       = obj.optString("roomId")
                        // Server sends "text", fallback to "message" for safety
                        val message = obj.optString("text").ifBlank { obj.optString("message") }
                        android.util.Log.d("MehfilSocket", "dm:message ← from=$fromUserId name=$fromUserName roomId=$roomId text=$message")
                        _dmEvent.tryEmit(DmEvent("message", fromUserId = fromUserId, fromUserName = fromUserName, roomId = roomId, message = message))
                    } catch (_: Exception) {}
                }
                on("dm:error") { args ->
                    try {
                        val raw = args.firstOrNull()?.toString() ?: return@on
                        val msg = JSONObject(raw).optString("message")
                        android.util.Log.w("MehfilSocket", "dm:error ← $msg")
                        _dmEvent.tryEmit(DmEvent("error", message = msg))
                    } catch (_: Exception) {}
                }

                connect()
            }
            // Ask server for any pending DM requests on (re)connect
            socket?.on(Socket.EVENT_CONNECT) {
                socket?.emit("dm:sync_pending", JSONObject())
            }
        } catch (e: Exception) {
            android.util.Log.e("MehfilSocket", "Socket init failed: ${e.message}")
            _connected.tryEmit(false)
        }
    }

    fun joinRoomAndLoad(room: String, limit: Int = 50) {
        pendingRoom = room
        val s = socket ?: return
        if (!s.connected()) return
        android.util.Log.d("MehfilSocket", "joinRoomAndLoad → room=$room")
        s.emit("joinRoom", JSONObject().put("room", room))
        s.emit("loadThoughts", JSONObject().apply {
            put("page", 1)
            put("limit", limit)
            put("room", room)
        })
    }

    fun loadThoughts(room: String, page: Int, limit: Int = 50) {
        val s = socket ?: return
        if (!s.connected()) return
        s.emit("loadThoughts", JSONObject().apply {
            put("page", page)
            put("limit", limit)
            put("room", room)
        })
    }

    fun emitNewThought(content: String, room: String, isAnonymous: Boolean = false) {
        val s = socket ?: return
        if (!s.connected()) return
        s.emit("newThought", JSONObject().apply {
            put("content", content)
            put("room", room)
            put("isAnonymous", isAnonymous)
        })
    }

    fun emitToggleReaction(thoughtId: String) {
        val s = socket ?: return
        if (!s.connected()) return
        s.emit("toggleReaction", JSONObject().put("thoughtId", thoughtId))
    }

    fun emitDmRequest(targetUserId: String, contextPostId: String = "", contextPreview: String = "") {
        val s = socket ?: return
        if (!s.connected()) return
        android.util.Log.d("MehfilSocket", "dm:request → toUserId=$targetUserId")
        val payload = JSONObject().put("toUserId", targetUserId)
        if (contextPostId.isNotEmpty()) {
            payload.put("context", JSONObject().apply {
                put("type", "post")
                put("id", contextPostId)
                put("preview", contextPreview.take(60))
            })
        }
        s.emit("dm:request", payload)
    }

    fun emitDmMessage(roomId: String, text: String) {
        val s = socket ?: return
        if (!s.connected()) return
        android.util.Log.d("MehfilSocket", "dm:message → roomId=$roomId text=$text")
        s.emit("dm:message", JSONObject().apply {
            put("roomId", roomId)
            put("text", text)
        })
    }

    fun emitDmAccept(requestId: String) {
        val s = socket ?: return
        if (!s.connected()) return
        android.util.Log.d("MehfilSocket", "dm:accept → requestId=$requestId")
        s.emit("dm:accept", JSONObject().put("requestId", requestId))
    }

    fun emitDmLeaveRoom(roomId: String) {
        val s = socket ?: return
        if (!s.connected()) return
        android.util.Log.d("MehfilSocket", "dm:leave_room → roomId=$roomId")
        s.emit("dm:leave_room", JSONObject().put("roomId", roomId))
    }

    fun emitDmDecline(fromUserId: String) {
        val s = socket ?: return
        if (!s.connected()) return
        s.emit("dm:decline", JSONObject().put("fromUserId", fromUserId))
    }

    fun isConnected() = socket?.connected() == true

    fun disconnect() {
        android.util.Log.d("MehfilSocket", "disconnect()")
        socket?.disconnect()
        socket = null
        _connected.tryEmit(false)
    }
}
