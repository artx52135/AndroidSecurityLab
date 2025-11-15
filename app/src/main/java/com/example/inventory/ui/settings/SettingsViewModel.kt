package com.example.inventory.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.inventory.data.AppSettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    private val settingsManager: AppSettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                hideSensitiveData = settingsManager.hideSensitiveData,
                disableSharing = settingsManager.disableSharing,
                useDefaultQuantity = settingsManager.useDefaultQuantity,
                defaultQuantity = settingsManager.defaultQuantity.toString()
            )
        }
    }

    fun updateHideSensitiveData(hide: Boolean) {
        settingsManager.hideSensitiveData = hide
        _uiState.update { it.copy(hideSensitiveData = hide) }
    }

    fun updateDisableSharing(disable: Boolean) {
        settingsManager.disableSharing = disable
        _uiState.update { it.copy(disableSharing = disable) }
    }

    fun updateUseDefaultQuantity(use: Boolean) {
        settingsManager.useDefaultQuantity = use
        _uiState.update { it.copy(useDefaultQuantity = use) }
    }

    fun updateDefaultQuantity(quantity: String) {
        val quantityInt = quantity.toIntOrNull() ?: 1
        settingsManager.defaultQuantity = quantityInt
        _uiState.update { it.copy(defaultQuantity = quantity) }
    }
}

data class SettingsUiState(
    val hideSensitiveData: Boolean = false,
    val disableSharing: Boolean = false,
    val useDefaultQuantity: Boolean = false,
    val defaultQuantity: String = "1"
)