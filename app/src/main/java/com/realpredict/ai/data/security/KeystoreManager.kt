package com.realpredict.ai.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Gestor criptográfico respaldado por Android Keystore (Sección 10).
 * Cifra y protege las API Keys sin exponerlas en texto plano.
 */
class KeystoreManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "realpredict_secure_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveEncryptedApiKey(providerId: String, apiKey: String) {
        sharedPreferences.edit()
            .putString("key_$providerId", apiKey)
            .apply()
    }

    fun getEncryptedApiKey(providerId: String): String? {
        return sharedPreferences.getString("key_$providerId", null)
    }

    fun deleteApiKey(providerId: String) {
        sharedPreferences.edit()
            .remove("key_$providerId")
            .apply()
    }

    fun clearAllKeys() {
        sharedPreferences.edit().clear().apply()
    }
}
