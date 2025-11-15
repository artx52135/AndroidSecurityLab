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
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.InventoryTopAppBar
import com.example.inventory.R
import com.example.inventory.data.AppSettingsManager
import com.example.inventory.data.Item
import com.example.inventory.ui.AppViewModelProvider
import com.example.inventory.ui.navigation.NavigationDestination
import com.example.inventory.ui.theme.InventoryTheme
import kotlinx.coroutines.launch

object ItemDetailsDestination : NavigationDestination {
    override val route = "item_details"
    override val titleRes = R.string.item_detail_title
    const val itemIdArg = "itemId"
    val routeWithArgs = "$route/{$itemIdArg}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(
    navigateToEditItem: (Int) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState = viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsManager = AppSettingsManager(context)

    var showSharingDisabledDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            InventoryTopAppBar(
                title = stringResource(ItemDetailsDestination.titleRes),
                canNavigateBack = true,
                navigateUp = navigateBack
            )
        },
        floatingActionButton = {
            Row {
                // Edit button
                FloatingActionButton(
                    onClick = { navigateToEditItem(uiState.value.itemDetails.id) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_item_title),
                    )
                }
                // Share button - всегда активна, но проверяет настройки при клике
                FloatingActionButton(
                    onClick = {
                        if (settingsManager.disableSharing) {
                            // Показываем диалог, что шаринг запрещен
                            showSharingDisabledDialog = true
                        } else {
                            // Шаринг разрешен - отправляем данные
                            val item = uiState.value.itemDetails.toItem()
                            shareItem(context, item, settingsManager)
                        }
                    },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Поделиться",
                    )
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        ItemDetailsBody(
            itemDetailsUiState = uiState.value,
            onSellItem = { viewModel.reduceQuantityByOne() },
            onDelete = {
                coroutineScope.launch {
                    viewModel.deleteItem()
                    navigateBack()
                }
            },
            hideSensitiveData = settingsManager.hideSensitiveData,
            disableSharing = settingsManager.disableSharing,
            modifier = Modifier
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                )
                .verticalScroll(rememberScrollState())
        )

        // Диалог о запрете шаринга
        if (showSharingDisabledDialog) {
            SharingDisabledDialog(
                onConfirm = { showSharingDisabledDialog = false },
                modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_medium))
            )
        }
    }
}

@Composable
private fun ItemDetailsBody(
    itemDetailsUiState: ItemDetailsUiState,
    onSellItem: () -> Unit,
    onDelete: () -> Unit,
    hideSensitiveData: Boolean = false,
    disableSharing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(dimensionResource(id = R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
    ) {
        var deleteConfirmationRequired by rememberSaveable { mutableStateOf(false) }

        // Показываем предупреждение, если шаринг запрещен
        if (disableSharing) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Отправка данных запрещена в настройках приложения",
                    modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_medium)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        ItemDetails(
            item = itemDetailsUiState.itemDetails.toItem(),
            hideSensitiveData = hideSensitiveData,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onSellItem,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            enabled = !itemDetailsUiState.outOfStock
        ) {
            Text(stringResource(R.string.sell))
        }

        OutlinedButton(
            onClick = { deleteConfirmationRequired = true },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.delete))
        }

        if (deleteConfirmationRequired) {
            DeleteConfirmationDialog(
                onDeleteConfirm = {
                    deleteConfirmationRequired = false
                    onDelete()
                },
                onDeleteCancel = { deleteConfirmationRequired = false },
                modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_medium))
            )
        }
    }
}

