package ai.moataz.app

import java.net.URI
import java.util.Locale

/**
 * Normalizes the user-selected moataz ai Gateway origin.
 *
 * The Android client talks to DeerFlow/moataz ai APIs directly. It never
 * accepts credentials embedded in the URL and it strips path/query fragments
 * so all API routes are resolved from one stable origin.
 */
object ServerUrlPolicy {
    @JvmStatic
    fun normalize(raw: String?): String {
        require(!raw.isNullOrBlank()) { "Server URL is required" }
        val candidate = raw.trim().let { if ("://" in it) it else "http://$it" }
        val uri = runCatching { URI(candidate) }
            .getOrElse { throw IllegalArgumentException("Invalid server URL", it) }
        val scheme = uri.scheme?.lowercase(Locale.ROOT).orEmpty()
        require(scheme == "http" || scheme == "https") { "Only http:// and https:// are supported" }
        require(!uri.host.isNullOrBlank()) { "Server URL must include a host" }
        require(uri.userInfo == null) { "Credentials are not allowed in the server URL" }

        val normalized = URI(
            scheme,
            null,
            uri.host.lowercase(Locale.ROOT),
            uri.port,
            cleanPath(uri.path),
            null,
            null,
        ).toString()
        return normalized.trimEnd('/')
    }

    @JvmStatic
    fun isTrustedOrigin(serverUrl: String?, trustedOrigin: String?): Boolean {
        if (trustedOrigin.isNullOrBlank()) return false
        return runCatching {
            val server = URI(normalize(serverUrl))
            val trusted = URI(normalize(trustedOrigin))
            server.scheme.equals(trusted.scheme, ignoreCase = true) &&
                server.host.equals(trusted.host, ignoreCase = true) &&
                originPort(server) == originPort(trusted)
        }.getOrDefault(false)
    }

    @JvmStatic
    fun isPrivateLan(serverUrl: String): Boolean {
        val uri = URI(normalize(serverUrl))
        val host = uri.host.lowercase(Locale.ROOT)
        return host == "localhost" || host == "127.0.0.1" || host == "::1" ||
            host.startsWith("10.") || host.startsWith("192.168.") ||
            host.matches(Regex("172\\.(1[6-9]|2\\d|3[01])\\..*")) ||
            host.endsWith(".local")
    }

    private fun originPort(uri: URI): Int = when {
        uri.port >= 0 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }

    private fun cleanPath(path: String?): String? = path
        ?.takeUnless { it.isBlank() || it == "/" }
        ?.trimEnd('/')
}
