package com.example.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.inventory.InventoryApplication
import com.example.inventory.data.AppContainer
import com.example.inventory.ui.home.HomeViewModel
import com.example.inventory.ui.item.ItemDetailsViewModel
import com.example.inventory.ui.item.ItemEditViewModel
import com.example.inventory.ui.item.ItemEntryViewModel
import com.example.inventory.ui.settings.SettingsViewModel
import kotlinx.coroutines.channels.Channel


/**
 * Provides Factory to create instance of ViewModel for the entire Inventory app
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        // Initializer for ItemEntryViewModel
        initializer {
            val application = (this[APPLICATION_KEY] as InventoryApplication)
            val container = application.container
            ItemEntryViewModel(
                itemsRepository = container.itemsRepository,
                fileEncryptionManager = container.fileEncryptionManager
            )
        }
        // Initializer for ItemEditViewModel
        initializer {
            val application = (this[APPLICATION_KEY] as InventoryApplication)
            val container = application.container
            ItemEditViewModel(
                this.createSavedStateHandle(),
                container.itemsRepository
            )
        }
        // Initializer for ItemDetailsViewModel
        initializer {
            val application = (this[APPLICATION_KEY] as InventoryApplication)
            ItemDetailsViewModel(
                this.createSavedStateHandle(),
                application.container.itemsRepository
            )
        }
        // Initializer for HomeViewModel
        initializer {
            val application = (this[APPLICATION_KEY] as InventoryApplication)
            HomeViewModel(application.container.itemsRepository)
        }
        // Initializer for SettingsViewModel
        initializer {
            SettingsViewModel()
        }
    }
}

///**
// * Extension function to queries for [Application] object and returns an instance of
// * [InventoryApplication].
// */
//fun Channel.Factory.createInventoryApplication(): InventoryApplication {
//    val application = (this[APPLICATION_KEY] as InventoryApplication)
//    return application
//}