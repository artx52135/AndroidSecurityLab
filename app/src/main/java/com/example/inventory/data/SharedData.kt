package com.example.inventory.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.inventory.ui.item.ItemDetails
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.content.edit

data class ShareData(val text: String = "") {
    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return text.hashCode()
    }
}

data class LoadData(val needToLoad: Boolean = false, val data: ItemDetails? = null)

object SharedData {
    val dataToShare: MutableStateFlow<ShareData> = MutableStateFlow(ShareData())
    val dataToSave: MutableStateFlow<ShareData> = MutableStateFlow(ShareData())
    val dataToLoad: MutableStateFlow<LoadData> = MutableStateFlow(LoadData())
    var preferences: Preferences? = null

    fun initialize(context: Context) {
        if (preferences == null) {
            preferences = Preferences(context)
        }
    }
}

class Preferences(context: Context) {
    val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        sharedPreferences.edit {
            if (!sharedPreferences.contains(HIDE_IMPORTANT_DATA)) {
                putBoolean(HIDE_IMPORTANT_DATA, false)
            }
            if (!sharedPreferences.contains(PROHIBIT_SENDING_DATA)) {
                putBoolean(PROHIBIT_SENDING_DATA, false)
            }
            if (!sharedPreferences.contains(USE_DEFAULT_ITEMS_QUANTITY)) {
                putBoolean(USE_DEFAULT_ITEMS_QUANTITY, false)
            }
            if (!sharedPreferences.contains(DEFAULT_ITEMS_QUANTITY)) {
                putInt(DEFAULT_ITEMS_QUANTITY, 1)
            }
        }
    }

    companion object {
        const val HIDE_IMPORTANT_DATA: String = "HIDE_IMPORTANT_DATA"
        const val PROHIBIT_SENDING_DATA: String = "PROHIBIT_SENDING_DATA"
        const val USE_DEFAULT_ITEMS_QUANTITY: String = "USE_DEFAULT_ITEMS_QUANTITY"
        const val DEFAULT_ITEMS_QUANTITY: String = "DEFAULT_ITEMS_QUANTITY"
    }
}