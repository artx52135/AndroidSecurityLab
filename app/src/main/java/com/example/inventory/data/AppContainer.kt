package com.example.inventory.data

import android.content.Context

/**
 * App container for Dependency injection.
 */
interface AppContainer {
    val itemsRepository: ItemsRepository
    val preferences: Preferences
    val fileEncryptionManager: FileEncryptionManager
}

/**
 * [AppContainer] implementation that provides instance of [OfflineItemsRepository]
 */
class AppDataContainer(private val context: Context) : AppContainer {
    /**
     * Implementation for [ItemsRepository]
     */
    override val itemsRepository: ItemsRepository by lazy {
        OfflineItemsRepository(InventoryDatabase.getDatabase(context).itemDao())
    }

    /**
     * Implementation for [Preferences]
     */
    override val preferences: Preferences by lazy {
        // Инициализируем SharedData
        SharedData.initialize(context)
        SharedData.preferences ?: throw IllegalStateException("Preferences not initialized")
    }

    /**
     * Implementation for [FileEncryptionManager]
     */
    override val fileEncryptionManager: FileEncryptionManager by lazy {
        FileEncryptionManager(context)
    }
}