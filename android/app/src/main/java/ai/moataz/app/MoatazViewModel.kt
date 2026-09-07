package ai.moataz.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.moataz.app.data.AppPreferences
import ai.moataz.app.data.ChatMessage
import ai.moataz.app.data.ConnectionState
import ai.moataz.app.data.DiagnosticsSnapshot
import ai.moataz.app.data.FlowEvent
import ai.moataz.app.data.FlowLevel
import ai.moataz.app.data.GatewayClient
import ai.moataz.app.data.GatewayException
import ai.moataz.app.data.ModelInfo
import ai.moataz.app.data.RunTelemetry
import ai.moataz.app.data.StreamFrame
import ai.moataz.app.data.ThreadSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant

enum class AppTab { Chat, Threads, Flow, Settings }

data class MoatazUiState(
    val tab: AppTab = AppTab.Chat,
    val serverOrigin: String = "",
    val serverDraft: String = "",
    val tokenConfigured: Boolean = false,
    val tokenDraft: String = "",
    val connection: ConnectionState = ConnectionState.NotConfigured,
    val latencyMs: Long? = null,
    val models: List<ModelInfo> = emptyList(),
    val selectedModel: String? = null,
    val thinkingEnabled: Boolean = true,
    val threads: List<ThreadSummary> = emptyList(),
    val activeThreadId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val composer: String = "",
    val loadingThreads: Boolean = false,
    val loadingMessages: Boolean = false,
    val sending: Boolean = false,
    val activeRunId: String? = null,
    val telemetry: RunTelemetry = RunTelemetry(),
    val flowEvents: List<FlowEvent> = emptyList(),
    val error: String? = null,
) {
    val configured: Boolean get() = serverOrigin.isNotBlank()

    fun diagnostics(): DiagnosticsSnapshot = DiagnosticsSnapshot(
        origin = serverOrigin,
        connection = connection,
        latencyMs = latencyMs,
        modelCount = models.size,
        selectedModel = selectedModel,
        activeThreadId = activeThreadId,
        telemetry = telemetry,
        recentEvents = flowEvents.takeLast(40),
        lastError = error,
    )
}

class MoatazViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application)
    private val initialProfile = preferences.load(BuildConfig.DEFAULT_ORIGIN)

    private val _state = MutableStateFlow(
        MoatazUiState(
            serverOrigin = initialProfile.origin,
            serverDraft = initialProfile.origin,
            tokenConfigured = preferences.hasBearerToken(),
            selectedModel = initialProfile.selectedModel,
            thinkingEnabled = initialProfile.thinkingEnabled,
            connection = if (initialProfile.origin.isBlank()) ConnectionState.NotConfigured else ConnectionState.Checking,
        ),
    )
    val state: StateFlow<MoatazUiState> = _state.asStateFlow()

    @Volatile
    private var client: GatewayClient? = initialProfile.origin.takeIf { it.isNotBlank() }?.let {
        GatewayClient(it, initialProfile.bearerToken)
    }
    private var streamJob: Job? = null

    init {
        if (initialProfile.origin.isNotBlank()) connect(refreshThreads = true)
    }

    fun selectTab(tab: AppTab) {
        _state.update { it.copy(tab = tab, error = null) }
        if (tab == AppTab.Threads && _state.value.threads.isEmpty()) refreshThreads()
    }

    fun updateServerDraft(value: String) = _state.update { it.copy(serverDraft = value) }
    fun updateTokenDraft(value: String) = _state.update { it.copy(tokenDraft = value) }
    fun updateComposer(value: String) = _state.update { it.copy(composer = value) }

    fun setThinkingEnabled(enabled: Boolean) {
        preferences.saveThinkingEnabled(enabled)
        _state.update { it.copy(thinkingEnabled = enabled) }
    }

    fun selectModel(name: String) {
        preferences.saveSelectedModel(name)
        _state.update { it.copy(selectedModel = name) }
        addEvent("model", "Model selected", name, FlowLevel.Info)
    }

    fun saveConnection() {
        val current = _state.value
        val normalized = runCatching { ServerUrlPolicy.normalize(current.serverDraft) }
            .getOrElse { error ->
                _state.update { it.copy(error = error.message ?: "Invalid server address") }
                return
            }
        preferences.saveOrigin(normalized)
        if (current.tokenDraft.isNotBlank()) {
            preferences.saveBearerToken(current.tokenDraft)
        }
        val profile = preferences.load(BuildConfig.DEFAULT_ORIGIN)
        client = GatewayClient(normalized, profile.bearerToken)
        _state.update {
            it.copy(
                serverOrigin = normalized,
                serverDraft = normalized,
                tokenDraft = "",
                tokenConfigured = preferences.hasBearerToken(),
                connection = ConnectionState.Checking,
                error = null,
            )
        }
        connect(refreshThreads = true)
    }

    fun clearAccessToken() {
        preferences.saveBearerToken(null)
        val current = _state.value
        client = current.serverOrigin.takeIf { it.isNotBlank() }?.let { GatewayClient(it) }
        _state.update { it.copy(tokenConfigured = false, tokenDraft = "") }
        addEvent("security", "Access token removed", level = FlowLevel.Warning)
    }

    fun connect(refreshThreads: Boolean = true) {
        val activeClient = client ?: run {
            _state.update { it.copy(connection = ConnectionState.NotConfigured) }
            return
        }
        _state.update { it.copy(connection = ConnectionState.Checking, error = null) }
        addEvent("gateway", "Checking Gateway", activeClient.origin, FlowLevel.Info)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { activeClient.probe() }
                .onSuccess { probe ->
                    val savedModel = _state.value.selectedModel
                    val selected = savedModel?.takeIf { name -> probe.models.any { it.name == name } }
                        ?: probe.models.firstOrNull()?.name
                    if (selected != null) preferences.saveSelectedModel(selected)
                    _state.update {
                        it.copy(
                            connection = ConnectionState.Connected,
                            latencyMs = probe.latencyMs,
                            models = probe.models,
                            selectedModel = selected,
                            error = null,
                        )
                    }
                    addEvent(
                        "gateway",
                        "Gateway connected",
                        "${probe.latencyMs} ms · ${probe.models.size} models",
                        FlowLevel.Success,
                    )
                    if (refreshThreads) refreshThreads()
                }
                .onFailure(::handleConnectionFailure)
        }
    }

    fun refreshThreads() {
        val activeClient = client ?: return
        _state.update { it.copy(loadingThreads = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { activeClient.searchThreads() }
                .onSuccess { rows ->
                    val sorted = rows.sortedByDescending { it.updatedAt.ifBlank { it.createdAt } }
                    _state.update { current ->
                        current.copy(
                            loadingThreads = false,
                            threads = sorted,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loadingThreads = false, error = friendlyError(error)) }
                }
        }
    }

    fun createThread() {
        val activeClient = client ?: return
        _state.update { it.copy(loadingMessages = true, tab = AppTab.Chat, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { activeClient.createThread() }
                .onSuccess { thread ->
                    _state.update {
                        it.copy(
                            activeThreadId = thread.id,
                            messages = emptyList(),
                            loadingMessages = false,
                            threads = listOf(thread) + it.threads.filterNot { row -> row.id == thread.id },
                        )
                    }
                    addEvent("thread", "New thread created", thread.id, FlowLevel.Success)
                }
                .onFailure { error ->
                    _state.update { it.copy(loadingMessages = false, error = friendlyError(error)) }
                }
        }
    }

    fun openThread(threadId: String) {
        val activeClient = client ?: return
        _state.update {
            it.copy(
                activeThreadId = threadId,
                loadingMessages = true,
                tab = AppTab.Chat,
                error = null,
            )
        }
        addEvent("thread", "Loading thread", threadId, FlowLevel.Info)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { activeClient.getThreadMessages(threadId) }
                .onSuccess { messages ->
                    _state.update { it.copy(messages = messages, loadingMessages = false) }
                    addEvent("thread", "Thread loaded", "${messages.size} visible messages", FlowLevel.Success)
                }
                .onFailure { error ->
                    _state.update { it.copy(loadingMessages = false, error = friendlyError(error)) }
                }
        }
    }

    fun sendMessage() {
        val current = _state.value
        if (current.sending || current.composer.isBlank()) return
        if (client == null) return
        val text = current.composer.trim()
        if (current.activeThreadId == null) {
            createThreadAndSend(text)
            return
        }
        startRun(current.activeThreadId, text)
    }

    private fun createThreadAndSend(text: String) {
        val activeClient = client ?: return
        _state.update { it.copy(sending = true, composer = "", error = null, tab = AppTab.Chat) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { activeClient.createThread() }
                .onSuccess { thread ->
                    _state.update {
                        it.copy(
                            activeThreadId = thread.id,
                            threads = listOf(thread) + it.threads.filterNot { row -> row.id == thread.id },
                            sending = false,
                        )
                    }
                    startRun(thread.id, text)
                }
                .onFailure { error ->
                    _state.update { it.copy(sending = false, composer = text, error = friendlyError(error)) }
                }
        }
    }

    private fun startRun(threadId: String, text: String) {
        val activeClient = client ?: return
        val selectedModel = _state.value.selectedModel
        val thinking = _state.value.thinkingEnabled
        val optimistic = ChatMessage(
            id = "local-${System.nanoTime()}",
            role = ai.moataz.app.data.MessageRole.User,
            text = text,
        )
        _state.update { current ->
            current.copy(
                composer = "",
                sending = true,
                activeRunId = null,
                messages = current.messages + optimistic,
                telemetry = RunTelemetry(
                    running = true,
                    phase = "starting",
                    eventCount = 0,
                    startedAt = Instant.now(),
                ),
                error = null,
            )
        }
        addEvent("run", "Run starting", selectedModel ?: "default model", FlowLevel.Info)

        streamJob?.cancel()
        streamJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                activeClient.streamRun(
                    threadId = threadId,
                    text = text,
                    modelName = selectedModel,
                    thinkingEnabled = thinking,
                    onRunCreated = { runId ->
                        _state.update { current ->
                            current.copy(
                                activeRunId = runId,
                                telemetry = current.telemetry.copy(
                                    runId = runId,
                                    running = true,
                                    phase = "streaming",
                                    lastEventAt = Instant.now(),
                                ),
                            )
                        }
                        addEvent("run", "Run admitted", runId, FlowLevel.Success)
                    },
                    onFrame = { frame -> handleStreamFrame(activeClient, frame) },
                )
            }.onSuccess {
                _state.update { current ->
                    current.copy(
                        sending = false,
                        activeRunId = null,
                        telemetry = current.telemetry.copy(
                            running = false,
                            phase = "completed",
                            lastEventAt = Instant.now(),
                        ),
                    )
                }
                addEvent("run", "Run completed", level = FlowLevel.Success)
                refreshMessagesAfterRun(activeClient, threadId)
                refreshThreads()
            }.onFailure { error ->
                _state.update { current ->
                    current.copy(
                        sending = false,
                        activeRunId = null,
                        telemetry = current.telemetry.copy(
                            running = false,
                            phase = "failed",
                            lastEventAt = Instant.now(),
                        ),
                        error = friendlyError(error),
                    )
                }
                addEvent("run", "Run failed", friendlyError(error), FlowLevel.Error)
                refreshMessagesAfterRun(activeClient, threadId)
            }
        }
    }

    private fun handleStreamFrame(activeClient: GatewayClient, frame: StreamFrame) {
        val now = Instant.now()
        _state.update { current ->
            current.copy(
                telemetry = current.telemetry.copy(
                    eventCount = current.telemetry.eventCount + 1,
                    phase = phaseFor(frame),
                    lastEventAt = now,
                ),
            )
        }

        activeClient.parseMessagesFrame(frame)?.let { streamedMessages ->
            if (streamedMessages.isNotEmpty()) {
                _state.update { it.copy(messages = streamedMessages) }
            }
        }

        when (frame.event) {
            "updates" -> addEvent("agent", "Agent state updated", summarizeFrame(frame), FlowLevel.Info)
            "custom" -> addEvent("flow", customEventTitle(frame), summarizeFrame(frame), FlowLevel.Info)
            "gap" -> addEvent("stream", "Stream replay gap", summarizeFrame(frame), FlowLevel.Warning)
            "error" -> addEvent("stream", "Stream error", summarizeFrame(frame), FlowLevel.Error)
        }
    }

    private fun refreshMessagesAfterRun(activeClient: GatewayClient, threadId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { activeClient.getThreadMessages(threadId) }
                .onSuccess { messages -> _state.update { it.copy(messages = messages) } }
        }
    }

    fun stopRun() {
        val current = _state.value
        val runId = current.activeRunId ?: return
        val threadId = current.activeThreadId ?: return
        val activeClient = client ?: return
        addEvent("run", "Stopping run", runId, FlowLevel.Warning)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { activeClient.cancelRun(threadId, runId) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            sending = false,
                            telemetry = state.telemetry.copy(running = false, phase = "interrupted"),
                        )
                    }
                    addEvent("run", "Run interrupted", runId, FlowLevel.Warning)
                }
                .onFailure { error ->
                    _state.update { it.copy(error = friendlyError(error)) }
                    addEvent("run", "Stop failed", friendlyError(error), FlowLevel.Error)
                }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun handleConnectionFailure(error: Throwable) {
        val state = when ((error as? GatewayException)?.statusCode) {
            401, 403 -> ConnectionState.Unauthorized
            else -> ConnectionState.Offline
        }
        _state.update {
            it.copy(
                connection = state,
                latencyMs = null,
                error = friendlyError(error),
            )
        }
        addEvent("gateway", "Gateway unavailable", friendlyError(error), FlowLevel.Error)
    }

    private fun phaseFor(frame: StreamFrame): String = when (frame.event) {
        "values" -> "syncing"
        "updates" -> "agent"
        "custom" -> "tools"
        "messages", "messages-tuple" -> "model"
        "gap" -> "recovering"
        "error" -> "error"
        else -> frame.event.ifBlank { "streaming" }
    }

    private fun customEventTitle(frame: StreamFrame): String {
        val json = runCatching { JSONObject(frame.rawData) }.getOrNull()
        return json?.optString("type")?.takeIf { it.isNotBlank() }
            ?.replace('_', ' ')
            ?.replaceFirstChar { it.uppercase() }
            ?: "Runtime event"
    }

    private fun summarizeFrame(frame: StreamFrame): String = frame.rawData
        .replace("\n", " ")
        .let { if (it.length <= 160) it else it.take(157) + "…" }

    private fun addEvent(stage: String, title: String, detail: String? = null, level: FlowLevel = FlowLevel.Info) {
        _state.update { current ->
            val updated = (current.flowEvents + FlowEvent(stage = stage, title = title, detail = detail, level = level))
                .takeLast(120)
            current.copy(flowEvents = updated)
        }
    }

    private fun friendlyError(error: Throwable): String = when (error) {
        is GatewayException -> when (error.statusCode) {
            401 -> "Authentication required. Add a valid access token in Settings."
            403 -> "This account or token does not have permission for this operation."
            404 -> "The requested DeerFlow resource was not found."
            409 -> error.message
            429 -> "The model provider is rate limited or out of quota."
            in 500..599 -> "The moataz ai Gateway reported a server error (${error.statusCode})."
            else -> error.message
        }
        else -> error.message ?: error.javaClass.simpleName
    }
}
