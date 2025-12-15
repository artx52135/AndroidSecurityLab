package com.example.inventory.ui.settings

import androidx.lifecycle.ViewModel
import com.example.inventory.data.SharedData
import com.example.inventory.data.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.core.content.edit

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val prefs = SharedData.preferences?.sharedPreferences
        if (prefs != null) {
            _uiState.update {
                it.copy(
                    hideSensitiveData = prefs.getBoolean(Preferences.HIDE_IMPORTANT_DATA, false),
                    disableSharing = prefs.getBoolean(Preferences.PROHIBIT_SENDING_DATA, false),
                    useDefaultQuantity = prefs.getBoolean(Preferences.USE_DEFAULT_ITEMS_QUANTITY, false),
                    defaultQuantity = prefs.getInt(Preferences.DEFAULT_ITEMS_QUANTITY, 1).toString()
                )
            }
        }
    }

    fun updateHideSensitiveData(hide: Boolean) {
        SharedData.preferences?.sharedPreferences?.edit {
            putBoolean(Preferences.HIDE_IMPORTANT_DATA, hide)
        }
        _uiState.update { it.copy(hideSensitiveData = hide) }
    }

    fun updateDisableSharing(disable: Boolean) {
        SharedData.preferences?.sharedPreferences?.edit {
            putBoolean(Preferences.PROHIBIT_SENDING_DATA, disable)
        }
        _uiState.update { it.copy(disableSharing = disable) }
    }

    fun updateUseDefaultQuantity(use: Boolean) {
        SharedData.preferences?.sharedPreferences?.edit {
            putBoolean(Preferences.USE_DEFAULT_ITEMS_QUANTITY, use)
        }
        _uiState.update { it.copy(useDefaultQuantity = use) }
    }

    fun updateDefaultQuantity(quantity: String) {
        val quantityInt = quantity.toIntOrNull() ?: 1
        SharedData.preferences?.sharedPreferences?.edit {
            putInt(Preferences.DEFAULT_ITEMS_QUANTITY, quantityInt)
        }
        _uiState.update { it.copy(defaultQuantity = quantity) }
    }
}

data class SettingsUiState(
    val hideSensitiveData: Boolean = false,
    val disableSharing: Boolean = false,
    val useDefaultQuantity: Boolean = false,
    val defaultQuantity: String = "1"
)