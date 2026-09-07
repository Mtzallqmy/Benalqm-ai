package ai.moataz.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.moataz.app.data.AppPreferences
import ai.moataz.app.data.GatewayException
import ai.moataz.app.data.WorkspaceGatewayClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


data class RuntimeFeatureFlags(
    val agentsApi: Boolean = false,
    val browserControl: Boolean = false,
    val mcpTasks: Boolean = false,
    val subagentBatchRepository: Boolean = false,
    val subagentBatchWorker: Boolean = false,
    val subagentMaxRunning: Int = 0,
)

data class RuntimeAgent(
    val name: String,
    val description: String,
    val model: String?,
    val thinkingEnabled: Boolean?,
    val reasoningEffort: String?,
    val soul: String,
    val raw: JSONObject,
)

data class RuntimeMcpTask(
    val id: String,
    val name: String,
    val status: String,
    val error: String?,
    val trackingDegraded: Boolean,
    val cancelRequested: Boolean,
    val raw: JSONObject,
)

data class RuntimeBatch(
    val id: String,
    val status: String,
    val raw: JSONObject,
)

data class RuntimeOperationsState(
    val activeThreadId: String? = null,
    val loading: Boolean = false,
    val features: RuntimeFeatureFlags = RuntimeFeatureFlags(),
    val goal: JSONObject? = null,
    val agents: List<RuntimeAgent> = emptyList(),
    val mcpTasks: List<RuntimeMcpTask> = emptyList(),
    val batches: List<RuntimeBatch> = emptyList(),
    val selectedBatchItems: JSONArray = JSONArray(),
    val error: String? = null,
    val notice: String? = null,
)

class RuntimeOperationsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application)
    private val mutableState = MutableStateFlow(RuntimeOperationsState())
    val state: StateFlow<RuntimeOperationsState> = mutableState.asStateFlow()

    fun setActiveThread(threadId: String?) {
        if (threadId == mutableState.value.activeThreadId) return
        mutableState.update { it.copy(activeThreadId = threadId, goal = null, mcpTasks = emptyList(), batches = emptyList()) }
        refreshAll()
    }

    fun dismissMessage() {
        mutableState.update { it.copy(error = null, notice = null) }
    }

    fun refreshAll() {
        viewModelScope.launch {
            val profile = preferences.load()
            if (profile.origin.isBlank()) {
                mutableState.update { it.copy(error = "Configure the Gateway first.") }
                return@launch
            }
            mutableState.update { it.copy(loading = true, error = null) }
            val gateway = WorkspaceGatewayClient(profile.origin, profile.bearerToken)
            runCatching {
                val features = loadFeatures(gateway)
                mutableState.update { it.copy(features = features) }
                coroutineScope {
                    val operations = mutableListOf(
                        async { if (features.agentsApi) loadAgents(gateway) else Unit },
                    )
                    val threadId = mutableState.value.activeThreadId
                    if (!threadId.isNullOrBlank()) {
                        operations += async { loadGoal(gateway, threadId) }
                        if (features.mcpTasks) operations += async { loadMcpTasks(gateway, threadId) }
                        if (features.subagentBatchRepository) operations += async { loadBatches(gateway, threadId) }
                    }
                    operations.awaitAll()
                }
            }.onFailure(::showError)
            mutableState.update { it.copy(loading = false) }
        }
    }

    fun setGoal(objective: String, maxContinuations: Int) = action("Goal updated.") { gateway ->
        val threadId = requireThread()
        val result = gateway.putRaw(
            "/api/threads/${encode(threadId)}/goal",
            JSONObject().put("objective", objective).put("max_continuations", maxContinuations),
        ) as? JSONObject
        mutableState.update { it.copy(goal = result?.optJSONObject("goal")) }
    }

    fun clearGoal() = action("Goal cleared.") { gateway ->
        gateway.deleteRaw("/api/threads/${encode(requireThread())}/goal")
        mutableState.update { it.copy(goal = null) }
    }

    fun createAgent(payload: JSONObject) = action("Agent created.") { gateway ->
        gateway.postRaw("/api/agents", payload)
        loadAgents(gateway)
    }

    fun updateAgent(name: String, payload: JSONObject) = action("Agent updated.") { gateway ->
        gateway.putRaw("/api/agents/${encode(name)}", payload)
        loadAgents(gateway)
    }

    fun deleteAgent(name: String) = action("Agent deleted.") { gateway ->
        gateway.deleteRaw("/api/agents/${encode(name)}")
        loadAgents(gateway)
    }

    fun cancelMcpTask(taskId: String) = action("MCP task cancellation requested.") { gateway ->
        gateway.postRaw("/api/threads/${encode(requireThread())}/mcp-tasks/${encode(taskId)}/cancel")
        loadMcpTasks(gateway, requireThread())
    }

    fun pauseBatch(batchId: String) = batchAction(batchId, "pause")
    fun resumeBatch(batchId: String) = batchAction(batchId, "resume")
    fun cancelBatch(batchId: String) = batchAction(batchId, "cancel")

    fun loadBatchItems(batchId: String) {
        viewModelScope.launch {
            runCatching {
                val gateway = gateway()
                val raw = gateway.getRaw(
                    "/api/threads/${encode(requireThread())}/subagent-batches/${encode(batchId)}/items?limit=500",
                )
                mutableState.update { it.copy(selectedBatchItems = raw as? JSONArray ?: JSONArray()) }
            }.onFailure(::showError)
        }
    }

    fun retryBatchItem(batchId: String, itemId: String) = action("Batch item queued for retry.") { gateway ->
        gateway.postRaw(
            "/api/threads/${encode(requireThread())}/subagent-batches/${encode(batchId)}/items/${encode(itemId)}/retry",
        )
        loadBatchItems(batchId)
    }

    private fun batchAction(batchId: String, verb: String) = action("Batch $verb completed.") { gateway ->
        gateway.postRaw(
            "/api/threads/${encode(requireThread())}/subagent-batches/${encode(batchId)}/$verb",
        )
        loadBatches(gateway, requireThread())
    }

    private suspend fun loadFeatures(gateway: WorkspaceGatewayClient): RuntimeFeatureFlags {
        val raw = gateway.getRaw("/api/features") as? JSONObject ?: JSONObject()
        val agents = raw.optJSONObject("agents_api") ?: JSONObject()
        val browser = raw.optJSONObject("browser_control") ?: JSONObject()
        val mcp = raw.optJSONObject("mcp_tasks") ?: JSONObject()
        val batches = raw.optJSONObject("subagent_batches") ?: JSONObject()
        return RuntimeFeatureFlags(
            agentsApi = agents.optBoolean("enabled"),
            browserControl = browser.optBoolean("enabled"),
            mcpTasks = mcp.optBoolean("enabled"),
            subagentBatchRepository = batches.optBoolean("repository_available"),
            subagentBatchWorker = batches.optBoolean("worker_running"),
            subagentMaxRunning = batches.optInt("max_running"),
        )
    }

    private suspend fun loadGoal(gateway: WorkspaceGatewayClient, threadId: String) {
        val raw = gateway.getRaw("/api/threads/${encode(threadId)}/goal") as? JSONObject ?: JSONObject()
        mutableState.update { it.copy(goal = raw.optJSONObject("goal")) }
    }

    private suspend fun loadAgents(gateway: WorkspaceGatewayClient) {
        val raw = gateway.getRaw("/api/agents") as? JSONObject ?: JSONObject()
        val array = raw.optJSONArray("agents") ?: JSONArray()
        val agents = (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            RuntimeAgent(
                name = item.optString("name"),
                description = item.optString("description"),
                model = item.optString("model").takeIf { it.isNotBlank() && it != "null" },
                thinkingEnabled = if (item.has("thinking_enabled") && !item.isNull("thinking_enabled")) item.optBoolean("thinking_enabled") else null,
                reasoningEffort = item.optString("reasoning_effort").takeIf { it.isNotBlank() && it != "null" },
                soul = item.optString("soul"),
                raw = item,
            )
        }
        mutableState.update { it.copy(agents = agents) }
    }

    private suspend fun loadMcpTasks(gateway: WorkspaceGatewayClient, threadId: String) {
        val raw = gateway.getRaw("/api/threads/${encode(threadId)}/mcp-tasks?limit=100")
        val array = raw as? JSONArray ?: JSONArray()
        val tasks = (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            RuntimeMcpTask(
                id = item.optString("task_id"),
                name = item.optString("task_name"),
                status = item.optString("status"),
                error = item.optString("error").takeIf { it.isNotBlank() && it != "null" },
                trackingDegraded = item.optBoolean("tracking_degraded"),
                cancelRequested = item.optBoolean("cancel_requested"),
                raw = item,
            )
        }
        mutableState.update { it.copy(mcpTasks = tasks) }
    }

    private suspend fun loadBatches(gateway: WorkspaceGatewayClient, threadId: String) {
        val raw = gateway.getRaw("/api/threads/${encode(threadId)}/subagent-batches?limit=100")
        val array = raw as? JSONArray ?: JSONArray()
        val batches = (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            RuntimeBatch(
                id = item.optString("id", item.optString("batch_id")),
                status = item.optString("status"),
                raw = item,
            )
        }
        mutableState.update { it.copy(batches = batches) }
    }

    private fun action(success: String, block: suspend (WorkspaceGatewayClient) -> Unit) {
        viewModelScope.launch {
            mutableState.update { it.copy(error = null, notice = null) }
            runCatching { block(gateway()) }
                .onSuccess { mutableState.update { it.copy(notice = success) } }
                .onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        val message = if (error is GatewayException) {
            "HTTP ${error.statusCode}: ${error.message}"
        } else {
            error.message ?: error::class.java.simpleName
        }
        mutableState.update { it.copy(error = message) }
    }

    private fun gateway(): WorkspaceGatewayClient {
        val profile = preferences.load()
        require(profile.origin.isNotBlank()) { "Gateway is not configured." }
        return WorkspaceGatewayClient(profile.origin, profile.bearerToken)
    }

    private fun requireThread(): String = mutableState.value.activeThreadId?.takeIf { it.isNotBlank() }
        ?: error("Open a conversation first.")

    private fun encode(value: String): String = java.net.URLEncoder
        .encode(value, java.nio.charset.StandardCharsets.UTF_8.name())
        .replace("+", "%20")
}
