package ai.moataz.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Native management client for DeerFlow/moataz ai capabilities that live
 * outside the chat/run protocol. Nothing in this class renders web content;
 * every operation talks directly to the existing Gateway HTTP API.
 */
class WorkspaceGatewayClient(
    private val origin: String,
    private val bearerToken: String?,
    private val http: OkHttpClient = sharedHttp,
) {
    data class Provider(
        val id: String,
        val name: String,
        val type: String,
        val baseUrl: String,
        val enabled: Boolean,
        val isDefault: Boolean,
        val modelCount: Int,
        val raw: JSONObject,
    )

    data class Skill(
        val name: String,
        val description: String,
        val category: String,
        val enabled: Boolean,
        val editable: Boolean,
        val raw: JSONObject,
    )

    data class McpServer(
        val name: String,
        val description: String,
        val transport: String,
        val enabled: Boolean,
        val raw: JSONObject,
    )

    data class Subagent(
        val name: String,
        val displayName: String,
        val description: String,
        val model: String,
        val source: String,
        val enabled: Boolean,
        val editable: Boolean,
        val raw: JSONObject,
    )

    data class MemoryFact(
        val id: String,
        val content: String,
        val category: String,
        val confidence: Double,
        val raw: JSONObject,
    )

    data class MemorySnapshot(
        val facts: List<MemoryFact>,
        val workContext: String,
        val personalContext: String,
        val topOfMind: String,
        val raw: JSONObject,
    )

    data class ScheduledTask(
        val id: String,
        val title: String,
        val prompt: String,
        val scheduleType: String,
        val status: String,
        val nextRunAt: String?,
        val timezone: String,
        val raw: JSONObject,
    )

    data class ThreadFile(
        val filename: String,
        val size: Long,
        val virtualPath: String,
        val artifactUrl: String,
        val extension: String?,
        val raw: JSONObject,
    )

    data class UploadLimits(
        val maxFiles: Int,
        val maxFileSize: Long,
        val maxTotalSize: Long,
    )

    data class BrowserNavigation(
        val url: String,
        val title: String,
        val screenshot: String?,
    )

    suspend fun listProviders(): List<Provider> {
        val body = getJson("/api/providers")
        val defaultId = runCatching { getJson("/api/providers/default").optString("provider_id") }.getOrNull()
        val array = body.optJSONArray("providers") ?: body.optJSONArray("items") ?: JSONArray()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { item ->
                val models = item.optJSONArray("models")
                Provider(
                    id = item.optString("id", item.optString("provider_id")),
                    name = item.optString("name", item.optString("display_name", "Provider")),
                    type = item.optString("provider_type", item.optString("type", "openai_compatible")),
                    baseUrl = item.optString("base_url"),
                    enabled = item.optBoolean("enabled", true),
                    isDefault = item.optBoolean("is_default", false) || item.optString("id") == defaultId,
                    modelCount = models?.length() ?: item.optInt("model_count", 0),
                    raw = item,
                )
            }
        }
    }

    suspend fun testProvider(providerType: String, baseUrl: String, apiKey: String): JSONObject = postJson(
        "/api/providers/test",
        JSONObject().put("provider_type", providerType).put("base_url", baseUrl).put("api_key", apiKey),
    )

    suspend fun discoverProviderModels(providerType: String, baseUrl: String, apiKey: String): JSONObject = postJson(
        "/api/providers/discover",
        JSONObject().put("provider_type", providerType).put("base_url", baseUrl).put("api_key", apiKey),
    )

    suspend fun probeProviderModel(
        providerType: String,
        baseUrl: String,
        apiKey: String,
        model: String,
    ): JSONObject = postJson(
        "/api/providers/probe",
        JSONObject()
            .put("provider_type", providerType)
            .put("base_url", baseUrl)
            .put("api_key", apiKey)
            .put("model", model),
    )

    suspend fun saveProvider(payload: JSONObject): JSONObject = postJson("/api/providers", payload)

    suspend fun setDefaultProvider(providerId: String): JSONObject = putJson(
        "/api/providers/default",
        JSONObject().put("provider_id", providerId),
    )

    suspend fun deleteProvider(providerId: String) {
        delete("/api/providers/${path(providerId)}")
    }

    suspend fun listSkills(): List<Skill> {
        val body = getJson("/api/skills")
        val array = body.optJSONArray("skills") ?: JSONArray()
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let { item ->
                Skill(
                    name = item.optString("name"),
                    description = item.optString("description"),
                    category = item.optString("category"),
                    enabled = item.optBoolean("enabled", true),
                    editable = item.optBoolean("editable", false),
                    raw = item,
                )
            }
        }
    }

    suspend fun setSkillEnabled(name: String, enabled: Boolean): JSONObject = putJson(
        "/api/skills/${path(name)}",
        JSONObject().put("enabled", enabled),
    )

    suspend fun reloadSkills(): JSONObject = postJson("/api/skills/reload", JSONObject())

    suspend fun getCustomSkill(name: String): JSONObject = getJson("/api/skills/custom/${path(name)}")

    suspend fun updateCustomSkill(name: String, content: String): JSONObject = putJson(
        "/api/skills/custom/${path(name)}",
        JSONObject().put("content", content),
    )

    suspend fun deleteCustomSkill(name: String) {
        delete("/api/skills/custom/${path(name)}")
    }

    suspend fun rollbackCustomSkill(name: String, historyIndex: Int = -1): JSONObject = postJson(
        "/api/skills/custom/${path(name)}/rollback",
        JSONObject().put("history_index", historyIndex),
    )

    suspend fun listMcpServers(): List<McpServer> {
        val body = getJson("/api/mcp/config")
        val servers = body.optJSONObject("mcp_servers") ?: JSONObject()
        return servers.keys().asSequence().map { name ->
            val item = servers.optJSONObject(name) ?: JSONObject()
            val transport = item.optString("transport").ifBlank {
                when {
                    item.has("command") -> "stdio"
                    item.optString("url").contains("sse", ignoreCase = true) -> "sse"
                    else -> "http"
                }
            }
            McpServer(
                name = name,
                description = item.optString("description"),
                transport = transport,
                enabled = item.optBoolean("enabled", true),
                raw = item,
            )
        }.toList()
    }

    suspend fun setMcpServerEnabled(name: String, enabled: Boolean): JSONObject = patchJson(
        "/api/mcp/config",
        JSONObject().put("server_name", name).put("enabled", enabled),
    )

    suspend fun resetMcpCache(): JSONObject = postJson("/api/mcp/cache/reset", JSONObject())

    suspend fun createMcpServer(name: String, server: JSONObject): JSONObject = postJson(
        "/api/mcp/config/servers",
        JSONObject().put("mcp_servers", JSONObject().put(name, server)),
    )

    suspend fun replaceMcpServer(name: String, server: JSONObject): JSONObject = putJson(
        "/api/mcp/config/server",
        JSONObject().put("server_name", name).put("server", server),
    )

    suspend fun deleteMcpServer(name: String) {
        delete("/api/mcp/config/servers/${path(name)}")
    }

    suspend fun listSubagents(): List<Subagent> {
        val body = getJson("/api/subagents")
        val array = body.optJSONArray("subagents") ?: JSONArray()
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let { item ->
                Subagent(
                    name = item.optString("name"),
                    displayName = item.optString("display_name").ifBlank { item.optString("name") },
                    description = item.optString("description"),
                    model = item.optString("model", "inherit"),
                    source = item.optString("source"),
                    enabled = item.optBoolean("enabled", true),
                    editable = item.optBoolean("editable", false),
                    raw = item,
                )
            }
        }
    }

    suspend fun createSubagent(payload: JSONObject): JSONObject = postJson("/api/subagents", payload)

    suspend fun updateSubagent(name: String, payload: JSONObject): JSONObject = putJson(
        "/api/subagents/${path(name)}",
        payload,
    )

    suspend fun deleteSubagent(name: String) {
        delete("/api/subagents/${path(name)}")
    }

    suspend fun getMemory(): MemorySnapshot {
        val body = getJson("/api/memory")
        val factsArray = body.optJSONArray("facts") ?: JSONArray()
        val facts = (0 until factsArray.length()).mapNotNull { i ->
            factsArray.optJSONObject(i)?.let { item ->
                MemoryFact(
                    id = item.optString("id"),
                    content = item.optString("content"),
                    category = item.optString("category", "context"),
                    confidence = item.optDouble("confidence", 0.5),
                    raw = item,
                )
            }
        }
        val user = body.optJSONObject("user") ?: JSONObject()
        return MemorySnapshot(
            facts = facts,
            workContext = user.optJSONObject("workContext")?.optString("summary").orEmpty(),
            personalContext = user.optJSONObject("personalContext")?.optString("summary").orEmpty(),
            topOfMind = user.optJSONObject("topOfMind")?.optString("summary").orEmpty(),
            raw = body,
        )
    }

    suspend fun createMemoryFact(content: String, category: String, confidence: Double): MemorySnapshot {
        postJson(
            "/api/memory/facts",
            JSONObject().put("content", content).put("category", category).put("confidence", confidence),
        )
        return getMemory()
    }

    suspend fun deleteMemoryFact(id: String): MemorySnapshot {
        requestJson("DELETE", "/api/memory/facts/${path(id)}", null)
        return getMemory()
    }

    suspend fun reloadMemory(): MemorySnapshot {
        postJson("/api/memory/reload", JSONObject())
        return getMemory()
    }

    suspend fun clearMemory(): MemorySnapshot {
        requestJson("DELETE", "/api/memory", null)
        return getMemory()
    }

    suspend fun listScheduledTasks(): List<ScheduledTask> {
        val value = requestValue("GET", "/api/scheduled-tasks", null)
        val array = when (value) {
            is JSONArray -> value
            is JSONObject -> value.optJSONArray("tasks") ?: JSONArray()
            else -> JSONArray()
        }
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let { item ->
                ScheduledTask(
                    id = item.optString("id"),
                    title = item.optString("title"),
                    prompt = item.optString("prompt"),
                    scheduleType = item.optString("schedule_type"),
                    status = item.optString("status"),
                    nextRunAt = item.optString("next_run_at").takeIf { it.isNotBlank() && it != "null" },
                    timezone = item.optString("timezone", "UTC"),
                    raw = item,
                )
            }
        }
    }

    suspend fun createScheduledTask(payload: JSONObject): JSONObject = postJson("/api/scheduled-tasks", payload)

    suspend fun pauseScheduledTask(id: String): JSONObject = postJson(
        "/api/scheduled-tasks/${path(id)}/pause",
        JSONObject(),
    )

    suspend fun resumeScheduledTask(id: String): JSONObject = postJson(
        "/api/scheduled-tasks/${path(id)}/resume",
        JSONObject(),
    )

    suspend fun triggerScheduledTask(id: String): JSONObject = postJson(
        "/api/scheduled-tasks/${path(id)}/trigger",
        JSONObject(),
    )

    suspend fun deleteScheduledTask(id: String) {
        delete("/api/scheduled-tasks/${path(id)}")
    }

    suspend fun getUploadLimits(threadId: String): UploadLimits {
        val body = getJson("/api/threads/${path(threadId)}/uploads/limits")
        return UploadLimits(
            maxFiles = body.optInt("max_files", 10),
            maxFileSize = body.optLong("max_file_size", 50L * 1024L * 1024L),
            maxTotalSize = body.optLong("max_total_size", 100L * 1024L * 1024L),
        )
    }

    suspend fun listThreadFiles(threadId: String): List<ThreadFile> {
        val body = getJson("/api/threads/${path(threadId)}/uploads/list")
        val array = body.optJSONArray("files") ?: JSONArray()
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let { item ->
                ThreadFile(
                    filename = item.optString("filename"),
                    size = item.optLong("size"),
                    virtualPath = item.optString("virtual_path"),
                    artifactUrl = item.optString("artifact_url"),
                    extension = item.optString("extension").takeIf { it.isNotBlank() },
                    raw = item,
                )
            }
        }
    }

    suspend fun uploadThreadFile(
        threadId: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray,
    ): JSONObject = withContext(Dispatchers.IO) {
        val partBody = bytes.toRequestBody(mimeType.toMediaType())
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("files", filename, partBody)
            .build()
        execute(buildRequest("/api/threads/${path(threadId)}/uploads").post(multipart).build()).asJsonObject()
    }

    suspend fun deleteThreadFile(threadId: String, filename: String) {
        delete("/api/threads/${path(threadId)}/uploads/${path(filename)}")
    }

    fun artifactUrl(threadId: String, virtualPath: String, download: Boolean = false): String {
        val clean = virtualPath.trimStart('/')
        return "$origin/api/threads/${path(threadId)}/artifacts/${pathSegments(clean)}${if (download) "?download=true" else ""}"
    }

    suspend fun fetchArtifact(threadId: String, virtualPath: String): Pair<ByteArray, String?> = withContext(Dispatchers.IO) {
        val request = buildRequest(
            "/api/threads/${path(threadId)}/artifacts/${pathSegments(virtualPath.trimStart('/'))}",
        ).get().build()
        val response = execute(request)
        val contentType = response.header("Content-Type")
        response.body?.bytes().orEmpty() to contentType
    }

    suspend fun navigateBrowser(threadId: String, url: String): BrowserNavigation {
        val body = postJson(
            "/api/threads/${path(threadId)}/browser/navigate",
            JSONObject().put("url", url),
        )
        return BrowserNavigation(
            url = body.optString("url"),
            title = body.optString("title"),
            screenshot = body.optString("screenshot").takeIf { it.isNotBlank() },
        )
    }

    suspend fun getRaw(path: String): Any = requestValue("GET", path, null)
    suspend fun postRaw(path: String, payload: JSONObject = JSONObject()): Any = requestValue("POST", path, payload)
    suspend fun putRaw(path: String, payload: JSONObject): Any = requestValue("PUT", path, payload)
    suspend fun patchRaw(path: String, payload: JSONObject): Any = requestValue("PATCH", path, payload)
    suspend fun deleteRaw(path: String): Any = requestValue("DELETE", path, null)

    private suspend fun getJson(path: String): JSONObject = requestJson("GET", path, null)
    private suspend fun postJson(path: String, payload: JSONObject): JSONObject = requestJson("POST", path, payload)
    private suspend fun putJson(path: String, payload: JSONObject): JSONObject = requestJson("PUT", path, payload)
    private suspend fun patchJson(path: String, payload: JSONObject): JSONObject = requestJson("PATCH", path, payload)

    private suspend fun delete(path: String) {
        requestValue("DELETE", path, null)
    }

    private suspend fun requestJson(method: String, path: String, payload: JSONObject?): JSONObject {
        val value = requestValue(method, path, payload)
        return value as? JSONObject ?: JSONObject().put("value", value)
    }

    private suspend fun requestValue(method: String, path: String, payload: JSONObject?): Any = withContext(Dispatchers.IO) {
        val builder = buildRequest(path)
        val requestBody = payload?.toString()?.toRequestBody(JSON)
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody ?: EMPTY)
            "PUT" -> builder.put(requestBody ?: EMPTY)
            "PATCH" -> builder.patch(requestBody ?: EMPTY)
            "DELETE" -> if (requestBody == null) builder.delete() else builder.delete(requestBody)
            else -> error("Unsupported HTTP method: $method")
        }
        execute(builder.build()).asJsonValue()
    }

    private fun buildRequest(path: String): Request.Builder {
        val normalized = if (path.startsWith('/')) path else "/$path"
        return Request.Builder()
            .url(origin.trimEnd('/') + normalized)
            .header("Accept", "application/json")
            .header("User-Agent", "moataz-ai-android/1.1")
            .apply {
                if (!bearerToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $bearerToken")
                }
            }
    }

    private fun execute(request: Request): okhttp3.Response {
        val response = http.newCall(request).execute()
        if (!response.isSuccessful) {
            val text = response.body?.string().orEmpty()
            response.close()
            throw GatewayHttpException(response.code, extractError(text))
        }
        return response
    }

    private fun okhttp3.Response.asJsonObject(): JSONObject = use {
        val text = body?.string().orEmpty()
        if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun okhttp3.Response.asJsonValue(): Any = use {
        val text = body?.string().orEmpty()
        if (text.isBlank()) return@use JSONObject()
        val trimmed = text.trim()
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed)
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> trimmed
        }
    }

    private fun extractError(text: String): String {
        if (text.isBlank()) return "Gateway request failed"
        return runCatching {
            val json = JSONObject(text)
            when (val detail = json.opt("detail")) {
                is String -> detail
                null -> json.optString("message", text)
                else -> detail.toString()
            }
        }.getOrDefault(text.take(600))
    }

    private fun path(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    private fun pathSegments(value: String): String = value.split('/').filter { it.isNotBlank() }.joinToString("/") { path(it) }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val EMPTY: RequestBody = ByteArray(0).toRequestBody(null)
        private val sharedHttp = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}
