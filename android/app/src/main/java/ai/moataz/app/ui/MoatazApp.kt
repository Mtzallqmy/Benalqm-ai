package ai.moataz.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.moataz.app.AppTab
import ai.moataz.app.BuildConfig
import ai.moataz.app.MoatazUiState
import ai.moataz.app.MoatazViewModel
import ai.moataz.app.R
import ai.moataz.app.ServerUrlPolicy
import ai.moataz.app.data.ChatMessage
import ai.moataz.app.data.ConnectionState
import ai.moataz.app.data.FlowEvent
import ai.moataz.app.data.FlowLevel
import ai.moataz.app.data.MessageRole
import ai.moataz.app.data.ThreadSummary
import ai.moataz.app.ui.theme.MoatazAmber
import ai.moataz.app.ui.theme.MoatazCyan
import ai.moataz.app.ui.theme.MoatazDanger
import ai.moataz.app.ui.theme.MoatazIndigo
import ai.moataz.app.ui.theme.MoatazMint
import ai.moataz.app.ui.theme.MoatazTextMuted
import ai.moataz.app.ui.theme.MoatazViolet
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MoatazApp(viewModel: MoatazViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MoatazNavigationBar(
                selected = state.tab,
                onSelected = viewModel::selectTab,
            )
        },
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.035f),
                        ),
                    ),
                ),
        ) {
            AnimatedContent(
                targetState = state.tab,
                label = "moataz-main-navigation",
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
                when (tab) {
                    AppTab.Chat -> ChatScreen(state, viewModel)
                    AppTab.Threads -> ThreadsScreen(state, viewModel)
                    AppTab.Flow -> FlowScreen(state, viewModel)
                    AppTab.Settings -> SettingsScreen(state, viewModel)
                }
            }

            if (state.error != null) {
                ErrorBanner(
                    message = state.error,
                    onDismiss = viewModel::dismissError,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .safeDrawingPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun MoatazNavigationBar(selected: AppTab, onSelected: (AppTab) -> Unit) {
    val items = listOf(
        AppTab.Chat to R.string.nav_chat,
        AppTab.Threads to R.string.nav_threads,
        AppTab.Flow to R.string.nav_flow,
        AppTab.Settings to R.string.nav_settings,
    )
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
    ) {
        items.forEach { (tab, labelId) ->
            val label = stringResource(labelId)
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                icon = {
                    MoatazNavGlyph(
                        tab = tab,
                        selected = selected == tab,
                        contentDescription = label,
                    )
                },
                label = { Text(label, maxLines = 1, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun MoatazNavGlyph(tab: AppTab, selected: Boolean, contentDescription: String) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val stroke = Stroke(width = if (selected) 2.35.dp.toPx() else 1.9.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (tab) {
            AppTab.Chat -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, h * 0.16f),
                    size = Size(w * 0.76f, h * 0.58f),
                    cornerRadius = CornerRadius(w * 0.16f),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.33f, h * 0.74f), Offset(w * 0.21f, h * 0.88f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.33f, h * 0.74f), Offset(w * 0.47f, h * 0.74f), stroke.width, StrokeCap.Round)
            }
            AppTab.Threads -> {
                listOf(0.28f, 0.5f, 0.72f).forEach { y ->
                    drawCircle(color, radius = w * 0.055f, center = Offset(w * 0.18f, h * y))
                    drawLine(color, Offset(w * 0.32f, h * y), Offset(w * 0.84f, h * y), stroke.width, StrokeCap.Round)
                }
            }
            AppTab.Flow -> {
                val a = Offset(w * 0.18f, h * 0.28f)
                val b = Offset(w * 0.52f, h * 0.5f)
                val c = Offset(w * 0.82f, h * 0.24f)
                val d = Offset(w * 0.78f, h * 0.78f)
                drawLine(color, a, b, stroke.width, StrokeCap.Round)
                drawLine(color, b, c, stroke.width, StrokeCap.Round)
                drawLine(color, b, d, stroke.width, StrokeCap.Round)
                listOf(a, b, c, d).forEach { drawCircle(color, radius = w * 0.09f, center = it, style = stroke) }
            }
            AppTab.Settings -> {
                drawCircle(color, radius = w * 0.19f, center = center, style = stroke)
                drawCircle(color, radius = w * 0.38f, center = center, style = stroke)
                repeat(8) { index ->
                    val angle = Math.toRadians(index * 45.0)
                    val p1 = Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * w * 0.38f,
                        center.y + kotlin.math.sin(angle).toFloat() * h * 0.38f,
                    )
                    val p2 = Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * w * 0.47f,
                        center.y + kotlin.math.sin(angle).toFloat() * h * 0.47f,
                    )
                    drawLine(color, p1, p2, stroke.width, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(state: MoatazUiState, viewModel: MoatazViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        ChatHeader(state, viewModel)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

        when {
            !state.configured -> ConnectionEmptyState(viewModel)
            state.connection != ConnectionState.Connected -> ConnectionPendingState(state, viewModel)
            state.loadingMessages -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
            else -> ConversationBody(state, viewModel, Modifier.weight(1f))
        }

        if (state.configured && state.connection == ConnectionState.Connected) {
            Composer(state, viewModel)
        }
    }
}

@Composable
private fun ChatHeader(state: MoatazUiState, viewModel: MoatazViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(38.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("moataz ai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(state.connection)
                Spacer(Modifier.width(6.dp))
                Text(
                    connectionLabel(state.connection),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.latencyMs?.let {
                    Text(
                        " · ${it}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (state.connection == ConnectionState.Connected) {
            ModelPicker(state, viewModel)
        }
    }
}

@Composable
private fun ModelPicker(state: MoatazUiState, viewModel: MoatazViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                state.models.firstOrNull { it.name == state.selectedModel }?.displayName
                    ?: stringResource(R.string.default_model),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 132.dp),
            )
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (state.models.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.no_models)) },
                    onClick = { expanded = false },
                    enabled = false,
                )
            } else {
                state.models.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(model.displayName, fontWeight = FontWeight.Medium)
                                if (model.model != model.displayName) {
                                    Text(
                                        model.model,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        },
                        onClick = {
                            viewModel.selectModel(model.name)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationBody(state: MoatazUiState, viewModel: MoatazViewModel, modifier: Modifier = Modifier) {
    if (state.messages.isEmpty()) {
        WelcomePanel(state, viewModel, modifier)
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.sending) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.messages, key = { it.id }) { message ->
            MessageBubble(message)
        }
        if (state.sending) {
            item(key = "native-stream-state") {
                RunProgressCard(state)
            }
        }
    }
}

@Composable
private fun WelcomePanel(state: MoatazUiState, viewModel: MoatazViewModel, modifier: Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            BrandMark(72.dp)
            Spacer(Modifier.height(22.dp))
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.welcome_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(24.dp))
            if (state.activeThreadId == null) {
                OutlinedButton(onClick = viewModel::createThread) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.new_conversation))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.User
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when (message.role) {
        MessageRole.User -> MaterialTheme.colorScheme.primary
        MessageRole.Assistant -> MaterialTheme.colorScheme.surfaceVariant
        MessageRole.Tool -> MoatazCyan.copy(alpha = 0.11f)
        MessageRole.System -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isUser) 22.dp else 7.dp,
                        bottomEnd = if (isUser) 7.dp else 22.dp,
                    ),
                )
                .background(bubbleColor)
                .then(
                    if (!isUser) Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                        RoundedCornerShape(22.dp),
                    ) else Modifier,
                )
                .padding(horizontal = 15.dp, vertical = 12.dp),
        ) {
            if (message.role == MessageRole.Tool) {
                Text(
                    message.name ?: "Tool",
                    style = MaterialTheme.typography.labelLarge,
                    color = MoatazCyan,
                )
                Spacer(Modifier.height(4.dp))
            }
            if (message.text.isNotBlank()) {
                SelectionContainer {
                    Text(message.text, color = textColor, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (message.toolCalls.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.tool_calls, message.toolCalls.joinToString(" · ")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) textColor.copy(alpha = 0.78f) else MoatazCyan,
                )
            }
        }
    }
}

