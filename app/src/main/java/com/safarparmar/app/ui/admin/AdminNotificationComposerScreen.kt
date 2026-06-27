package com.safarparmar.app.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.NotificationApi
import com.safarparmar.app.data.remote.dto.AdminBroadcastRequest
import com.safarparmar.app.domain.repository.AuthRepository
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.decodeIsAdminClaim
import com.safarparmar.app.util.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminTriggerOption(
    val id: String,
    val label: String,
    val type: String,
    val channel: String,
    val defaultTitle: String,
    val defaultBody: String,
    val defaultDeepLink: String,
)

private fun adminBroadcastErrorMessage(httpCode: Int?, raw: String?, email: String?): String {
    if (httpCode == 403 || raw?.contains("forbidden", ignoreCase = true) == true) {
        val who = email?.takeIf { it.isNotBlank() } ?: "your account"
        return "Admin denied for $who. On Render set ADMIN_EMAILS to this email, redeploy the server, then sign in again. Or use “Send test to this device” below."
    }
    return raw ?: "Failed to send broadcast"
}

private val triggerOptions = listOf(
    AdminTriggerOption(
        id = "announcements",
        label = "General Announcement",
        type = "announcements",
        channel = "announcements",
        defaultTitle = "SAFAR Update",
        defaultBody = "We have an important update for you.",
        defaultDeepLink = "safar://home",
    )
)

data class AdminDeepLinkOption(
    val label: String,
    val path: String
)

private val deepLinkOptions = listOf(
    AdminDeepLinkOption("Home Dashboard", "safar://dashboard"),
    AdminDeepLinkOption("Exam Planner", "safar://study_planner"),
    AdminDeepLinkOption("Mehfil (Community)", "safar://mehfil"),
    AdminDeepLinkOption("Mehfil DM Chat", "safar://mehfil/dm_chat"),
    AdminDeepLinkOption("Nishtha (Goals & Streaks)", "safar://nishtha"),
    AdminDeepLinkOption("Nishtha Goals", "safar://nishtha/goals"),
    AdminDeepLinkOption("Nishtha Streaks", "safar://nishtha/streaks"),
    AdminDeepLinkOption("Nishtha Journal", "safar://nishtha/journal"),
    AdminDeepLinkOption("Nishtha Check-in", "safar://nishtha/checkin"),
    AdminDeepLinkOption("Nishtha Analytics", "safar://nishtha/analytics"),
    AdminDeepLinkOption("Ekagra Shield", "safar://focus_shield"),
    AdminDeepLinkOption("App Picker", "safar://ekagra/app_picker"),
    AdminDeepLinkOption("Ekagra (Ekagra)", "safar://ekagra"),
    AdminDeepLinkOption("Dhyan (Mindfulness)", "safar://dhyan"),
    AdminDeepLinkOption("Live Sessions", "safar://live/sessions"),
    AdminDeepLinkOption("Profile", "safar://profile"),
    AdminDeepLinkOption("Settings", "safar://settings"),
    AdminDeepLinkOption("Achievements", "safar://achievements"),
    AdminDeepLinkOption("Admin Notifications", "safar://admin/notifications")
)

data class AdminNotificationUiState(
    val selectedTriggerId: String = triggerOptions.first().id,
    val title: String = "",
    val body: String = "",
    val deepLink: String = "",
    val isSending: Boolean = false,
    val lastError: String? = null,
    val sentCount: Int? = null,
    val userEmail: String? = null,
    val serverAdminGranted: Boolean? = null,
    val tokenAdminClaim: Boolean = false,
)

