package minicla03.cashmate.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.domain.model.AppThemeMode

@Composable
fun SettingsRoute(
    onNavigateToCategories: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToCategories -> onNavigateToCategories()
            }
        }
    }

    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        item(key = "appearance") {
            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        text = stringResource(R.string.settings_appearance_section),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = stringResource(R.string.settings_theme_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        ThemeChip(
                            label = stringResource(R.string.settings_theme_system),
                            selected = state.themeMode == AppThemeMode.SYSTEM,
                            onClick = { onEvent(SettingsEvent.ThemeSelected(AppThemeMode.SYSTEM)) }
                        )
                        ThemeChip(
                            label = stringResource(R.string.settings_theme_light),
                            selected = state.themeMode == AppThemeMode.LIGHT,
                            onClick = { onEvent(SettingsEvent.ThemeSelected(AppThemeMode.LIGHT)) }
                        )
                        ThemeChip(
                            label = stringResource(R.string.settings_theme_dark),
                            selected = state.themeMode == AppThemeMode.DARK,
                            onClick = { onEvent(SettingsEvent.ThemeSelected(AppThemeMode.DARK)) }
                        )
                    }
                }
            }
        }

        item(key = "currency") {
            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        text = stringResource(R.string.settings_preferences_section),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = stringResource(R.string.settings_currency_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        state.availableCurrencies.forEach { currency ->
                            FilterChip(
                                selected = state.selectedCurrency == currency,
                                onClick = { onEvent(SettingsEvent.CurrencySelected(currency)) },
                                label = { Text("${currency.currencyCode} (${currency.symbol})") }
                            )
                        }
                    }
                }
            }
        }

        item(key = "categories_management") {
            CashMateCard(
                onClick = { onEvent(SettingsEvent.ManageCategoriesClick) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_manage_categories),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.settings_manage_categories_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "about") {
            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.settings_about_section),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                    InfoRow(
                        label = stringResource(R.string.app_name),
                        value = "${stringResource(R.string.settings_version_label)} ${state.appVersion}"
                    )
                    HorizontalDivider()
                    InfoRow(
                        label = stringResource(R.string.settings_backend_mode_label),
                        value = stringResource(R.string.settings_backend_mode_mock)
                    )
                    Text(
                        text = stringResource(R.string.settings_architecture_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

