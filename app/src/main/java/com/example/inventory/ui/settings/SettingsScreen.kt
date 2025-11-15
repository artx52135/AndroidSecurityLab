package com.example.inventory.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.R
import com.example.inventory.ui.AppViewModelProvider
import com.example.inventory.ui.navigation.NavigationDestination
import com.example.inventory.ui.theme.InventoryTheme

object SettingsDestination : NavigationDestination {
    override val route = "settings"
    override val titleRes = R.string.settings_title
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(SettingsDestination.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        SettingsBody(
            uiState = uiState,
            onHideSensitiveDataChange = { viewModel.updateHideSensitiveData(it) },
            onDisableSharingChange = { viewModel.updateDisableSharing(it) },
            onUseDefaultQuantityChange = { viewModel.updateUseDefaultQuantity(it) },
            onDefaultQuantityChange = { viewModel.updateDefaultQuantity(it) },
            modifier = modifier.padding(innerPadding)
        )
    }
}

@Composable
fun SettingsBody(
    uiState: SettingsUiState,
    onHideSensitiveDataChange: (Boolean) -> Unit,
    onDisableSharingChange: (Boolean) -> Unit,
    onUseDefaultQuantityChange: (Boolean) -> Unit,
    onDefaultQuantityChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.padding_medium))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_large)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
            ) {
                Text(
                    text = stringResource(R.string.security_settings),
                    style = MaterialTheme.typography.titleLarge
                )

                SettingSwitch(
                    checked = uiState.hideSensitiveData,
                    onCheckedChange = onHideSensitiveDataChange,
                    title = stringResource(R.string.hide_sensitive_data),
                    description = stringResource(R.string.hide_sensitive_data_desc)
                )

                SettingSwitch(
                    checked = uiState.disableSharing,
                    onCheckedChange = onDisableSharingChange,
                    title = stringResource(R.string.disable_sharing),
                    description = stringResource(R.string.disable_sharing_desc)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_large)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
            ) {
                Text(
                    text = stringResource(R.string.item_creation_settings),
                    style = MaterialTheme.typography.titleLarge
                )

                SettingSwitch(
                    checked = uiState.useDefaultQuantity,
                    onCheckedChange = onUseDefaultQuantityChange,
                    title = stringResource(R.string.use_default_quantity),
                    description = stringResource(R.string.use_default_quantity_desc)
                )

                if (uiState.useDefaultQuantity) {
                    OutlinedTextField(
                        value = uiState.defaultQuantity,
                        onValueChange = onDefaultQuantityChange,
                        label = { Text(stringResource(R.string.default_quantity)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SettingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_small)))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    InventoryTheme {
        SettingsScreen(navigateBack = {})
    }
}