@Composable
fun ItemDetails(
    item: Item,
    hideSensitiveData: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier, colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
        ) {
            ItemDetailsRow(
                labelResID = R.string.item,
                itemDetail = item.name,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(
                        id = R.dimen.padding_medium
                    )
                )
            )
            ItemDetailsRow(
                labelResID = R.string.quantity_in_stock,
                itemDetail = item.quantity.toString(),
                modifier = Modifier.padding(
                    horizontal = dimensionResource(
                        id = R.dimen.padding_medium
                    )
                )
            )
            ItemDetailsRow(
                labelResID = R.string.price,
                itemDetail = item.formatedPrice(),
                modifier = Modifier.padding(
                    horizontal = dimensionResource(
                        id = R.dimen.padding_medium
                    )
                )
            )

            // Источник данных
            ItemDetailsRow(
                labelResID = R.string.data_source,
                itemDetail = when (item.dataSource) {
                    com.example.inventory.data.DataSource.MANUAL -> "Ручное заполнение"
                    com.example.inventory.data.DataSource.FILE -> "Загрузка из файла"
                },
                modifier = Modifier.padding(
                    horizontal = dimensionResource(
                        id = R.dimen.padding_medium
                    )
                )
            )

            // New supplier fields - скрываются если включена настройка
            if (item.supplierName.isNotBlank()) {
                ItemDetailsRow(
                    labelResID = R.string.supplier_name,
                    itemDetail = if (hideSensitiveData) "***" else item.supplierName,
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(
                            id = R.dimen.padding_medium
                        )
                    )
                )
            }
            if (item.supplierEmail.isNotBlank()) {
                ItemDetailsRow(
                    labelResID = R.string.supplier_email,
                    itemDetail = if (hideSensitiveData) "***" else item.supplierEmail,
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(
                            id = R.dimen.padding_medium
                        )
                    )
                )
            }
            if (item.supplierPhone.isNotBlank()) {
                ItemDetailsRow(
                    labelResID = R.string.supplier_phone,
                    itemDetail = if (hideSensitiveData) "***" else item.supplierPhone,
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(
                            id = R.dimen.padding_medium
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun ItemDetailsRow(
    @StringRes labelResID: Int, itemDetail: String, modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Text(text = stringResource(labelResID))
        Spacer(modifier = Modifier.weight(1f))
        Text(text = itemDetail, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onDeleteConfirm: () -> Unit, onDeleteCancel: () -> Unit, modifier: Modifier = Modifier
) {
    AlertDialog(onDismissRequest = { },
        title = { Text(stringResource(R.string.attention)) },
        text = { Text(stringResource(R.string.delete_question)) },
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = onDeleteCancel) {
                Text(text = stringResource(R.string.no))
            }
        },
        confirmButton = {
            TextButton(onClick = onDeleteConfirm) {
                Text(text = stringResource(R.string.yes))
            }
        })
}

@Composable
private fun SharingDisabledDialog(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = { Text("Отправка запрещена") },
        text = {
            Text("Отправка данных из приложения запрещена в настройках. " +
                    "Чтобы включить отправку, перейдите в настройки приложения.")
        },
        modifier = modifier,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        }
    )
}

// Share function with settings check
private fun shareItem(context: Context, item: Item, settingsManager: AppSettingsManager) {
    val shareText = buildString {
        appendLine("Информация о товаре:")
        appendLine("Название: ${item.name}")
        appendLine("Цена: ${item.formatedPrice()}")
        appendLine("Количество: ${item.quantity}")
        appendLine("Источник: ${
            when (item.dataSource) {
                com.example.inventory.data.DataSource.MANUAL -> "Ручное заполнение"
                com.example.inventory.data.DataSource.FILE -> "Загрузка из файла"
            }
        }")

        if (!settingsManager.hideSensitiveData) {
            if (item.supplierName.isNotBlank()) {
                appendLine("Поставщик: ${item.supplierName}")
            }
            if (item.supplierEmail.isNotBlank()) {
                appendLine("Email поставщика: ${item.supplierEmail}")
            }
            if (item.supplierPhone.isNotBlank()) {
                appendLine("Телефон поставщика: ${item.supplierPhone}")
            }
        } else {
            appendLine("Данные поставщика: скрыты")
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Информация о товаре: ${item.name}")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    context.startActivity(Intent.createChooser(intent, "Поделиться информацией о товаре"))
}

@Preview(showBackground = true)
@Composable
fun ItemDetailsScreenPreview() {
    InventoryTheme {
        ItemDetailsBody(ItemDetailsUiState(
            outOfStock = true,
            itemDetails = ItemDetails(
                id = 1,
                name = "Pen",
                price = "100",
                quantity = "10",
                supplierName = "Test Supplier",
                supplierEmail = "test@example.com",
                supplierPhone = "+79991234567",
                dataSource = com.example.inventory.data.DataSource.MANUAL
            )
        ), onSellItem = {}, onDelete = {})
    }
}