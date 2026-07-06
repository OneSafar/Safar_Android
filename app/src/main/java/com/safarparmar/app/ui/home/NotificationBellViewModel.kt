package com.safarparmar.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.NotificationApi
import com.safarparmar.app.domain.model.NotificationFeedItem
import com.safarparmar.app.domain.model.NotificationFeedSource

import com.safarparmar.app.util.Resource
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
                        )
                    }
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val merged = customItems.sortedByDescending { parseTimestamp(it.createdAt) }
            val lastSeenAt = dataStore.notificationBellLastSeenAt.first()
            val lastSeenInstant = lastSeenAt?.let { parseTimestamp(it) } ?: Instant.EPOCH
            val unreadCount = merged.count { parseTimestamp(it.createdAt).isAfter(lastSeenInstant) }
            val mappedItems = merged.map {
                it.copy(isUnread = parseTimestamp(it.createdAt).isAfter(lastSeenInstant))
            }

            _uiState.value = NotificationBellUiState(
                isLoading = false,
                items = mappedItems,
                unreadCount = unreadCount,
            )
        }
    }

    fun markAllRead() {
        val latest = _uiState.value.items.maxByOrNull { parseTimestamp(it.createdAt) } ?: return
        viewModelScope.launch {
            dataStore.setNotificationBellLastSeenAt(latest.createdAt)
            _uiState.value = _uiState.value.copy(unreadCount = 0)
        }
    }

    private fun parseTimestamp(raw: String): Instant = runCatching { Instant.parse(raw) }.getOrDefault(Instant.EPOCH)
}