@HiltViewModel
class AdminNotificationComposerViewModel @Inject constructor(
    private val notificationApi: NotificationApi,
    private val authRepository: AuthRepository,
    private val dataStore: SafarDataStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AdminNotificationUiState().let { initial ->
            val trigger = triggerOptions.first()
            initial.copy(
                selectedTriggerId = trigger.id,
                title = trigger.defaultTitle,
                body = trigger.defaultBody,
                deepLink = trigger.defaultDeepLink,
            )
        },
    )
    val uiState: StateFlow<AdminNotificationUiState> = _uiState.asStateFlow()

    init {
        refreshAdminStatus()
    }

    fun refreshAdminStatus() {
        viewModelScope.launch {
            val email = dataStore.userEmail.first()
            val tokenAdmin = decodeIsAdminClaim(dataStore.authToken.first())
            _uiState.update { it.copy(userEmail = email, tokenAdminClaim = tokenAdmin) }
            when (val me = authRepository.getMe()) {
                is Resource.Success -> _uiState.update {
                    it.copy(serverAdminGranted = me.data.isAdmin, userEmail = me.data.email.ifBlank { email })
                }
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, lastError = null) }
    fun onBodyChange(value: String) = _uiState.update { it.copy(body = value, lastError = null) }
    fun onDeepLinkChange(value: String) = _uiState.update { it.copy(deepLink = value, lastError = null) }
    fun onTriggerChange(triggerId: String) {
        val trigger = triggerOptions.firstOrNull { it.id == triggerId } ?: return
        _uiState.update {
            it.copy(
                selectedTriggerId = trigger.id,
                title = trigger.defaultTitle,
                body = trigger.defaultBody,
                deepLink = trigger.defaultDeepLink,
                lastError = null,
            )
        }
    }

    fun sendBroadcast() {
        val state = _uiState.value
        val trigger = triggerOptions.firstOrNull { it.id == state.selectedTriggerId } ?: triggerOptions.first()
        if (state.isSending) return
        if (state.title.isBlank() || state.body.isBlank() || state.deepLink.isBlank()) {
            _uiState.update { it.copy(lastError = "Title, body, and deep link are required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, lastError = null, sentCount = null) }
            val result = safeApiCall {
                notificationApi.sendAdminBroadcast(
                    AdminBroadcastRequest(
                        type = trigger.type,
                        channel = trigger.channel,
                        title = state.title.trim(),
                        body = state.body.trim(),
                        deepLink = state.deepLink.trim(),
                    ),
                )
            }

            when (result) {
                is Resource.Success -> {
                    val delivered = result.data.results?.count { it.success } ?: 0
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            sentCount = delivered,
                            title = trigger.defaultTitle,
                            body = trigger.defaultBody,
                            deepLink = trigger.defaultDeepLink,
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(
                        isSending = false,
                        lastError = adminBroadcastErrorMessage(result.code, result.message, state.userEmail),
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun sendTestToThisDevice() {
        val state = _uiState.value
        val trigger = triggerOptions.firstOrNull { it.id == state.selectedTriggerId } ?: triggerOptions.first()
        if (state.isSending) return
        if (state.title.isBlank() || state.body.isBlank() || state.deepLink.isBlank()) {
            _uiState.update { it.copy(lastError = "Title, body, and deep link are required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, lastError = null, sentCount = null) }
            val result = safeApiCall {
                notificationApi.sendTestNotification(
                    AdminBroadcastRequest(
                        type = trigger.type,
                        channel = trigger.channel,
                        title = state.title.trim(),
                        body = state.body.trim(),
                        deepLink = state.deepLink.trim(),
                    ),
                )
            }

            when (result) {
                is Resource.Success -> {
                    val delivered = result.data.results?.count { it.success } ?: 0
                    val firstFail = result.data.results?.firstOrNull { !it.success }
                    val err = if (delivered > 0) null else {
                        when (firstFail?.error) {
                            "preference_disabled" -> "Test not delivered: This notification type is disabled in your settings."
                            "quiet_hours" -> "Test not delivered: Blocked by your quiet hours settings."
                            "deduped" -> "Test not delivered: Blocked by deduplication. Wait a few minutes and try again."
                            "token_inactive" -> "Test not delivered: Device token is inactive/revoked in database."
                            null -> if (result.data.results.isNullOrEmpty()) {
                                "Test not delivered: No registered FCM token found for this account. Ensure you are signed in and have granted permissions."
                            } else {
                                "Test not delivered. Check notification permissions and FCM token."
                            }
                            else -> "Test not delivered: ${firstFail.error}"
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            sentCount = delivered,
                            lastError = err,
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isSending = false, lastError = result.message ?: "Test notification failed")
                }
                is Resource.Loading -> Unit
            }
        }
    }
}

@Composable
fun AdminNotificationComposerScreen(
    currentRoute: String = Routes.ADMIN_NOTIFICATIONS,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    viewModel: AdminNotificationComposerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showConfirm by remember { mutableStateOf(false) }
    val selectedTrigger = triggerOptions.firstOrNull { it.id == uiState.selectedTriggerId } ?: triggerOptions.first()
    val canSubmit = uiState.title.isNotBlank() && uiState.body.isNotBlank() && uiState.deepLink.isNotBlank() && !uiState.isSending
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uiState.lastError) {
        uiState.lastError?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    LaunchedEffect(uiState.sentCount) {
        uiState.sentCount?.let { Toast.makeText(context, "Broadcast sent to $it device(s).", Toast.LENGTH_SHORT).show() }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isSending) showConfirm = false },
            title = { Text("Confirm broadcast") },
            text = { Text("This will send a push notification to all active Android devices. Proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        viewModel.sendBroadcast()
                    },
                    enabled = !uiState.isSending,
                ) {
                    Text(if (uiState.isSending) "Sending..." else "Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }, enabled = !uiState.isSending) {
                    Text("Cancel")
                }
            },
        )
    }

    SafarDrawerScaffold(
        title = "Admin Notifications",
        subtitle = "Broadcast Composer",
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
    ) { padding ->
        ComposerContent(
            padding = padding,
            uiState = uiState,
            canSubmit = canSubmit,
            onTitleChange = viewModel::onTitleChange,
            onBodyChange = viewModel::onBodyChange,
            onDeepLinkChange = viewModel::onDeepLinkChange,
            selectedTrigger = selectedTrigger,
            onTriggerChange = viewModel::onTriggerChange,
            onSendClick = { showConfirm = true },
            onSendTestClick = viewModel::sendTestToThisDevice,
            onRefreshAdmin = viewModel::refreshAdminStatus,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposerContent(
    padding: PaddingValues,
    uiState: AdminNotificationUiState,
    canSubmit: Boolean,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onDeepLinkChange: (String) -> Unit,
    selectedTrigger: AdminTriggerOption,
    onTriggerChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onSendTestClick: () -> Unit,
    onRefreshAdmin: () -> Unit,
) {
    var triggerExpanded by remember { mutableStateOf(false) }
    var deepLinkExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Compose a broadcast",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Visible only to admin users. Requires confirmation before sending.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Pick a predefined trigger, then edit the message content.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val adminLine = when (uiState.serverAdminGranted) {
            true -> "Server admin: yes (${uiState.userEmail ?: "unknown"})"
            false -> "Server admin: no (${uiState.userEmail ?: "unknown"}) — broadcast blocked until Render ADMIN_EMAILS includes this email."
            null -> "Checking server admin for ${uiState.userEmail ?: "…"}"
        }
        Text(
            text = adminLine,
            style = MaterialTheme.typography.bodySmall,
            color = if (uiState.serverAdminGranted == true) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
        TextButton(onClick = onRefreshAdmin) {
            Text("Refresh admin status")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = triggerExpanded,
                    onExpandedChange = { triggerExpanded = !triggerExpanded },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        value = selectedTrigger.label,
                        onValueChange = {},
                        label = { Text("Trigger") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = triggerExpanded)
                        },
                    )
                    ExposedDropdownMenu(
                        expanded = triggerExpanded,
                        onDismissRequest = { triggerExpanded = false },
                    ) {
                        triggerOptions.forEach { trigger ->
                            DropdownMenuItem(
                                text = { Text(trigger.label) },
                                onClick = {
                                    triggerExpanded = false
                                    onTriggerChange(trigger.id)
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.body,
                    onValueChange = onBodyChange,
                    label = { Text("Body") },
                    minLines = 4,
                )
                ExposedDropdownMenuBox(
                    expanded = deepLinkExpanded,
                    onExpandedChange = { deepLinkExpanded = !deepLinkExpanded },
                ) {
                    val currentDeepLink = deepLinkOptions.firstOrNull { it.path == uiState.deepLink }
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        value = currentDeepLink?.label ?: uiState.deepLink,
                        onValueChange = {},
                        label = { Text("Deep link") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = deepLinkExpanded)
                        },
                    )
                    ExposedDropdownMenu(
                        expanded = deepLinkExpanded,
                        onDismissRequest = { deepLinkExpanded = false },
                    ) {
                        deepLinkOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    deepLinkExpanded = false
                                    onDeepLinkChange(option.path)
                                },
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = onSendTestClick,
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isSending) "Sending..." else "Send test to this device")
        }
        Button(
            onClick = onSendClick,
            enabled = canSubmit && uiState.serverAdminGranted != false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isSending) "Sending..." else "Review & Send Broadcast")
        }
    }
}