@Composable
private fun RunProgressCard(state: MoatazUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.telemetry.phase.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "${state.telemetry.eventCount} SSE events${state.activeRunId?.let { " · ${it.take(8)}" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Composer(state: MoatazUiState, viewModel: MoatazViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.setThinkingEnabled(!state.thinkingEnabled) }
                    .background(
                        if (state.thinkingEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (state.thinkingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    stringResource(R.string.thinking),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.thinkingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.sending) {
                Spacer(Modifier.width(8.dp))
                Text(
                    state.telemetry.phase,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoatazCyan,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = state.composer,
                onValueChange = viewModel::updateComposer,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.composer_hint)) },
                minLines = 1,
                maxLines = 6,
                shape = RoundedCornerShape(22.dp),
                enabled = !state.sending,
            )
            Spacer(Modifier.width(9.dp))
            FilledIconButton(
                onClick = if (state.sending) viewModel::stopRun else viewModel::sendMessage,
                enabled = state.sending || state.composer.isNotBlank(),
                modifier = Modifier.size(52.dp),
                colors = ButtonDefaults.filledIconButtonColors(
                    containerColor = if (state.sending) MoatazDanger else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    if (state.sending) Icons.Rounded.StopCircle else Icons.Rounded.Send,
                    contentDescription = stringResource(if (state.sending) R.string.stop else R.string.send),
                )
            }
        }
    }
}

