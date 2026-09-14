package com.houshmandhesab.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.houshmandhesab.app.R
import com.houshmandhesab.app.ai.AiRepository
import com.houshmandhesab.app.ai.ChatMessage
import com.houshmandhesab.app.data.repo.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUi(
    val fromUser: Boolean,
    val text: String,
    val loading: Boolean = false,
    val error: Boolean = false
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val ai: AiRepository,
    val wallet: WalletRepository
) : ViewModel() {

    val configured = ai.configured
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _messages = MutableStateFlow<List<ChatUi>>(emptyList())
    val messages = _messages.asStateFlow()

    private var history = mutableListOf<ChatMessage>()

    fun send(text: String) {
        if (text.isBlank()) return
        val userMsg = text.trim()
        _messages.value = _messages.value + ChatUi(fromUser = true, text = userMsg)
        _messages.value = _messages.value + ChatUi(fromUser = false, text = "", loading = true)
        viewModelScope.launch {
            val result = ai.chat(history, userMsg)
            _messages.value = _messages.value.dropLast(1)
            result.onSuccess { reply ->
                _messages.value = _messages.value + ChatUi(fromUser = false, text = reply)
                history.add(ChatMessage("user", userMsg))
                history.add(ChatMessage("assistant", reply))
            }.onFailure {
                _messages.value = _messages.value + ChatUi(fromUser = false, text = "", error = true)
            }
        }
    }
}

@Composable
fun AiScreen(
    onNavigate: (String) -> Unit,
    viewModel: AiViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val configured by viewModel.configured.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.ai_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (!configured) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    stringResource(R.string.ai_needs_setup),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Button(onClick = { onNavigate("settings") }) {
                    Text(stringResource(R.string.ai_setup_now))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                R.string.ai_quick_analysis,
                R.string.ai_quick_saving,
                R.string.ai_quick_waste
            ).forEach { res ->
                androidx.compose.material3.AssistChip(
                    onClick = { viewModel.send(stringResource(res)) },
                    label = {
                        Text(stringResource(res), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.ai_greeting),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(stringResource(R.string.ai_hint_message)) },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors()
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.send(input)
                    input = ""
                },
                enabled = input.isNotBlank() && configured,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(R.string.ai_send),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatUi) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.fromUser) Arrangement.Start else Arrangement.End
    ) {
        when {
            msg.loading -> Box(
                Modifier
                    .widthIn(min = 60.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            msg.error -> Box(
                Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                    )
                    .padding(14.dp)
            ) {
                Text(
                    stringResource(R.string.ai_error),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            else -> Box(
                Modifier
                    .widthIn(max = 300.dp)
                    .background(
                        if (msg.fromUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(
                            if (msg.fromUser) 18.dp else 18.dp,
                            if (msg.fromUser) 4.dp else 18.dp,
                            if (msg.fromUser) 18.dp else 4.dp,
                            18.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Text(
                    msg.text,
                    color = if (msg.fromUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
