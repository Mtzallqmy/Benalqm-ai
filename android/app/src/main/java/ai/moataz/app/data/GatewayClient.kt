package ai.moataz.app.data

import ai.moataz.app.BuildConfig
import ai.moataz.app.ServerUrlPolicy
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

class GatewayException(
    val statusCode: Int,
    override val message: String,
    val responseBody: String? = null,
) : IOException(message)

/**
 * Native Android client for the same Gateway/LangGraph boundary used by the
 * DeerFlow web application. There is intentionally no WebView dependency here.
 */
class GatewayClient(
    serverOrigin: String,
    private val bearerToken: String? = null,
) {
    val origin: String = ServerUrlPolicy.normalize(serverOrigin)

    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val streamingHttp = http.newBuilder()
        .readTimeout(Duration.ZERO)
        .callTimeout(Duration.ZERO)
        .build()

    fun probe(): GatewayProbe {
        val started = System.nanoTime()
        val models = listModels()
        val latency = (System.nanoTime() - started) / 1_000_000
        return GatewayProbe(
            origin = origin,
            latencyMs = latency,
            models = models,
            secureTransport = origin.startsWith("https://"),
        )
    }

    fun listModels(): List<ModelInfo> {
        val root = requestObject("/api/models")
        val data = root.optJSONArray("models") ?: JSONArray()
        return buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                add(
                    ModelInfo(
                        name = name,
                        model = item.optString("model", name),
                        displayName = item.optString("display_name").takeIf { it.isNotBlank() } ?: name,
                        description = item.optString("description").takeIf { it.isNotBlank() },
                        supportsThinking = item.optBoolean("supports_thinking", false),
                        supportsReasoningEffort = item.optBoolean("supports_reasoning_effort", false),
                    ),
                )
            }
        }
    }

    fun searchThreads(limit: Int = 100, offset: Int = 0): List<ThreadSummary> {
        val body = JSONObject()
            .put("limit", limit.coerceIn(1, 1000))
            .put("offset", offset.coerceAtLeast(0))
        val response = requestArray("/api/langgraph/threads/search", "POST", body)
        return buildList {
            for (index in 0 until response.length()) {
                val item = response.optJSONObject(index) ?: continue
                val id = item.optString("thread_id").takeIf { it.isNotBlank() } ?: continue
                val values = item.optJSONObject("values")
                val metadata = item.optJSONObject("metadata")
                val title = values?.optString("title")?.takeIf { it.isNotBlank() }
                    ?: metadata?.optString("title")?.takeIf { it.isNotBlank() }
                    ?: "New conversation"
                add(
                    ThreadSummary(
                        id = id,
                        title = title,
                        status = item.optString("status", "idle"),
                        createdAt = item.optString("created_at"),
                        updatedAt = item.optString("updated_at"),
                    ),
                )
            }
        }
    }

    fun createThread(): ThreadSummary {
        val request = JSONObject().put("metadata", JSONObject())
        val item = requestObject("/api/langgraph/threads", "POST", request)
        val id = item.optString("thread_id")
        if (id.isBlank()) throw IOException("Gateway created a thread without thread_id")
        return ThreadSummary(
            id = id,
            title = item.optJSONObject("values")?.optString("title")?.takeIf { it.isNotBlank() }
                ?: "New conversation",
            status = item.optString("status", "idle"),
            createdAt = item.optString("created_at"),
            updatedAt = item.optString("updated_at"),
        )
    }

    fun getThreadMessages(threadId: String): List<ChatMessage> {
        val path = "/api/threads/${encodeSegment(threadId)}/messages?limit=200"
        return runCatching {
            parseEventRows(requestArray(path))
        }.getOrElse {
            val state = requestObject("/api/langgraph/threads/${encodeSegment(threadId)}/state")
            parseMessagesFromValues(state.optJSONObject("values") ?: JSONObject())
        }
    }

    fun getThreadState(threadId: String): JSONObject =
        requestObject("/api/langgraph/threads/${encodeSegment(threadId)}/state")

    fun streamRun(
        threadId: String,
        text: String,
        modelName: String?,
        thinkingEnabled: Boolean,
        onRunCreated: (String?) -> Unit,
        onFrame: (StreamFrame) -> Unit,
    ): String? {
        val messageId = UUID.randomUUID().toString()
        val message = JSONObject()
            .put("type", "human")
            .put("id", messageId)
            .put(
                "content",
                JSONArray().put(JSONObject().put("type", "text").put("text", text)),
            )

        val context = JSONObject().put("thinking_enabled", thinkingEnabled)
        if (!modelName.isNullOrBlank()) context.put("model_name", modelName)

        val payload = JSONObject()
            .put("input", JSONObject().put("messages", JSONArray().put(message)))
            .put("context", context)
            .put("stream_mode", JSONArray().put("values").put("updates").put("custom"))
            .put("stream_subgraphs", true)
            .put("on_disconnect", "continue")
            .put("multitask_strategy", "reject")

        val request = baseRequest("/api/langgraph/threads/${encodeSegment(threadId)}/runs/stream")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .post(payload.toString().toRequestBody(jsonType))
            .build()

        streamingHttp.newCall(request).execute().use { response ->
            val rawError = if (!response.isSuccessful) response.body?.string() else null
            if (!response.isSuccessful) throw gatewayError(response.code, rawError)

            val runId = response.header("Content-Location")
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
            onRunCreated(runId)

            val source = response.body?.source() ?: throw IOException("Empty SSE response")
            var event = "message"
            var eventId: String? = null
            val data = StringBuilder()

            fun dispatch() {
                if (data.isEmpty()) return
                onFrame(
                    StreamFrame(
                        event = event,
                        id = eventId,
                        rawData = data.toString(),
                    ),
                )
                event = "message"
                eventId = null
                data.clear()
            }

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.isEmpty() -> dispatch()
                    line.startsWith(":") -> Unit // heartbeat/comment
                    line.startsWith("event:") -> event = line.substringAfter(':').trim()
                    line.startsWith("id:") -> eventId = line.substringAfter(':').trim().ifBlank { null }
                    line.startsWith("data:") -> {
                        if (data.isNotEmpty()) data.append('\n')
                        data.append(line.substringAfter(':').trimStart())
                    }
                }
            }
            dispatch()
            return runId
        }
    }

    fun cancelRun(threadId: String, runId: String) {
        val path = "/api/langgraph/threads/${encodeSegment(threadId)}/runs/${encodeSegment(runId)}/cancel?action=interrupt&wait=false"
        execute(baseRequest(path).post(ByteArray(0).toRequestBody(null)).build()).close()
    }

    fun parseMessagesFrame(frame: StreamFrame): List<ChatMessage>? {
        if (frame.event != "values") return null
        val value = runCatching { JSONObject(frame.rawData) }.getOrNull() ?: return null
        return parseMessagesFromValues(value)
    }

    private fun parseEventRows(rows: JSONArray): List<ChatMessage> = buildList {
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val content = when (val raw = row.opt("content")) {
                is JSONObject -> JSONObject(raw.toString())
                is String -> runCatching { JSONObject(raw) }.getOrNull()
                else -> null
            } ?: continue
            if (!row.optString("run_id").isNullOrBlank()) {
                val kwargs = content.optJSONObject("additional_kwargs") ?: JSONObject().also {
                    content.put("additional_kwargs", it)
                }
                if (!kwargs.has("run_id")) kwargs.put("run_id", row.optString("run_id"))
            }
            parseMessage(content)?.let(::add)
        }
    }

    private fun parseMessagesFromValues(values: JSONObject): List<ChatMessage> {
        val messages = values.optJSONArray("messages") ?: return emptyList()
        return buildList {
            for (index in 0 until messages.length()) {
                val message = messages.optJSONObject(index) ?: continue
                parseMessage(message)?.let(::add)
            }
        }
    }

    private fun parseMessage(message: JSONObject): ChatMessage? {
        val kwargs = message.optJSONObject("additional_kwargs")
        if (kwargs?.optBoolean("hide_from_ui", false) == true) return null
        val rawRole = message.optString("type", message.optString("role", ""))
        val role = when (rawRole.lowercase()) {
            "human", "user" -> MessageRole.User
            "ai", "assistant" -> MessageRole.Assistant
            "tool" -> MessageRole.Tool
            "system" -> MessageRole.System
            "remove" -> return null
            else -> MessageRole.System
        }
        val text = extractText(message.opt("content"))
        val toolCalls = buildList {
            val calls = message.optJSONArray("tool_calls")
                ?: kwargs?.optJSONArray("tool_calls")
                ?: JSONArray()
            for (index in 0 until calls.length()) {
                val call = calls.optJSONObject(index) ?: continue
                val name = call.optString("name").takeIf { it.isNotBlank() }
                    ?: call.optJSONObject("function")?.optString("name")?.takeIf { it.isNotBlank() }
                if (name != null) add(name)
            }
        }
        if (text.isBlank() && toolCalls.isEmpty() && role != MessageRole.Tool) return null
        return ChatMessage(
            id = message.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            role = role,
            text = text,
            name = message.optString("name").takeIf { it.isNotBlank() },
            runId = kwargs?.optString("run_id")?.takeIf { it.isNotBlank() },
            toolCalls = toolCalls,
        )
    }

    private fun extractText(value: Any?): String = when (value) {
        null, JSONObject.NULL -> ""
        is String -> value
        is JSONArray -> buildList {
            for (index in 0 until value.length()) {
                val part = value.opt(index)
                when (part) {
                    is String -> add(part)
                    is JSONObject -> {
                        val type = part.optString("type")
                        when (type) {
                            "text", "input_text", "output_text" -> part.optString("text").takeIf { it.isNotBlank() }?.let(::add)
                            else -> part.optString("text").takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }
            }
        }.joinToString("\n")
        is JSONObject -> value.optString("text").ifBlank { value.optString("content") }
        else -> value.toString()
    }

    private fun requestObject(path: String, method: String = "GET", body: JSONObject? = null): JSONObject {
        val responseBody = requestText(path, method, body)
        return runCatching { JSONObject(responseBody) }
            .getOrElse { throw IOException("Invalid JSON object from Gateway", it) }
    }

    private fun requestArray(path: String, method: String = "GET", body: JSONObject? = null): JSONArray {
        val responseBody = requestText(path, method, body)
        return runCatching { JSONArray(responseBody) }
            .getOrElse { throw IOException("Invalid JSON array from Gateway", it) }
    }

    private fun requestText(path: String, method: String, body: JSONObject?): String {
        val builder = baseRequest(path)
        val request = when (method) {
            "GET" -> builder.get().build()
            "POST" -> builder.post((body ?: JSONObject()).toString().toRequestBody(jsonType)).build()
            else -> error("Unsupported method: $method")
        }
        execute(request).use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw gatewayError(response.code, text)
            return text
        }
    }

    private fun execute(request: Request) = http.newCall(request).execute()

    private fun baseRequest(path: String): Request.Builder {
        val normalizedPath = if (path.startsWith('/')) path else "/$path"
        return Request.Builder()
            .url(origin + normalizedPath)
            .header("Accept", "application/json")
            .header("User-Agent", "moataz-ai-android/${BuildConfig.VERSION_NAME}")
            .header("X-Moataz-Client", "android-native")
            .apply {
                if (!bearerToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $bearerToken")
                }
            }
    }

    private fun gatewayError(code: Int, body: String?): GatewayException {
        val detail = body?.let {
            runCatching { JSONObject(it).optString("detail") }.getOrNull()
        }?.takeIf { it.isNotBlank() }
        return GatewayException(
            statusCode = code,
            message = detail ?: "Gateway request failed (HTTP $code)",
            responseBody = body,
        )
    }

    private fun encodeSegment(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}
