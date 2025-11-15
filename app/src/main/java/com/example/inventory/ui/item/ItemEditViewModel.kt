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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.ItemsRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel to retrieve and update an item from the [ItemsRepository]'s data source.
 */
class ItemEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val itemsRepository: ItemsRepository
) : ViewModel() {

    /**
     * Holds current item ui state
     */
    var itemUiState by mutableStateOf(ItemUiState())
        private set

    // Состояние ошибок валидации
    var validationErrors by mutableStateOf(ValidationErrors())
        private set

    private val itemId: Int = checkNotNull(savedStateHandle[ItemEditDestination.itemIdArg])

    private var originalDataSource: com.example.inventory.data.DataSource? = null

    init {
        viewModelScope.launch {
            val originalItem = itemsRepository.getItemStream(itemId)
                .filterNotNull()
                .first()

            originalDataSource = originalItem.dataSource
            itemUiState = originalItem.toItemUiState(true)
        }
    }

    /**
     * Update the item in the [ItemsRepository]'s data source
     */
    suspend fun updateItem(): Boolean {
        val isValid = validateInputOnSave(itemUiState.itemDetails)
        if (isValid) {
            // Сохраняем оригинальный источник данных
            val updatedItem = itemUiState.itemDetails.toItem().copy(
                dataSource = originalDataSource ?: com.example.inventory.data.DataSource.MANUAL
            )

            itemsRepository.updateItem(updatedItem)
            return true
        }
        return false
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