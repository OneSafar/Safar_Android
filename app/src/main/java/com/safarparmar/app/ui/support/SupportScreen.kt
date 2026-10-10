package com.safarparmar.app.ui.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit, viewModel: SupportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var previousId by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.support_talk_to_someone), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back))
                    }
                }
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { previousId = null; viewModel.refresh() }) {
                    Text(stringResource(R.string.support_current_request))
                }
                Box {
                    TextButton(onClick = { showHistory = true }, enabled = state.history.isNotEmpty()) {
                        Text(stringResource(R.string.support_previous_requests))
                    }
                    DropdownMenu(expanded = showHistory, onDismissRequest = { showHistory = false }) {
                        state.history.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${statusText(item.status)} · ${item.createdAt.take(10)}") },
                                onClick = { previousId = item.id; showHistory = false }
                            )
                        }
                    }
                }
            }
            val shown = state.history.find { it.id == previousId } ?: state.ticket
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.loadError -> Column(Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.support_load_error))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text(stringResource(R.string.common_try_again))
                    }
                }
                shown != null -> key(shown.id) {
                    ExistingRequest(
                        ticket = shown,
                        saving = state.saving,
                        escalate = viewModel::escalate,
                        cancel = viewModel::cancel,
                        feedback = { rating, comment -> viewModel.feedback(shown.id, rating, comment) },
                        modifier = Modifier
                    )
                }
                !state.enabled -> Unavailable(Modifier)
                else -> Intake(
                    saving = state.saving,
                    submit = { problem, topic, urgency, phone, time, contactMethod ->
                        viewModel.create(problem, topic, urgency, phone, time, contactMethod)
                    },
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
private fun Unavailable(modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.FavoriteBorder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.support_callbacks_closed), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.support_check_back_emergency), modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Intake(
    saving: Boolean,
    submit: (String, String, String, String, String, String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    var topic by remember { mutableStateOf("study_pressure") }
    var problem by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("soon") }
    var contactMethod by remember { mutableStateOf("phone") }
    var phone by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("evening") }

    val validPhone = phone.trim().matches(Regex("""^\+?[0-9 ()-]{8,20}$""")) && phone.filter { it.isDigit() }.length in 8..15

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Privacy Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.support_confidential_badge),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Text(stringResource(R.string.support_talk_to_someone), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.support_ask_volunteer), style = MaterialTheme.typography.bodyMedium)
        LearnMore(stringResource(R.string.support_process_explanation))

        // Topic Selection
        Section(stringResource(R.string.support_topic_title)) {
            ChoiceGrid(
                items = listOf(
                    "study_pressure" to stringResource(R.string.support_topic_study),
                    "feeling_low" to stringResource(R.string.support_topic_feeling_low),
                    "anxiety_stress" to stringResource(R.string.support_topic_anxiety),
                    "loneliness" to stringResource(R.string.support_topic_loneliness),
                    "family_relationships" to stringResource(R.string.support_topic_family),
                    "other" to stringResource(R.string.support_topic_other),
                ),
                selected = topic,
                onSelect = { topic = it }
            )
        }

        // What's on your mind?
        Section(stringResource(R.string.support_whats_going_on), stringResource(R.string.support_share_only_want)) {
            OutlinedTextField(
                value = problem,
                onValueChange = { problem = it.take(5000) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.support_share_only_want)) },
                minLines = 4,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Urgency
        Section(stringResource(R.string.support_how_soon)) {
            ChoiceGrid(
                items = listOf(
                    "not_urgent" to stringResource(R.string.support_can_wait),
                    "soon" to stringResource(R.string.support_soon),
                    "urgent" to stringResource(R.string.support_urgent),
                    "immediate" to stringResource(R.string.support_very_urgent),
                ),
                selected = urgency,
                onSelect = { urgency = it }
            )
        }

        // Crisis Helpline Banner if immediate
        if (urgency == "immediate") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.support_crisis_banner_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.support_crisis_banner_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:14416"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.support_call_telemanas), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Preferred contact method
        Section(stringResource(R.string.support_method_title)) {
            ChoiceGrid(
                items = listOf(
                    "phone" to stringResource(R.string.support_method_phone),
                    "google_meet" to stringResource(R.string.support_method_meet),
                ),
                selected = contactMethod,
                onSelect = { contactMethod = it }
            )
        }

        // Phone number
        Section(stringResource(R.string.support_phone_number), stringResource(R.string.support_phone_help)) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.take(30) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 98765 43210") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Time window
        Section(stringResource(R.string.support_best_time)) {
            ChoiceGrid(
                items = listOf(
                    "morning" to stringResource(R.string.support_morning),
                    "afternoon" to stringResource(R.string.support_afternoon),
                    "evening" to stringResource(R.string.support_evening),
                    "anytime" to stringResource(R.string.support_anytime),
                ),
                selected = time,
                onSelect = { time = it }
            )
        }

        Text(stringResource(R.string.support_emergency_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

        Button(
            onClick = { submit(problem, topic, urgency, phone, time, contactMethod) },
            enabled = !saving && problem.isNotBlank() && validPhone,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.support_send_request), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ExistingRequest(
    ticket: com.safarparmar.app.data.remote.api.SupportTicketDto,
    saving: Boolean,
    escalate: () -> Unit,
    cancel: () -> Unit,
    feedback: (Int, String) -> Unit,
    modifier: Modifier
) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var showCancelConfirm by remember { mutableStateOf(false) }

    val isOpen = ticket.status !in listOf("resolved", "closed", "cancelled")

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text(stringResource(R.string.support_cancel_request)) },
            text = { Text(stringResource(R.string.support_cancel_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirm = false
                    cancel()
                }) {
                    Text(stringResource(R.string.support_cancel_request), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text(stringResource(R.string.common_back))
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.support_request_status).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(statusText(ticket.status), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.support_preferred_callback, ticket.contactPreference), modifier = Modifier.padding(top = 8.dp))
            }
        }

        Text(stringResource(R.string.support_submitted, ticket.createdAt))
        Text(stringResource(if (ticket.status in listOf("resolved", "closed", "cancelled")) R.string.support_request_ended else R.string.support_request_received))

        if (isOpen && !ticket.escalated) {
            Button(onClick = escalate, enabled = !saving, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.support_need_help_sooner))
            }
        }
        if (ticket.escalated) {
            Text(stringResource(R.string.support_marked_urgent), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
        }

        if (isOpen) {
            OutlinedButton(
                onClick = { showCancelConfirm = true },
                enabled = !saving,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.support_cancel_request), color = MaterialTheme.colorScheme.error)
            }
        }

        if (ticket.status == "resolved" && ticket.feedback == null) {
            Text(stringResource(R.string.support_was_helpful), fontWeight = FontWeight.Bold)
            ChoiceGrid(
                items = (1..4).map { it.toString() to stringResource(R.string.support_rating_of_four, it) },
                selected = rating.toString(),
                onSelect = { rating = it.toInt() }
            )
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it.take(1000) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.support_optional_comment)) },
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = { feedback(rating, comment) },
                enabled = rating > 0 && !saving,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.support_send_feedback))
            }
        }
        if (ticket.feedback != null) {
            Text(stringResource(R.string.support_feedback_received), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Section(title: String, hint: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
        content()
    }
}

@Composable
private fun ChoiceGrid(items: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(selected = selected == value, role = Role.RadioButton, onClick = { onSelect(value) }),
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected == value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(14.dp),
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected == value) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun statusText(value: String) = stringResource(
    when (value) {
        "new" -> R.string.support_status_received
        "triaged" -> R.string.support_status_reviewed
        "assigned" -> R.string.support_status_assigned
        "in_progress" -> R.string.support_status_in_touch
        "resolved" -> R.string.support_status_resolved
        else -> R.string.support_status_closed
    }
)

@Composable
private fun LearnMore(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) R.string.support_show_less else R.string.support_learn_more))
        }
        if (expanded) Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}
