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
import androidx.compose.foundation.layout.imePadding
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
        return "Admin denied for $who. On Render set ADMIN_EMAILS to this email, redeploy the server, then sign in again."
    }
    return raw ?: "Failed to send"
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
    ),
)

data class AdminDeepLinkOption(
    val label: String,
    val path: String,
)

private val deepLinkOptions = listOf(
    AdminDeepLinkOption("Home Dashboard", "safar://dashboard"),
    AdminDeepLinkOption("Exam Planner", "safar://study_planner"),
    AdminDeepLinkOption("Exam Planner - Create Plan", "safar://study_planner/create"),
    AdminDeepLinkOption("Exam Planner - Revision", "safar://study_planner?tab=revision"),
    AdminDeepLinkOption("Mehfil (Community)", "safar://mehfil"),
    AdminDeepLinkOption("Mehfil DM Chat", "safar://mehfil/dm_chat"),
    AdminDeepLinkOption("Study Circles", "safar://study_circles"),
    AdminDeepLinkOption("Leaderboard", "safar://leaderboard"),
    AdminDeepLinkOption("Nishtha (Goals & Streaks)", "safar://nishtha"),
    AdminDeepLinkOption("Nishtha Goals", "safar://nishtha/goals"),
    AdminDeepLinkOption("Nishtha Streaks", "safar://nishtha/streaks"),
    AdminDeepLinkOption("Nishtha Journal", "safar://nishtha/journal"),
    AdminDeepLinkOption("Nishtha Check-in", "safar://nishtha/checkin"),
    AdminDeepLinkOption("Nishtha Analytics", "safar://nishtha/analytics"),
    AdminDeepLinkOption("Ekagra Shield", "safar://focus_shield"),
    AdminDeepLinkOption("App Picker", "safar://ekagra/app_picker"),
    AdminDeepLinkOption("Kavach About", "safar://kavach/about"),
    AdminDeepLinkOption("Kavach App Categories", "safar://kavach/app_categories"),
    AdminDeepLinkOption("YouTube Study Mode", "safar://youtube_study_mode"),
    AdminDeepLinkOption("YouTube Study Analytics", "safar://youtube_study_mode/analytics"),
    AdminDeepLinkOption("Ekagra (Ekagra)", "safar://ekagra"),
    AdminDeepLinkOption("Dhyan (Mindfulness)", "safar://dhyan"),
    AdminDeepLinkOption("Courses", "safar://dhyan_courses"),
    AdminDeepLinkOption("Live Sessions", "safar://live/sessions"),
    AdminDeepLinkOption("Profile", "safar://profile"),
    AdminDeepLinkOption("Settings", "safar://settings"),
    AdminDeepLinkOption("Achievements", "safar://achievements"),
    AdminDeepLinkOption("Premium (Subscription)", "safar://premium"),
    AdminDeepLinkOption("Mehfil DM Paywall", "safar://premium/mehfil-dm"),
    AdminDeepLinkOption("Suggestions", "safar://suggestions"),
    AdminDeepLinkOption("Updates", "safar://updates"),
    AdminDeepLinkOption("100K Challenge", "safar://challenge-100k"),
    AdminDeepLinkOption("Admin Notifications", "safar://admin/notifications"),
)

enum class AdminSendKind {
    PUSH_TEST,
    PUSH_BROADCAST,
    BELL_TEST,
    BELL_INBOX,
}

