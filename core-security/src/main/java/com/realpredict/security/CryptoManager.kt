package com.realpredict.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.realpredict.common.AppError
import com.realpredict.common.Result

/**
 * Gestor criptográfico respaldado por Android Keystore (Sección 10).
 *
 * - Cifra las API Keys con AES256-GCM.
 * - Oculta las claves en la interfaz (muestra solo últimos 4 caracteres).
 * - Redacta credenciales en logs y excepciones.
 *
 * ADVERTENCIA explícita: las llamadas directas desde el teléfono no eliminan
 * completamente el riesgo de extracción en un dispositivo comprometido.
 *
 * NUNCA almacenar claves en:
 *   - el APK
 *   - código fuente versionado
 *   - archivos de preferencias ordinarios (SharedPreferences sin cifrar)
 *   - logs
 */
class CryptoManager(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Cifra y guarda la API Key de un proveedor.
     */
    fun saveApiKey(providerId: String, apiKey: String): Result<Unit> = try {
        require(apiKey.isNotBlank()) { "API Key no puede estar vacía" }
        securePrefs.edit().putString(keyFor(providerId), apiKey).apply()
        Result.Success(Unit)
    } catch (t: Throwable) {
        Result.Failure(AppError.Crypto("No se pudo guardar la clave: ${redact(t)}", t))
    }

    /**
     * Recupera la API Key descifrada. Solo para uso interno.
     */
    fun getApiKey(providerId: String): Result<String> = try {
        val key = securePrefs.getString(keyFor(providerId), null)
            ?: return Result.Failure(AppError.NotFound("No hay clave para $providerId"))
        Result.Success(key)
    } catch (t: Throwable) {
        Result.Failure(AppError.Crypto("No se pudo leer la clave: ${redact(t)}", t))
    }

    /**
     * Elimina la API Key de un proveedor específico.
     */
    fun deleteApiKey(providerId: String): Result<Unit> = try {
        securePrefs.edit().remove(keyFor(providerId)).apply()
        Result.Success(Unit)
    } catch (t: Throwable) {
        Result.Failure(AppError.Crypto("No se pudo eliminar la clave: ${redact(t)}", t))
    }

    /**
     * Elimina TODAS las claves (reset completo).
     */
    fun clearAllKeys(): Result<Unit> = try {
        securePrefs.edit().clear().apply()
        Result.Success(Unit)
    } catch (t: Throwable) {
        Result.Failure(AppError.Crypto("No se pudieron eliminar todas las claves: ${redact(t)}", t))
    }

    /**
     * Devuelve una máscara legible para mostrar en UI: ej: "sk-...AB12"
     */
    fun maskApiKey(apiKey: String): String {
        if (apiKey.length <= 8) return "****"
        val prefix = apiKey.take(3)
        val suffix = apiKey.takeLast(4)
        return "$prefix...$suffix"
    }

    fun maskApiKeyForProvider(providerId: String): String = when (val r = getApiKey(providerId)) {
        is Result.Success -> maskApiKey(r.data)
        is Result.Failure -> "—"
    }

    /**
     * Verifica si existe una clave guardada para el proveedor.
     */
    fun hasApiKey(providerId: String): Boolean =
        securePrefs.contains(keyFor(providerId))

    private fun keyFor(providerId: String) = "key_$providerId"

    /**
     * Redacción de credenciales en mensajes de error.
     * Reemplaza cualquier cadena que parezca una API Key (>= 16 chars alfanuméricos).
     */
    fun redact(t: Throwable): String {
        val msg = t.message ?: t.javaClass.simpleName
        return redactString(msg)
    }

    fun redactString(input: String): String {
        // Reemplazar secuencias largas alfanuméricas (potenciales API Keys)
        return input.replace(Regex("[A-Za-z0-9_\\-]{16,}")) { match ->
            val m = match.value
            "${m.take(3)}...${m.takeLast(2)}(redacted)"
        }
    }

    companion object {
        private const val PREF_NAME = "realpredict_secure_vault"

        /**
         * Log seguro: redacta cualquier posible API Key del mensaje.
         */
        fun secureLog(tag: String, message: String, redactor: (String) -> String) {
            Log.d(tag, redactor(message))
        }
    }
}
