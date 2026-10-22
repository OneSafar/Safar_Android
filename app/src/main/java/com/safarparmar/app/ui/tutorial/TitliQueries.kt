package com.safarparmar.app.ui.tutorial

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TitliQueriesState(val loading: Boolean = true, val sending: Boolean = false, val signedIn: Boolean = false,
    val draft: String = "", val queries: List<TitliQueryDto> = emptyList(), val error: String? = null, val sent: Boolean = false)

@HiltViewModel
class TitliQueriesViewModel @Inject constructor(private val api: TitliApi, private val store: SafarDataStore,
    @ApplicationContext private val context: Context) : ViewModel() {
    private val mutable = MutableStateFlow(TitliQueriesState())
    val state = mutable.asStateFlow()
    private var owner: String? = null
    private var loadGeneration = 0
    private var requestId = UUID.randomUUID().toString()
    init { viewModelScope.launch {
        combine(store.userId, store.isLoggedIn) { id, loggedIn -> id?.takeIf { loggedIn && it.isNotBlank() } }
            .distinctUntilChanged().collectLatest { id ->
                owner = id; loadGeneration++; requestId = UUID.randomUUID().toString()
                mutable.value = TitliQueriesState(loading = id != null, signedIn = id != null)
                if (id != null) load(id)
            }
    } }
    fun edit(text: String) {
        if (state.value.sending) return
        if (text != state.value.draft) requestId = UUID.randomUUID().toString()
        mutable.update { it.copy(draft = text.take(2000), sent = false, error = null) }
    }
    fun refresh() { if (!state.value.sending) owner?.let { id -> viewModelScope.launch { load(id) } } }
    private suspend fun load(id: String) {
        if (owner != id) return
        val generation = ++loadGeneration
        mutable.update { it.copy(loading = true, error = null) }
        try {
            val rows = api.mine().queries
            if (owner == id && generation == loadGeneration) mutable.update { it.copy(loading = false, queries = rows) }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (owner == id && generation == loadGeneration) mutable.update { it.copy(loading = false, error = context.getString(R.string.titli_queries_load_error)) }
        }
    }
    fun submit() {
        val id = owner ?: return
        val message = state.value.draft.trim()
        if (state.value.sending || message.length !in 3..2000) return
        loadGeneration++ // An older inbox read must not replace a newly submitted query.
        mutable.update { it.copy(sending = true, loading = false, error = null, sent = false) }
        val stableRequestId = requestId
        viewModelScope.launch {
            try {
                val query = api.submit(TitliSubmitRequest(message, stableRequestId)).feedback
                if (owner == id) {
                    requestId = UUID.randomUUID().toString()
                    mutable.update { it.copy(sending = false, draft = "", sent = true, queries = listOf(query) + it.queries.filterNot { row -> row.id == query.id }) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (owner == id) mutable.update { it.copy(sending = false, error = context.getString(R.string.titli_query_send_error)) }
            }
        }
    }
}

@Composable
internal fun TitliQueriesScreen(onBack: () -> Unit, model: TitliQueriesViewModel = hiltViewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    TitliQueriesContent(state, model::edit, model::submit, model::refresh, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TitliQueriesContent(state: TitliQueriesState, onEdit: (String) -> Unit, onSubmit: () -> Unit,
    onRefresh: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.titli_queries_title)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back)) } },
        actions = { TitliGuideButton(); IconButton(onClick = onRefresh, enabled = !state.loading && !state.sending && state.signedIn) { Icon(Icons.Default.Refresh, stringResource(R.string.titli_refresh)) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.titli_query_hint), style = MaterialTheme.typography.bodyMedium)
            if (!state.signedIn && !state.loading) Text(stringResource(R.string.titli_sign_in))
            OutlinedTextField(value = state.draft, onValueChange = onEdit, enabled = state.signedIn && !state.sending,
                label = { Text(stringResource(R.string.titli_write_query)) }, minLines = 4, maxLines = 8,
                supportingText = { Text("${state.draft.length}/2000") }, modifier = Modifier.fillMaxWidth().testTag("titli.query.input"))
            Button(onClick = onSubmit, enabled = state.signedIn && !state.sending && state.draft.trim().length in 3..2000,
                modifier = Modifier.testTag("titli.query.submit")) {
                Text(stringResource(if (state.sending) R.string.titli_sending else R.string.titli_submit))
            }
            if (state.sent) Text(stringResource(R.string.titli_query_sent), color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("titli.query.sent"))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            HorizontalDivider()
            Text(stringResource(R.string.titli_your_queries), style = MaterialTheme.typography.titleMedium)
            if (state.loading) CircularProgressIndicator()
            if (!state.loading && state.queries.isEmpty()) Text(stringResource(R.string.titli_no_queries))
            state.queries.forEach { query ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(query.message, style = MaterialTheme.typography.bodyLarge)
                        query.reply?.let { reply ->
                            Text(stringResource(R.string.titli_reply_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(reply.message, modifier = Modifier.testTag("titli.query.reply.${query.id}"))
                        } ?: Text(stringResource(R.string.titli_waiting_reply), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