@Composable
private fun ConnectionEmptyState(viewModel: MoatazViewModel) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .padding(22.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BrandMark(64.dp)
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.connect_to_server), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.configure_server),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = { viewModel.selectTab(AppTab.Settings) }) {
                    Text(stringResource(R.string.open_settings))
                }
            }
        }
    }
}

@Composable
private fun ConnectionPendingState(state: MoatazUiState, viewModel: MoatazViewModel) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            if (state.connection == ConnectionState.Checking) {
                CircularProgressIndicator(strokeWidth = 2.dp)
                Spacer(Modifier.height(18.dp))
            } else {
                BrandMark(56.dp)
                Spacer(Modifier.height(18.dp))
            }
            Text(connectionLabel(state.connection), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                state.serverOrigin,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.connect(true) }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.refresh))
                }
                Button(onClick = { viewModel.selectTab(AppTab.Settings) }) {
                    Text(stringResource(R.string.nav_settings))
                }
            }
        }
    }
}

@Composable
private fun ThreadsScreen(state: MoatazUiState, viewModel: MoatazViewModel) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.threads_title),
            subtitle = stringResource(R.string.threads_subtitle),
            action = {
                FilledIconButton(onClick = viewModel::createThread, enabled = state.connection == ConnectionState.Connected) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.new_conversation))
                }
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(state.connection)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = viewModel::refreshThreads, enabled = !state.loadingThreads) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.refresh))
            }
        }
        if (state.loadingThreads && state.threads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 2.dp) }
        } else if (state.threads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_threads), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.threads, key = { it.id }) { thread ->
                    ThreadCard(thread, thread.id == state.activeThreadId) { viewModel.openThread(thread.id) }
                }
            }
        }
    }
}

@Composable
private fun ThreadCard(thread: ThreadSummary, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
            else MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                MoatazNavGlyph(AppTab.Chat, true, thread.title)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(thread.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(
                    thread.status + if (thread.updatedAt.isNotBlank()) " · ${thread.updatedAt.take(16).replace('T', ' ')}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FlowScreen(state: MoatazUiState, viewModel: MoatazViewModel) {
    val diagnostics = state.diagnostics()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.flow_title),
            subtitle = stringResource(R.string.flow_subtitle),
            action = {
                IconButton(onClick = {
                    val text = diagnosticsText(state)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("moataz ai diagnostics", text))
                }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.copy_diagnostics))
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PipelineCard(state)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard(stringResource(R.string.latency), diagnostics.latencyMs?.let { "${it} ms" } ?: "—", Modifier.weight(1f))
                    MetricCard(stringResource(R.string.models), diagnostics.modelCount.toString(), Modifier.weight(1f))
                    MetricCard(stringResource(R.string.events), diagnostics.telemetry.eventCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                DiagnosticsCard(state)
            }
            item {
                Text(
                    stringResource(R.string.recent_events),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (diagnostics.recentEvents.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_events),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 18.dp),
                    )
                }
            } else {
                items(diagnostics.recentEvents.reversed(), key = { "${it.timestamp}-${it.stage}-${it.title}" }) { event ->
                    FlowEventRow(event)
                }
            }
        }
    }
}

