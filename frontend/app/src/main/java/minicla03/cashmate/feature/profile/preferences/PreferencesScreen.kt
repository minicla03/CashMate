package minicla03.cashmate.feature.profile.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.designsystem.ErrorStateView
import minicla03.cashmate.core.designsystem.LoadingStateView
import minicla03.cashmate.domain.model.AppThemeMode

@Composable
fun PreferencesRoute(
    onNavigateBack: () -> Unit,
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.actionError) {
        val err = state.actionError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(err.messageResId))
        viewModel.onEvent(PreferencesEvent.DismissActionError)
    }

    PreferencesScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreferencesScreen(
    state: PreferencesUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onEvent: (PreferencesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preferences_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingStateView(modifier = Modifier.padding(padding))
            state.loadError != null -> ErrorStateView(
                message = stringResource(state.loadError.messageResId),
                onRetry = if (state.loadError.canRetry) {
                    { onEvent(PreferencesEvent.Refresh) }
                } else {
                    null
                },
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                                FilterChip(
                                    selected = state.themeMode == AppThemeMode.SYSTEM,
                                    enabled = !state.isSaving,
                                    onClick = { onEvent(PreferencesEvent.ThemeSelected(AppThemeMode.SYSTEM)) },
                                    label = { Text(stringResource(R.string.settings_theme_system)) }
                                )
                                FilterChip(
                                    selected = state.themeMode == AppThemeMode.LIGHT,
                                    enabled = !state.isSaving,
                                    onClick = { onEvent(PreferencesEvent.ThemeSelected(AppThemeMode.LIGHT)) },
                                    label = { Text(stringResource(R.string.settings_theme_light)) }
                                )
                                FilterChip(
                                    selected = state.themeMode == AppThemeMode.DARK,
                                    enabled = !state.isSaving,
                                    onClick = { onEvent(PreferencesEvent.ThemeSelected(AppThemeMode.DARK)) },
                                    label = { Text(stringResource(R.string.settings_theme_dark)) }
                                )
                            }
                        }
                    }
                }

                item(key = "finance") {
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
                                        enabled = !state.isSaving,
                                        onClick = { onEvent(PreferencesEvent.CurrencySelected(currency)) },
                                        label = { Text("${currency.currencyCode} (${currency.symbol})") }
                                    )
                                }
                            }
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${stringResource(R.string.settings_version_label)} ${state.appVersion}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            HorizontalDivider()
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
    }
}

