package ai.moataz.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class ClientProfile(
    val origin: String,
    val bearerToken: String?,
    val selectedModel: String?,
    val thinkingEnabled: Boolean,
)

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("moataz_ai_native", Context.MODE_PRIVATE)
    private val crypto = SecretStore()

    fun load(defaultOrigin: String = ""): ClientProfile = ClientProfile(
        origin = prefs.getString(KEY_ORIGIN, null)?.takeIf { it.isNotBlank() }
            ?: defaultOrigin.trim().trimEnd('/'),
        bearerToken = prefs.getString(KEY_TOKEN, null)?.let(crypto::decrypt),
        selectedModel = prefs.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() },
        thinkingEnabled = prefs.getBoolean(KEY_THINKING, true),
    )

    fun saveOrigin(origin: String) {
        prefs.edit().putString(KEY_ORIGIN, origin.trim().trimEnd('/')).apply()
    }

    fun saveBearerToken(token: String?) {
        val editor = prefs.edit()
        if (token.isNullOrBlank()) editor.remove(KEY_TOKEN)
        else editor.putString(KEY_TOKEN, crypto.encrypt(token.trim()))
        editor.apply()
    }

    fun hasBearerToken(): Boolean = prefs.contains(KEY_TOKEN)

    fun saveSelectedModel(model: String?) {
        prefs.edit().apply {
            if (model.isNullOrBlank()) remove(KEY_MODEL) else putString(KEY_MODEL, model)
        }.apply()
    }

    fun saveThinkingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_THINKING, enabled).apply()
    }

    fun clearConnection() {
        prefs.edit().remove(KEY_ORIGIN).remove(KEY_TOKEN).remove(KEY_MODEL).apply()
    }

    private class SecretStore {
        private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        private fun key(): SecretKey {
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            return generator.generateKey()
        }

        fun encrypt(value: String): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
        }

        fun decrypt(stored: String): String? = runCatching {
            val parts = stored.split(':', limit = 2)
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()
    }

    private companion object {
        const val KEY_ORIGIN = "server_origin"
        const val KEY_TOKEN = "bearer_token_v1"
        const val KEY_MODEL = "selected_model"
        const val KEY_THINKING = "thinking_enabled"
        const val KEY_ALIAS = "moataz-ai-client-secrets-v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