@Composable
private fun PipelineCard(state: MoatazUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            PipelineNode(
                title = stringResource(R.string.android_client),
                subtitle = "Kotlin · Compose · API 26+",
                active = true,
                complete = true,
            )
            PipelineConnector(state.connection == ConnectionState.Connected)
            PipelineNode(
                title = stringResource(R.string.gateway),
                subtitle = state.serverOrigin.ifBlank { stringResource(R.string.status_not_configured) },
                active = state.connection == ConnectionState.Checking,
                complete = state.connection == ConnectionState.Connected,
            )
            PipelineConnector(state.sending)
            PipelineNode(
                title = stringResource(R.string.agent_runtime),
                subtitle = if (state.sending) state.telemetry.phase else "DeerFlow Harness",
                active = state.sending,
                complete = state.connection == ConnectionState.Connected && !state.sending,
            )
            PipelineConnector(state.sending)
            PipelineNode(
                title = stringResource(R.string.model_provider),
                subtitle = state.models.firstOrNull { it.name == state.selectedModel }?.displayName
                    ?: stringResource(R.string.default_model),
                active = state.sending && state.telemetry.phase in setOf("model", "streaming", "syncing"),
                complete = state.selectedModel != null,
            )
        }
    }
}

@Composable
private fun PipelineNode(title: String, subtitle: String, active: Boolean, complete: Boolean) {
    val color = when {
        active -> MoatazCyan
        complete -> MoatazMint
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(if (active) 10.dp else 8.dp).clip(CircleShape).background(color))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (active) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = color)
    }
}

@Composable
private fun PipelineConnector(active: Boolean) {
    Box(
        Modifier
            .padding(start = 16.dp)
            .height(22.dp)
            .width(2.dp)
            .background(if (active) MoatazCyan.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(3.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DiagnosticsCard(state: MoatazUiState) {
    val rows = listOf(
        stringResource(R.string.active_thread) to (state.activeThreadId ?: "—"),
        stringResource(R.string.run_state) to state.telemetry.phase,
        "Run ID" to (state.telemetry.runId ?: "—"),
        "Transport" to if (state.serverOrigin.startsWith("https://")) "HTTPS" else if (state.serverOrigin.isNotBlank()) "HTTP / LAN" else "—",
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    SelectionContainer {
                        Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 190.dp))
                    }
                }
                if (index != rows.lastIndex) HorizontalDivider(Modifier.padding(vertical = 9.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun FlowEventRow(event: FlowEvent) {
    val color = when (event.level) {
        FlowLevel.Info -> MoatazCyan
        FlowLevel.Success -> MoatazMint
        FlowLevel.Warning -> MoatazAmber
        FlowLevel.Error -> MoatazDanger
    }
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 5.dp).size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                Text(event.title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(formatter.format(event.timestamp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!event.detail.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                SelectionContainer {
                    Text(event.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: MoatazUiState, viewModel: MoatazViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.native_client),
            )
        }
        item {
            SettingsSectionTitle(stringResource(R.string.connection_section))
            Card(
                modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(state.connection)
                        Spacer(Modifier.weight(1f))
                        state.latencyMs?.let {
                            Text("${it} ms", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = state.serverDraft,
                        onValueChange = viewModel::updateServerDraft,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.server_origin)) },
                        placeholder = { Text(stringResource(R.string.server_url_hint)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.tokenDraft,
                        onValueChange = viewModel::updateTokenDraft,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.access_token)) },
                        placeholder = { Text(if (state.tokenConfigured) "••••••••••••" else "") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        if (state.tokenConfigured) stringResource(R.string.token_saved) else stringResource(R.string.token_optional_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = viewModel::saveConnection, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.save_connect))
                    }
                    if (state.tokenConfigured) {
                        TextButton(onClick = viewModel::clearAccessToken, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.remove_token), color = MoatazDanger)
                        }
                    }
                    if (state.serverDraft.startsWith("http://") || runCatching { state.serverDraft.isNotBlank() && ServerUrlPolicy.isPrivateLan(state.serverDraft) }.getOrDefault(false)) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.security_lan_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MoatazAmber,
                        )
                    }
                }
            }
        }
        item {
            SettingsSectionTitle(stringResource(R.string.preferences))
            Card(
                modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.thinking), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.thinking_mode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.thinkingEnabled, onCheckedChange = viewModel::setThinkingEnabled)
                }
            }
        }
        item {
            SettingsSectionTitle(stringResource(R.string.about))
            Card(
                modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(48.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("moataz ai", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.about_text), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.upstream_source), color = MoatazCyan, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.android_support), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Text(BuildConfig.UPSTREAM_REPOSITORY, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr))
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

