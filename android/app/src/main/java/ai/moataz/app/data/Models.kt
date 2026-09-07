package ai.moataz.app.data

import java.time.Instant

enum class ConnectionState {
    NotConfigured,
    Checking,
    Connected,
    Degraded,
    Offline,
    Unauthorized,
}

enum class MessageRole {
    User,
    Assistant,
    Tool,
    System,
}

data class ModelInfo(
    val name: String,
    val model: String,
    val displayName: String,
    val description: String? = null,
    val supportsThinking: Boolean = false,
    val supportsReasoningEffort: Boolean = false,
)

data class ThreadSummary(
    val id: String,
    val title: String,
    val status: String = "idle",
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val name: String? = null,
    val runId: String? = null,
    val toolCalls: List<String> = emptyList(),
    val createdAt: Instant? = null,
)

data class GatewayProbe(
    val origin: String,
    val latencyMs: Long,
    val models: List<ModelInfo>,
    val secureTransport: Boolean,
    val message: String? = null,
)

data class StreamFrame(
    val event: String,
    val id: String? = null,
    val rawData: String,
)

data class FlowEvent(
    val timestamp: Instant = Instant.now(),
    val stage: String,
    val title: String,
    val detail: String? = null,
    val level: FlowLevel = FlowLevel.Info,
)

enum class FlowLevel {
    Info,
    Success,
    Warning,
    Error,
}

data class RunTelemetry(
    val runId: String? = null,
    val running: Boolean = false,
    val phase: String = "idle",
    val eventCount: Int = 0,
    val startedAt: Instant? = null,
    val lastEventAt: Instant? = null,
)

data class DiagnosticsSnapshot(
    val origin: String = "",
    val connection: ConnectionState = ConnectionState.NotConfigured,
    val latencyMs: Long? = null,
    val modelCount: Int = 0,
    val selectedModel: String? = null,
    val activeThreadId: String? = null,
    val telemetry: RunTelemetry = RunTelemetry(),
    val recentEvents: List<FlowEvent> = emptyList(),
    val lastError: String? = null,
)
