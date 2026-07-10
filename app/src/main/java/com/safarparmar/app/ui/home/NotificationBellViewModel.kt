package com.safarparmar.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.NotificationApi
import com.safarparmar.app.domain.model.AnnouncementType
import com.safarparmar.app.domain.model.NotificationFeedItem
import com.safarparmar.app.domain.model.NotificationFeedSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationBellUiState(
    val isLoading: Boolean = false,
    val items: List<NotificationFeedItem> = emptyList(),
    val unreadCount: Int = 0,
)

@HiltViewModel
class NotificationBellViewModel @Inject constructor(
    private val notificationApi: NotificationApi,
    private val dataStore: SafarDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationBellUiState())
    val uiState = _uiState.asStateFlow()
    private val dismissedIds = mutableSetOf<String>()
    private val readIds = mutableSetOf<String>()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val customItems = try {
                val response = notificationApi.getNotificationHistory()
                if (response.isSuccessful) {
                    response.body()?.notifications.orEmpty().mapNotNull { dto ->
                        val id = dto.id ?: return@mapNotNull null
                        NotificationFeedItem(
                            id = "custom_$id",
                            source = NotificationFeedSource.CUSTOM,
                            title = dto.title ?: "",
                            body = dto.body ?: "",
                            createdAt = dto.createdAt ?: "",
                            deepLink = dto.deepLink,
                            type = announcementTypeFor(
                                title = dto.title.orEmpty(),
                                body = dto.body.orEmpty(),
                            ),
                        )
                    }
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val merged = customItems
                .filterNot { it.id in dismissedIds }
                .sortedByDescending { parseTimestamp(it.createdAt) }
            val lastSeenAt = dataStore.notificationBellLastSeenAt.first()
            val lastSeenInstant = lastSeenAt?.let { parseTimestamp(it) } ?: Instant.EPOCH
            val mappedItems = merged.map {
                it.copy(
                    isUnread = it.id !in readIds &&
                        parseTimestamp(it.createdAt).isAfter(lastSeenInstant)
                )
            }

            _uiState.value = NotificationBellUiState(
                isLoading = false,
                items = mappedItems,
                unreadCount = mappedItems.count(NotificationFeedItem::isUnread),
            )
        }
    }

    fun markAllRead() {
        val latest = _uiState.value.items.maxByOrNull { parseTimestamp(it.createdAt) } ?: return
        viewModelScope.launch {
            dataStore.setNotificationBellLastSeenAt(latest.createdAt)
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items.map { it.copy(isUnread = false) },
                unreadCount = 0,
            )
        }
    }

    fun markAsRead(id: String) {
        readIds += id
        _uiState.value = _uiState.value.let { state ->
            val updatedItems = state.items.map { item ->
                if (item.id == id) item.copy(isUnread = false) else item
            }
            state.copy(
                items = updatedItems,
                unreadCount = updatedItems.count(NotificationFeedItem::isUnread),
            )
        }
    }

    fun dismiss(id: String) {
        dismissedIds += id
        _uiState.value = _uiState.value.let { state ->
            val updatedItems = state.items.filterNot { it.id == id }
            state.copy(
                items = updatedItems,
                unreadCount = updatedItems.count(NotificationFeedItem::isUnread),
            )
        }
    }

    private fun announcementTypeFor(title: String, body: String): AnnouncementType {
        val text = "$title $body".lowercase()
        return when {
            listOf("new version", "app update", "update available", "what's new")
                .any(text::contains) -> AnnouncementType.APP_UPDATE
            listOf("maintenance", "downtime", "service interruption")
                .any(text::contains) -> AnnouncementType.MAINTENANCE
            else -> AnnouncementType.GENERAL
        }
    }

    private fun parseTimestamp(raw: String): Instant = runCatching { Instant.parse(raw) }.getOrDefault(Instant.EPOCH)
}