@Composable
private fun ScreenHeader(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action?.invoke()
    }
}

@Composable
private fun BrandMark(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(18.dp, RoundedCornerShape(size * 0.32f), ambientColor = MoatazViolet.copy(alpha = 0.35f), spotColor = MoatazCyan.copy(alpha = 0.18f))
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(listOf(MoatazViolet, MoatazIndigo, MoatazCyan))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(size * 0.2f)) {
            val stroke = Stroke(width = this.size.width * 0.12f, cap = StrokeCap.Round)
            drawLine(Color.White, Offset(this.size.width * 0.15f, this.size.height * 0.74f), Offset(this.size.width * 0.42f, this.size.height * 0.22f), stroke.width, StrokeCap.Round)
            drawLine(Color.White, Offset(this.size.width * 0.42f, this.size.height * 0.22f), Offset(this.size.width * 0.6f, this.size.height * 0.62f), stroke.width, StrokeCap.Round)
            drawLine(Color.White, Offset(this.size.width * 0.6f, this.size.height * 0.62f), Offset(this.size.width * 0.84f, this.size.height * 0.28f), stroke.width, StrokeCap.Round)
            drawCircle(Color.White, radius = this.size.width * 0.075f, center = Offset(this.size.width * 0.6f, this.size.height * 0.62f))
        }
    }
}

@Composable
private fun StatusDot(connection: ConnectionState) {
    val color = connectionColor(connection)
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}

@Composable
private fun StatusPill(connection: ConnectionState) {
    val color = connectionColor(connection)
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.11f))
            .border(1.dp, color.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(connectionLabel(connection), color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun connectionColor(connection: ConnectionState): Color = when (connection) {
    ConnectionState.Connected -> MoatazMint
    ConnectionState.Checking -> MoatazCyan
    ConnectionState.Degraded -> MoatazAmber
    ConnectionState.Unauthorized -> MoatazAmber
    ConnectionState.Offline -> MoatazDanger
    ConnectionState.NotConfigured -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun connectionLabel(connection: ConnectionState): String = stringResource(
    when (connection) {
        ConnectionState.Connected -> R.string.status_connected
        ConnectionState.Checking -> R.string.status_checking
        ConnectionState.Degraded -> R.string.status_degraded
        ConnectionState.Unauthorized -> R.string.status_unauthorized
        ConnectionState.Offline -> R.string.status_offline
        ConnectionState.NotConfigured -> R.string.status_not_configured
    },
)

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MoatazDanger.copy(alpha = 0.96f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.dismiss), tint = Color.White)
            }
        }
    }
}

private fun diagnosticsText(state: MoatazUiState): String = buildString {
    appendLine("moataz ai ${BuildConfig.VERSION_NAME}")
    appendLine("Android native client · minSdk 26")
    appendLine("Origin: ${state.serverOrigin}")
    appendLine("Connection: ${state.connection}")
    appendLine("Latency: ${state.latencyMs ?: "unknown"} ms")
    appendLine("Models: ${state.models.size}")
    appendLine("Selected model: ${state.selectedModel ?: "default"}")
    appendLine("Thread: ${state.activeThreadId ?: "none"}")
    appendLine("Run: ${state.telemetry.runId ?: "none"}")
    appendLine("Phase: ${state.telemetry.phase}")
    appendLine("Events: ${state.telemetry.eventCount}")
    state.error?.let { appendLine("Last error: $it") }
}
