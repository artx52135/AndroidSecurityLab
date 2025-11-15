package com.example.inventory.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Менеджер для работы с настройками приложения с базовым шифрованием
 */
class AppSettingsManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // Простое шифрование для демонстрации
    private val secretKey: SecretKey by lazy {
        generateKey()
    }

    companion object {
        private const val KEY_HIDE_SENSITIVE_DATA = "hide_sensitive_data"
        private const val KEY_DISABLE_SHARING = "disable_sharing"
        private const val KEY_USE_DEFAULT_QUANTITY = "use_default_quantity"
        private const val KEY_DEFAULT_QUANTITY = "default_quantity"

        // Ключ для хранения зашифрованных настроек
        private const val ENCRYPTED_PREFIX = "enc_"
    }

    var hideSensitiveData: Boolean
        get() = sharedPreferences.getBoolean(KEY_HIDE_SENSITIVE_DATA, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_HIDE_SENSITIVE_DATA, value).apply()

    var disableSharing: Boolean
        get() = sharedPreferences.getBoolean(KEY_DISABLE_SHARING, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_DISABLE_SHARING, value).apply()

    var useDefaultQuantity: Boolean
        get() = sharedPreferences.getBoolean(KEY_USE_DEFAULT_QUANTITY, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_USE_DEFAULT_QUANTITY, value).apply()

    var defaultQuantity: Int
        get() = sharedPreferences.getInt(KEY_DEFAULT_QUANTITY, 1)
        set(value) = sharedPreferences.edit().putInt(KEY_DEFAULT_QUANTITY, value).apply()

    // Методы для зашифрованного хранения (опционально)
    fun setEncryptedSetting(key: String, value: String) {
        val encrypted = encrypt(value)
        sharedPreferences.edit().putString("${ENCRYPTED_PREFIX}$key", encrypted).apply()
    }

    fun getEncryptedSetting(key: String, defaultValue: String = ""): String {
        val encrypted = sharedPreferences.getString("${ENCRYPTED_PREFIX}$key", null)
        return encrypted?.let { decrypt(it) } ?: defaultValue
    }

    private fun generateKey(): SecretKey {
        // Для демонстрации используем фиксированный ключ
        // В реальном приложении используйте Android Keystore
        val keyBytes = "MyDemoKey1234567890123456789012".toByteArray(Charsets.UTF_8)
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encrypt(data: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)

        return Base64.encodeToString(result, Base64.DEFAULT)
    }

    private fun decrypt(encryptedData: String): String {
        val data = Base64.decode(encryptedData, Base64.DEFAULT)
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }
}