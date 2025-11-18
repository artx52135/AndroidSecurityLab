package com.example.inventory.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Менеджер для шифрования/дешифрования файлов с использованием AES-GCM
 */
class FileEncryptionManager(private val context: Context) {

    private val gson = Gson()

    // Для демонстрации используем фиксированный ключ. В продакшене используйте Android Keystore
    private val secretKey: SecretKey by lazy {
        val keyBytes = "MySuperSecretKeyForDemo123".toByteArray(Charsets.UTF_8)
        val paddedKey = ByteArray(32) // 256-bit key
        System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.size.coerceAtMost(32))
        SecretKeySpec(paddedKey, "AES")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun saveItemToEncryptedFile(item: Item): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = gson.toJson(item)
            val encryptedData = encryptData(jsonString.toByteArray(Charsets.UTF_8))

            // Создаем имя файла
            val fileName = "item_${System.currentTimeMillis()}_${item.name.replace(" ", "_")}.enc"

            // Используем MediaStore для сохранения в Downloads папку
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Inventory")
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { fileUri ->
                resolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(encryptedData.toByteArray(Charsets.UTF_8))
                    return@withContext true
                }
            }

            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveItemToEncryptedFileInCache(item: Item): Uri? = withContext(Dispatchers.IO) {
        try {
            val jsonString = gson.toJson(item)
            val encryptedData = encryptData(jsonString.toByteArray(Charsets.UTF_8))

            // Сохраняем во внутреннее хранилище приложения
            val fileName = "item_${System.currentTimeMillis()}_${item.name.replace(" ", "_")}.enc"
            val file = File(context.cacheDir, fileName)

            file.writeText(encryptedData)

            // Возвращаем URI файла
            file.toUri()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun loadItemFromEncryptedFile(uri: Uri): Item? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val encryptedData = inputStream.bufferedReader().use { it.readText() }
                val decryptedBytes = decryptData(encryptedData)
                val jsonString = String(decryptedBytes, Charsets.UTF_8)
                gson.fromJson(jsonString, Item::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun encryptData(data: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12) // 96-bit IV для GCM
        SecureRandom().nextBytes(iv)

        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val encrypted = cipher.doFinal(data)
        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)

        return Base64.encodeToString(result, Base64.DEFAULT)
    }

    private fun decryptData(encryptedData: String): ByteArray {
        val data = Base64.decode(encryptedData, Base64.DEFAULT)
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(encrypted)
    }
}