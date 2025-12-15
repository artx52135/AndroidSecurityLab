/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.inventory.ui.item

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.inventory.data.SharedData
import com.example.inventory.data.Preferences
import com.example.inventory.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.NumberFormat

/**
 * ViewModel to validate and insert items in the Room database.
 */
class ItemEntryViewModel(
    private val itemsRepository: ItemsRepository,
    private val fileEncryptionManager: FileEncryptionManager? = null
) : ViewModel() {

    /**
     * Holds current item ui state
     */
    var itemUiState by mutableStateOf(ItemUiState())
        private set

    // Состояние ошибок валидации
    var validationErrors by mutableStateOf(ValidationErrors())
        private set

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState = _loadState.asStateFlow()

    // Добавляем флаг для навигации
    private val _navigateToHome = MutableStateFlow(false)
    val navigateToHome = _navigateToHome.asStateFlow()

    init {
        // Устанавливаем количество по умолчанию если нужно
        val useDefaultQuantity = SharedData.preferences?.sharedPreferences
            ?.getBoolean(Preferences.USE_DEFAULT_ITEMS_QUANTITY, false)

        if (useDefaultQuantity == true) {
            val defaultQuantity = SharedData.preferences?.sharedPreferences
                ?.getInt(Preferences.DEFAULT_ITEMS_QUANTITY, 1)

            updateUiState(
                itemUiState.itemDetails.copy(
                    quantity = defaultQuantity.toString()
                )
            )
        }
    }

    /**
     * Updates the [itemUiState] with the value provided in the argument.
     */
    fun updateUiState(itemDetails: ItemDetails) {
        itemUiState = ItemUiState(
            itemDetails = itemDetails,
            isEntryValid = validateInputOnSave(itemDetails)
        )
        // Сбрасываем ошибки при изменении полей
        validationErrors = ValidationErrors()
    }

    /**
     * Inserts an [Item] in the Room database
     */
    suspend fun saveItem(): Boolean {
        val isValid = validateInputOnSave(itemUiState.itemDetails)
        if (isValid) {
            itemsRepository.insertItem(itemUiState.itemDetails.toItem())
            return true
        }
        return false
    }

    /**
     * Загружает товар из зашифрованного файла
     */
    suspend fun loadItemFromFile(uri: Uri, context: Context): Boolean {
        _loadState.value = LoadState.Loading
        return try {
            val loadedItem = fileEncryptionManager?.loadItemFromEncryptedFile(uri)
            loadedItem?.let { item ->
                // Устанавливаем источник данных как FILE и сбрасываем ID
                val itemWithSource = item.copy(
                    id = 0, // ← СБРАСЫВАЕМ ID ДЛЯ СОЗДАНИЯ НОВОЙ ЗАПИСИ
                    dataSource = DataSource.FILE
                )

                // Сохраняем товар в базу данных
                itemsRepository.insertItem(itemWithSource)

                _loadState.value = LoadState.Success
                _navigateToHome.value = true // Триггер навигации
                true
            } ?: run {
                _loadState.value = LoadState.Error("Не удалось загрузить товар из файла")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _loadState.value = LoadState.Error("Ошибка при загрузке файла: ${e.message}")
            false
        }
    }

    /**
     * Сброс флага навигации
     */
    fun resetNavigation() {
        _navigateToHome.value = false
    }

    /**
     * Валидация при сохранении
     */
    private fun validateInputOnSave(uiState: ItemDetails = itemUiState.itemDetails): Boolean {
        val errors = ValidationErrors()

        // Валидация основных полей
        if (uiState.name.isBlank()) {
            errors.nameError = "Название товара обязательно"
        }
        if (uiState.price.isBlank()) {
            errors.priceError = "Цена обязательна"
        } else if (uiState.price.toDoubleOrNull() == null) {
            errors.priceError = "Неверный формат цены"
        }
        if (uiState.quantity.isBlank()) {
            errors.quantityError = "Количество обязательно"
        } else if (uiState.quantity.toIntOrNull() == null) {
            errors.quantityError = "Неверный формат количества"
        }

        // Валидация новых полей
        if (uiState.supplierEmail.isNotBlank() && !isValidEmail(uiState.supplierEmail)) {
            errors.emailError = "Неверный формат email"
        }
        if (uiState.supplierPhone.isNotBlank() && !isValidPhone(uiState.supplierPhone)) {
            errors.phoneError = "Неверный формат телефона"
        }

        validationErrors = errors

        return errors.nameError == null &&
                errors.priceError == null &&
                errors.quantityError == null &&
                errors.emailError == null &&
                errors.phoneError == null
    }

    private fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return true
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(emailRegex.toRegex())
    }

    private fun isValidPhone(phone: String): Boolean {
        if (phone.isBlank()) return true
        val phoneRegex = "^[+]?[0-9]{10,15}\$"
        return phone.matches(phoneRegex.toRegex())
    }
}

/**
 * Represents Ui State for an Item.
 */
data class ItemUiState(
    val itemDetails: ItemDetails = ItemDetails(),
    val isEntryValid: Boolean = true
)

data class ItemDetails(
    val id: Int = 0,
    val name: String = "",
    val price: String = "",
    val quantity: String = "",
    val supplierName: String = "",
    val supplierEmail: String = "",
    val supplierPhone: String = "",
    val dataSource: DataSource = DataSource.MANUAL
)

// Класс для хранения ошибок валидации
data class ValidationErrors(
    var nameError: String? = null,
    var priceError: String? = null,
    var quantityError: String? = null,
    var emailError: String? = null,
    var phoneError: String? = null
)

// Состояние загрузки файла
sealed class LoadState {
    object Idle : LoadState()
    object Loading : LoadState()
    object Success : LoadState()
    data class Error(val message: String) : LoadState()
}

/**
 * Extension function to convert [ItemDetails] to [Item]. If the value of [ItemDetails.price] is
 * not a valid [Double], then the price will be set to 0.0. Similarly if the value of
 * [ItemUiState] is not a valid [Int], then the quantity will be set to 0
 */
fun ItemDetails.toItem(): Item = Item(
    id = id,
    name = name,
    price = price.toDoubleOrNull() ?: 0.0,
    quantity = quantity.toIntOrNull() ?: 0,
    supplierName = supplierName,
    supplierEmail = supplierEmail,
    supplierPhone = supplierPhone,
    dataSource = dataSource
)

fun Item.formatedPrice(): String {
    return NumberFormat.getCurrencyInstance().format(price)
}

/**
 * Extension function to convert [Item] to [ItemUiState]
 */
fun Item.toItemUiState(isEntryValid: Boolean = false): ItemUiState = ItemUiState(
    itemDetails = this.toItemDetails(),
    isEntryValid = isEntryValid
)

/**
 * Extension function to convert [Item] to [ItemDetails]
 */
fun Item.toItemDetails(): ItemDetails = ItemDetails(
    id = id,
    name = name,
    price = price.toString(),
    quantity = quantity.toString(),
    supplierName = supplierName,
    supplierEmail = supplierEmail,
    supplierPhone = supplierPhone,
    dataSource = dataSource
)