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
import java.time.Instant

enum class CapabilitySection {
    Overview,
    Providers,
    Files,
    Skills,
    Mcp,
    Subagents,
    Memory,
    ScheduledTasks,
    Browser,
}

enum class CapabilityHealth { Idle, Loading, Ready, Unauthorized, Unavailable, Error }

data class CapabilityStatus(
    val health: CapabilityHealth = CapabilityHealth.Idle,
    val message: String? = null,
    val updatedAt: String? = null,
)

data class ControlCenterState(
    val section: CapabilitySection = CapabilitySection.Overview,
    val activeThreadId: String? = null,
    val origin: String = "",
    val refreshing: Boolean = false,
    val providers: List<WorkspaceGatewayClient.Provider> = emptyList(),
    val skills: List<WorkspaceGatewayClient.Skill> = emptyList(),
    val mcpServers: List<WorkspaceGatewayClient.McpServer> = emptyList(),
    val subagents: List<WorkspaceGatewayClient.Subagent> = emptyList(),
    val memory: WorkspaceGatewayClient.MemorySnapshot? = null,
    val scheduledTasks: List<WorkspaceGatewayClient.ScheduledTask> = emptyList(),
    val files: List<WorkspaceGatewayClient.ThreadFile> = emptyList(),
    val uploadLimits: WorkspaceGatewayClient.UploadLimits? = null,
    val browser: WorkspaceGatewayClient.BrowserNavigation? = null,
    val providerStatus: CapabilityStatus = CapabilityStatus(),
    val filesStatus: CapabilityStatus = CapabilityStatus(),
    val skillsStatus: CapabilityStatus = CapabilityStatus(),
    val mcpStatus: CapabilityStatus = CapabilityStatus(),
    val subagentsStatus: CapabilityStatus = CapabilityStatus(),
    val memoryStatus: CapabilityStatus = CapabilityStatus(),
    val tasksStatus: CapabilityStatus = CapabilityStatus(),
    val browserStatus: CapabilityStatus = CapabilityStatus(),
    val actionInProgress: String? = null,
    val notice: String? = null,
    val error: String? = null,
    val lastFullRefresh: String? = null,
)

class ControlCenterViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application)
    private val mutableState = MutableStateFlow(ControlCenterState())
    val state: StateFlow<ControlCenterState> = mutableState.asStateFlow()

    fun setSection(section: CapabilitySection) {
        mutableState.update { it.copy(section = section) }
    }

    fun setActiveThread(threadId: String?) {
        if (threadId == mutableState.value.activeThreadId) return
        mutableState.update { it.copy(activeThreadId = threadId) }
        if (!threadId.isNullOrBlank()) refreshFiles()
    }

    fun dismissMessage() {
        mutableState.update { it.copy(error = null, notice = null) }
    }

    fun refreshAll() {
        viewModelScope.launch {
            val profile = preferences.load()
            if (profile.origin.isBlank()) {
                mutableState.update {
                    it.copy(origin = "", refreshing = false, error = "Configure the Gateway connection first.")
                }
                return@launch
            }
            mutableState.update { it.copy(origin = profile.origin, refreshing = true, error = null) }
            coroutineScope {
                listOf(
                    async { refreshProvidersInternal() },
                    async { refreshSkillsInternal() },
                    async { refreshMcpInternal() },
                    async { refreshSubagentsInternal() },
                    async { refreshMemoryInternal() },
                    async { refreshTasksInternal() },
                    async { refreshFilesInternal() },
                ).awaitAll()
            }
            mutableState.update {
                it.copy(refreshing = false, lastFullRefresh = Instant.now().toString())
            }
        }
    }

    fun refreshProviders() = launchRefresh(::refreshProvidersInternal)
    fun refreshSkills() = launchRefresh(::refreshSkillsInternal)
    fun refreshMcp() = launchRefresh(::refreshMcpInternal)
    fun refreshSubagents() = launchRefresh(::refreshSubagentsInternal)
    fun refreshMemory() = launchRefresh(::refreshMemoryInternal)
    fun refreshScheduledTasks() = launchRefresh(::refreshTasksInternal)
    fun refreshFiles() = launchRefresh(::refreshFilesInternal)

    /**
     * DeerFlow's dynamic-provider default is a model registry name, not a provider id.
     * Choosing a provider here selects its first discovered model as the default model.
     * A later UI layer can expose per-model default selection without changing this API.
     */
    fun setDefaultProvider(providerId: String) = action("provider-default") { client ->
        val provider = mutableState.value.providers.firstOrNull { it.id == providerId }
            ?: error("Provider not found")
        val models = provider.raw.optJSONArray("models") ?: JSONArray()
        val firstModel = models.optJSONObject(0) ?: error("Provider has no discovered models")
        val registryName = firstModel.optString("registry_name")
        require(registryName.isNotBlank()) { "Provider model does not have a registry name" }
        client.putRaw("/api/providers/default", JSONObject().put("model_name", registryName))
        refreshProvidersInternal()
        "Default model updated."
    }

    fun deleteProvider(providerId: String) = action("provider-delete") { client ->
        client.deleteProvider(providerId)
        refreshProvidersInternal()
        "Provider removed."
    }

    fun saveProvider(payload: JSONObject) = action("provider-save") { client ->
        client.saveProvider(payload)
        refreshProvidersInternal()
        "Provider saved."
    }

    fun testProvider(type: String, baseUrl: String, apiKey: String, callback: (Result<JSONObject>) -> Unit) {
        viewModelScope.launch {
            runCatching { client().testProvider(type, baseUrl, apiKey) }.also(callback)
        }
    }

    fun discoverProvider(type: String, baseUrl: String, apiKey: String, callback: (Result<JSONObject>) -> Unit) {
        viewModelScope.launch {
            runCatching { client().discoverProviderModels(type, baseUrl, apiKey) }.also(callback)
        }
    }

    fun setSkillEnabled(name: String, enabled: Boolean) = action("skill-$name") { client ->
        client.setSkillEnabled(name, enabled)
        refreshSkillsInternal()
        if (enabled) "Skill enabled." else "Skill disabled."
    }

    fun reloadSkills() = action("skills-reload") { client ->
        client.reloadSkills()
        refreshSkillsInternal()
        "Skill cache reloaded."
    }

    fun updateCustomSkill(name: String, content: String) = action("skill-edit-$name") { client ->
        client.updateCustomSkill(name, content)
        refreshSkillsInternal()
        "Custom skill updated."
    }

    fun deleteCustomSkill(name: String) = action("skill-delete-$name") { client ->
        client.deleteCustomSkill(name)
        refreshSkillsInternal()
        "Custom skill deleted."
    }

    fun setMcpEnabled(name: String, enabled: Boolean) = action("mcp-$name") { client ->
        client.setMcpServerEnabled(name, enabled)
        refreshMcpInternal()
        if (enabled) "MCP server enabled." else "MCP server disabled."
    }

    fun resetMcpCache() = action("mcp-cache") { client ->
        client.resetMcpCache()
        "MCP tool cache reset."
    }

    fun saveMcpServer(name: String, server: JSONObject, replacing: Boolean) = action("mcp-save") { client ->
        if (replacing) client.replaceMcpServer(name, server) else client.createMcpServer(name, server)
        refreshMcpInternal()
        "MCP server saved."
    }

    fun deleteMcpServer(name: String) = action("mcp-delete-$name") { client ->
        client.deleteMcpServer(name)
        refreshMcpInternal()
        "MCP server removed."
    }

    fun createSubagent(payload: JSONObject) = action("subagent-create") { client ->
        client.createSubagent(payload)
        refreshSubagentsInternal()
        "Sub-agent created."
    }

    fun updateSubagent(name: String, payload: JSONObject) = action("subagent-$name") { client ->
        client.updateSubagent(name, payload)
        refreshSubagentsInternal()
        "Sub-agent updated."
    }

    fun deleteSubagent(name: String) = action("subagent-delete-$name") { client ->
        client.deleteSubagent(name)
        refreshSubagentsInternal()
        "Sub-agent deleted."
    }

    fun addMemoryFact(content: String, category: String = "context", confidence: Double = 0.8) = action("memory-add") { client ->
        val memory = client.createMemoryFact(content, category, confidence)
        mutableState.update { it.copy(memory = memory) }
        "Memory fact saved."
    }

    fun deleteMemoryFact(id: String) = action("memory-delete-$id") { client ->
        val memory = client.deleteMemoryFact(id)
        mutableState.update { it.copy(memory = memory) }
        "Memory fact deleted."
    }

    fun clearMemory() = action("memory-clear") { client ->
        val memory = client.clearMemory()
        mutableState.update { it.copy(memory = memory) }
        "Memory cleared."
    }

    fun reloadMemoryData() = action("memory-reload") { client ->
        val memory = client.reloadMemory()
        mutableState.update { it.copy(memory = memory) }
        "Memory reloaded."
    }

    fun createScheduledTask(payload: JSONObject) = action("task-create") { client ->
        client.createScheduledTask(payload)
        refreshTasksInternal()
        "Scheduled task created."
    }

    fun pauseScheduledTask(id: String) = taskAction(id, "pause") { it.pauseScheduledTask(id) }
    fun resumeScheduledTask(id: String) = taskAction(id, "resume") { it.resumeScheduledTask(id) }
    fun triggerScheduledTask(id: String) = taskAction(id, "trigger") { it.triggerScheduledTask(id) }

    fun deleteScheduledTask(id: String) = action("task-delete-$id") { client ->
        client.deleteScheduledTask(id)
        refreshTasksInternal()
        "Scheduled task deleted."
    }

    fun uploadFile(filename: String, mimeType: String, bytes: ByteArray) = action("file-upload") { client ->
        val threadId = requireActiveThread()
        client.uploadThreadFile(threadId, filename, mimeType, bytes)
        refreshFilesInternal()
        "File uploaded."
    }

    fun deleteFile(filename: String) = action("file-delete-$filename") { client ->
        val threadId = requireActiveThread()
        client.deleteThreadFile(threadId, filename)
        refreshFilesInternal()
        "File deleted."
    }

    fun artifactUrl(virtualPath: String, download: Boolean = false): String? = runCatching {
        client().artifactUrl(requireActiveThread(), virtualPath, download)
    }.getOrNull()

    fun navigateBrowser(url: String) = action("browser-navigate") { client ->
        val result = client.navigateBrowser(requireActiveThread(), url)
        mutableState.update {
            it.copy(
                browser = result,
                browserStatus = CapabilityStatus(CapabilityHealth.Ready, result.title, Instant.now().toString()),
            )
        }
        "Browser navigated to ${result.url}"
    }

    private fun taskAction(
        id: String,
        verb: String,
        call: suspend (WorkspaceGatewayClient) -> JSONObject,
    ) = action("task-$verb-$id") { client ->
        call(client)
        refreshTasksInternal()
        "Scheduled task $verb completed."
    }

    private fun launchRefresh(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun action(
        name: String,
        operation: suspend (WorkspaceGatewayClient) -> String,
    ) {
        viewModelScope.launch {
            mutableState.update { it.copy(actionInProgress = name, error = null, notice = null) }
            runCatching { operation(client()) }
                .onSuccess { message ->
                    mutableState.update { it.copy(actionInProgress = null, notice = message) }
                }
                .onFailure { throwable ->
                    mutableState.update {
                        it.copy(actionInProgress = null, error = readableError(throwable))
                    }
                }
        }
    }

    private suspend fun refreshProvidersInternal() {
        updateProviderStatus(CapabilityHealth.Loading)
        runCatching {
            val client = client()
            val rawList = client.getRaw("/api/providers")
            val providersArray = rawList as? JSONArray ?: JSONArray()
            val defaultModel = runCatching {
                (client.getRaw("/api/providers/default") as? JSONObject)?.optString("model_name").orEmpty()
            }.getOrDefault("")

            (0 until providersArray.length()).mapNotNull { index ->
                val item = providersArray.optJSONObject(index) ?: return@mapNotNull null
                val models = item.optJSONArray("models") ?: JSONArray()
                val providerIsDefault = (0 until models.length()).any { modelIndex ->
                    models.optJSONObject(modelIndex)?.optString("registry_name") == defaultModel && defaultModel.isNotBlank()
                }
                WorkspaceGatewayClient.Provider(
                    id = item.optString("id"),
                    name = item.optString("name", "Provider"),
                    type = item.optString("provider_type", "openai_compatible"),
                    baseUrl = item.optString("base_url"),
                    enabled = true,
                    isDefault = providerIsDefault,
                    modelCount = models.length(),
                    raw = item,
                )
            }
        }.onSuccess { providers ->
            mutableState.update { it.copy(providers = providers) }
            updateProviderStatus(CapabilityHealth.Ready, "${providers.size} providers")
        }.onFailure { updateProviderFailure(it) }
    }

    private suspend fun refreshSkillsInternal() {
        updateSkillsStatus(CapabilityHealth.Loading)
        runCatching { client().listSkills() }
            .onSuccess { skills ->
                mutableState.update { it.copy(skills = skills) }
                updateSkillsStatus(CapabilityHealth.Ready, "${skills.size} skills")
            }
            .onFailure { updateSkillsFailure(it) }
    }

    private suspend fun refreshMcpInternal() {
        updateMcpStatus(CapabilityHealth.Loading)
        runCatching { client().listMcpServers() }
            .onSuccess { servers ->
                mutableState.update { it.copy(mcpServers = servers) }
                updateMcpStatus(CapabilityHealth.Ready, "${servers.size} MCP servers")
            }
            .onFailure { updateMcpFailure(it) }
    }

    private suspend fun refreshSubagentsInternal() {
        updateSubagentsStatus(CapabilityHealth.Loading)
        runCatching { client().listSubagents() }
            .onSuccess { agents ->
                mutableState.update { it.copy(subagents = agents) }
                updateSubagentsStatus(CapabilityHealth.Ready, "${agents.size} sub-agents")
            }
            .onFailure { updateSubagentsFailure(it) }
    }

    private suspend fun refreshMemoryInternal() {
        updateMemoryStatus(CapabilityHealth.Loading)
        runCatching { client().getMemory() }
            .onSuccess { memory ->
                mutableState.update { it.copy(memory = memory) }
                updateMemoryStatus(CapabilityHealth.Ready, "${memory.facts.size} facts")
            }
            .onFailure { updateMemoryFailure(it) }
    }

    private suspend fun refreshTasksInternal() {
        updateTasksStatus(CapabilityHealth.Loading)
        runCatching { client().listScheduledTasks() }
            .onSuccess { tasks ->
                mutableState.update { it.copy(scheduledTasks = tasks) }
                updateTasksStatus(CapabilityHealth.Ready, "${tasks.size} tasks")
            }
            .onFailure { updateTasksFailure(it) }
    }

    private suspend fun refreshFilesInternal() {
        val threadId = mutableState.value.activeThreadId
        if (threadId.isNullOrBlank()) {
            mutableState.update {
                it.copy(
                    files = emptyList(),
                    uploadLimits = null,
                    filesStatus = CapabilityStatus(CapabilityHealth.Idle, "Open a conversation first."),
                )
            }
            return
        }
        updateFilesStatus(CapabilityHealth.Loading)
        runCatching {
            val client = client()
            val files = client.listThreadFiles(threadId)
            val limits = runCatching { client.getUploadLimits(threadId) }.getOrNull()
            files to limits
        }.onSuccess { (files, limits) ->
            mutableState.update { it.copy(files = files, uploadLimits = limits) }
            updateFilesStatus(CapabilityHealth.Ready, "${files.size} files")
        }.onFailure { updateFilesFailure(it) }
    }

    private fun client(): WorkspaceGatewayClient {
        val profile = preferences.load()
        require(profile.origin.isNotBlank()) { "Gateway is not configured." }
        if (profile.origin != mutableState.value.origin) {
            mutableState.update { it.copy(origin = profile.origin) }
        }
        return WorkspaceGatewayClient(profile.origin, profile.bearerToken)
    }

    private fun requireActiveThread(): String =
        mutableState.value.activeThreadId?.takeIf { it.isNotBlank() }
            ?: error("Open a conversation before using thread files or browser controls.")

    private fun readableError(error: Throwable): String = when (error) {
        is GatewayException -> "HTTP ${error.statusCode}: ${error.message}"
        else -> error.message ?: error::class.java.simpleName
    }

    private fun statusFor(error: Throwable): CapabilityStatus {
        val now = Instant.now().toString()
        val gateway = error as? GatewayException
        return when (gateway?.statusCode) {
            401, 403 -> CapabilityStatus(CapabilityHealth.Unauthorized, gateway.message, now)
            404, 501 -> CapabilityStatus(CapabilityHealth.Unavailable, gateway.message, now)
            else -> CapabilityStatus(CapabilityHealth.Error, readableError(error), now)
        }
    }

    private fun status(health: CapabilityHealth, message: String? = null) =
        CapabilityStatus(health, message, Instant.now().toString())

    private fun updateProviderStatus(h: CapabilityHealth, m: String? = null) { mutableState.update { it.copy(providerStatus = status(h, m)) } }
    private fun updateFilesStatus(h: CapabilityHealth, m: String? = null) { mutableState.update { it.copy(filesStatus = status(h, m)) } }
    private fun updateSkillsStatus(h: CapabilityHealth, m: String? = null) { mutableState.update { it.copy(skillsStatus = status(h, m)) } }
    private fun updateMcpStatus(h: CapabilityHealth, m: String? = null) { mutableState.update { it.copy(mcpStatus = status(h, m)) } }
    private fun updateSubagentsStatus(h: CapabilityHealth, m: String? = null) { mutableState.update { it.copy(subagentsStatus = status(h, m)) } }
    private fun updateMemoryStatus(h: CapabilityHealth, m: String? = null) { mutableState.update { it.copy(memoryStatus = status(h, m)) } }
    private fun updateTasksStatus(h: CapabilityHealth, m: String? = null) { mutableState.update { it.copy(tasksStatus = status(h, m)) } }

    private fun updateProviderFailure(e: Throwable) { mutableState.update { it.copy(providerStatus = statusFor(e)) } }
    private fun updateFilesFailure(e: Throwable) { mutableState.update { it.copy(filesStatus = statusFor(e)) } }
    private fun updateSkillsFailure(e: Throwable) { mutableState.update { it.copy(skillsStatus = statusFor(e)) } }
    private fun updateMcpFailure(e: Throwable) { mutableState.update { it.copy(mcpStatus = statusFor(e)) } }
    private fun updateSubagentsFailure(e: Throwable) { mutableState.update { it.copy(subagentsStatus = statusFor(e)) } }
    private fun updateMemoryFailure(e: Throwable) { mutableState.update { it.copy(memoryStatus = statusFor(e)) } }
    private fun updateTasksFailure(e: Throwable) { mutableState.update { it.copy(tasksStatus = statusFor(e)) } }
}