data class AdminNotificationUiState(
    val selectedTriggerId: String = triggerOptions.first().id,
    val title: String = "",
    val body: String = "",
    val deepLink: String = "",
    val isSending: Boolean = false,
    val lastError: String? = null,
    val lastSuccessMessage: String? = null,
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

    fun clearSuccessMessage() = _uiState.update { it.copy(lastSuccessMessage = null) }

    private fun requireMessageFields(state: AdminNotificationUiState): Boolean {
        if (state.title.isBlank() || state.body.isBlank() || state.deepLink.isBlank()) {
            _uiState.update { it.copy(lastError = "Title, body, and deep link are required.") }
            return false
        }
        return true
    }

    private fun requestPayload(state: AdminNotificationUiState, trigger: AdminTriggerOption) =
        AdminBroadcastRequest(
            type = trigger.type,
            channel = trigger.channel,
            title = state.title.trim(),
            body = state.body.trim(),
            deepLink = state.deepLink.trim(),
            persistToInbox = false,
        )

    fun sendPushBroadcast() {
        val state = _uiState.value
        val trigger = triggerOptions.firstOrNull { it.id == state.selectedTriggerId } ?: triggerOptions.first()
        if (state.isSending) return
        if (!requireMessageFields(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, lastError = null, lastSuccessMessage = null) }
            val result = safeApiCall {
                notificationApi.sendAdminBroadcast(requestPayload(state, trigger))
            }
            when (result) {
                is Resource.Success -> {
                    val count = result.data.count ?: 0
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            lastSuccessMessage =
                                "Push broadcast started for $count device(s). Not shown in the Home bell.",
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

    fun sendTestPush() {
        val state = _uiState.value
        val trigger = triggerOptions.firstOrNull { it.id == state.selectedTriggerId } ?: triggerOptions.first()
        if (state.isSending) return
        if (!requireMessageFields(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, lastError = null, lastSuccessMessage = null) }
            val result = safeApiCall {
                notificationApi.sendTestNotification(requestPayload(state, trigger))
            }
            when (result) {
                is Resource.Success -> {
                    val delivered = result.data.results?.count { it.success } ?: 0
                    val firstFail = result.data.results?.firstOrNull { !it.success }
                    val err = if (delivered > 0) {
                        null
                    } else {
                        when (firstFail?.error) {
                            "preference_disabled" -> "Test not delivered: This notification type is disabled in your settings."
                            "quiet_hours" -> "Test not delivered: Blocked by your quiet hours settings."
                            "deduped" -> "Test not delivered: Blocked by deduplication. Wait a few minutes and try again."
                            "token_inactive" -> "Test not delivered: Device token is inactive/revoked in database."
                            null -> if (result.data.results.isNullOrEmpty()) {
                                "Test not delivered: No registered FCM token found for this account."
                            } else {
                                "Test not delivered. Check notification permissions and FCM token."
                            }
                            else -> "Test not delivered: ${firstFail.error}"
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            lastError = err,
                            lastSuccessMessage = if (err == null) {
                                "Test push sent to this device tray. Not shown in the Home bell."
                            } else {
                                null
                            },
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isSending = false, lastError = result.message ?: "Test push failed")
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun postToBellInbox() {
        val state = _uiState.value
        val trigger = triggerOptions.firstOrNull { it.id == state.selectedTriggerId } ?: triggerOptions.first()
        if (state.isSending) return
        if (!requireMessageFields(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, lastError = null, lastSuccessMessage = null) }
            val result = safeApiCall {
                notificationApi.postAdminInbox(requestPayload(state, trigger))
            }
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isSending = false,
                        lastSuccessMessage = "Posted to the Home bell for all users. No push was sent.",
                        title = trigger.defaultTitle,
                        body = trigger.defaultBody,
                        deepLink = trigger.defaultDeepLink,
                    )
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

    fun postTestToMyBell() {
        val state = _uiState.value
        val trigger = triggerOptions.firstOrNull { it.id == state.selectedTriggerId } ?: triggerOptions.first()
        if (state.isSending) return
        if (!requireMessageFields(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, lastError = null, lastSuccessMessage = null) }
            val result = safeApiCall {
                notificationApi.postAdminInbox(
                    requestPayload(state, trigger).copy(testOnly = true),
                )
            }
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isSending = false,
                        lastSuccessMessage =
                            "Bell test posted for your admin account only. Open Home → bell to see it.",
                    )
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
    var confirmKind by remember { mutableStateOf<AdminSendKind?>(null) }
    val selectedTrigger = triggerOptions.firstOrNull { it.id == uiState.selectedTriggerId } ?: triggerOptions.first()
    val canSubmit = uiState.title.isNotBlank() &&
        uiState.body.isNotBlank() &&
        uiState.deepLink.isNotBlank() &&
        !uiState.isSending
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uiState.lastError) {
        uiState.lastError?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(uiState.lastSuccessMessage) {
        uiState.lastSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSuccessMessage()
        }
    }

    when (confirmKind) {
        AdminSendKind.PUSH_BROADCAST -> AlertDialog(
            onDismissRequest = { if (!uiState.isSending) confirmKind = null },
            title = { Text("Confirm push broadcast") },
            text = {
                Text(
                    "Sends a tray/push notification to all active Android devices. " +
                        "It will not appear in the Home bell.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmKind = null
                        viewModel.sendPushBroadcast()
                    },
                    enabled = !uiState.isSending,
                ) {
                    Text(if (uiState.isSending) "Sending..." else "Send push")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmKind = null }, enabled = !uiState.isSending) {
                    Text("Cancel")
                }
            },
        )
        AdminSendKind.BELL_TEST -> Unit
        AdminSendKind.BELL_INBOX -> AlertDialog(
            onDismissRequest = { if (!uiState.isSending) confirmKind = null },
            title = { Text("Confirm in-app update") },
            text = {
                Text(
                    "Adds this message to every user's Home bell (Updates). " +
                        "No phone push will be sent. Prefer “Post test to my Home bell” first.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmKind = null
                        viewModel.postToBellInbox()
                    },
                    enabled = !uiState.isSending,
                ) {
                    Text(if (uiState.isSending) "Posting..." else "Post to bell")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmKind = null }, enabled = !uiState.isSending) {
                    Text("Cancel")
                }
            },
        )
        else -> Unit
    }

    SafarDrawerScaffold(
        title = "Admin Notifications",
        subtitle = "Notification Composer",
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
    ) { padding ->
        ComposerContent(
            padding = padding,
            uiState = uiState,
            canSubmit = canSubmit,
            selectedTrigger = selectedTrigger,
            onTitleChange = viewModel::onTitleChange,
            onBodyChange = viewModel::onBodyChange,
            onDeepLinkChange = viewModel::onDeepLinkChange,
            onTriggerChange = viewModel::onTriggerChange,
            onRefreshAdmin = viewModel::refreshAdminStatus,
            onSendTestPush = viewModel::sendTestPush,
            onConfirmPushBroadcast = { confirmKind = AdminSendKind.PUSH_BROADCAST },
            onPostBellTest = viewModel::postTestToMyBell,
            onConfirmBellPost = { confirmKind = AdminSendKind.BELL_INBOX },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposerContent(
    padding: PaddingValues,
    uiState: AdminNotificationUiState,
    canSubmit: Boolean,
    selectedTrigger: AdminTriggerOption,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onDeepLinkChange: (String) -> Unit,
    onTriggerChange: (String) -> Unit,
    onRefreshAdmin: () -> Unit,
    onSendTestPush: () -> Unit,
    onConfirmPushBroadcast: () -> Unit,
    onPostBellTest: () -> Unit,
    onConfirmBellPost: () -> Unit,
) {
    var triggerExpanded by remember { mutableStateOf(false) }
    var deepLinkExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Compose a notification",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Write the message once, then choose push (tray) or Home bell. Use both if you need both.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val adminLine = when (uiState.serverAdminGranted) {
            true -> "Server admin: yes (${uiState.userEmail ?: "unknown"})"
            false -> "Server admin: no (${uiState.userEmail ?: "unknown"}) — admin actions blocked until Render ADMIN_EMAILS includes this email."
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
                Text(
                    text = "Message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
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
                val currentDeepLink = deepLinkOptions.firstOrNull { it.path == uiState.deepLink }
                ExposedDropdownMenuBox(
                    expanded = deepLinkExpanded,
                    onExpandedChange = { deepLinkExpanded = !deepLinkExpanded },
                ) {
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
                        DropdownMenuItem(
                            text = { Text("Custom web link") },
                            onClick = {
                                deepLinkExpanded = false
                                onDeepLinkChange("")
                            },
                        )
                    }
                }
                if (currentDeepLink == null) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.deepLink,
                        onValueChange = onDeepLinkChange,
                        label = { Text("Custom web link") },
                        placeholder = { Text("https://example.com/page") },
                        supportingText = { Text("Only secure https links can be sent.") },
                        singleLine = true,
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "1. Normal push notification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Phone notification tray via FCM. Does not appear under the Home bell.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onSendTestPush,
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isSending) "Sending..." else "Send test push to this device")
                }
                Button(
                    onClick = onConfirmPushBroadcast,
                    enabled = canSubmit && uiState.serverAdminGranted != false,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isSending) "Sending..." else "Broadcast push to all devices")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "2. In-app bell (Updates)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Home bell only. Test posts to your admin account; post-for-all reaches every user.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onPostBellTest,
                    enabled = canSubmit && uiState.serverAdminGranted != false,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isSending) "Posting..." else "Post test to my Home bell")
                }
                Button(
                    onClick = onConfirmBellPost,
                    enabled = canSubmit && uiState.serverAdminGranted != false,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isSending) "Posting..." else "Post to Home bell for all users")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
