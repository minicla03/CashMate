package minicla03.cashmate.feature.profile.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMateOutlinedButton
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.designsystem.ErrorStateView
import minicla03.cashmate.core.designsystem.LoadingStateView
import minicla03.cashmate.domain.model.ConsentItem
import minicla03.cashmate.domain.model.PrivacyDataCategory

@Composable
fun PrivacyRoute(
    onNavigateBack: () -> Unit,
    onNavigateToExportData: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    viewModel: PrivacyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.actionError, state.snackbarMessageResId) {
        val msgRes = state.actionError?.messageResId ?: state.snackbarMessageResId ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(msgRes))
        viewModel.onEvent(PrivacyEvent.DismissSnackbar)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PrivacyEffect.NavigateToExportData -> onNavigateToExportData()
                PrivacyEffect.NavigateToDeleteAccount -> onNavigateToDeleteAccount()
            }
        }
    }

    PrivacyScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    state: PrivacyUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onEvent: (PrivacyEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
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
            state.error != null && state.privacyInfo == null -> ErrorStateView(
                message = stringResource(state.error.messageResId),
                onRetry = if (state.error.canRetry) {
                    { onEvent(PrivacyEvent.Refresh) }
                } else {
                    null
                },
                modifier = Modifier.padding(padding)
            )
            state.privacyInfo != null -> {
                val info = state.privacyInfo
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    item(key = "your_data_header") {
                        CashMateCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                Text(
                                    text = stringResource(R.string.privacy_your_data_section),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.semantics { heading() }
                                )
                                Text(
                                    text = info.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                info.policyUpdatedAt?.let { updated ->
                                    Text(
                                        text = stringResource(R.string.privacy_policy_updated, updated),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(
                        items = info.dataCategories,
                        key = { "category_${it.id}" }
                    ) { category ->
                        PrivacyDataCategoryCard(category = category)
                    }

                    if (info.consents.isNotEmpty()) {
                        item(key = "consents_section") {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                Text(
                                    text = stringResource(R.string.privacy_consents_section),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.semantics { heading() }
                                )
                                CashMateCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                        info.consents.forEachIndexed { index, consent ->
                                            if (index > 0) {
                                                HorizontalDivider()
                                            }
                                            ConsentRow(
                                                consent = consent,
                                                isUpdating = state.updatingConsentId == consent.id,
                                                onToggle = { granted ->
                                                    onEvent(PrivacyEvent.ConsentToggled(consent.id, granted))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "privacy_actions") {
                        CashMateCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                Text(
                                    text = stringResource(R.string.privacy_actions_section),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.semantics { heading() }
                                )
                                CashMateOutlinedButton(
                                    text = stringResource(R.string.profile_export_data),
                                    onClick = { onEvent(PrivacyEvent.ExportDataClick) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedButton(
                                    onClick = { onEvent(PrivacyEvent.DeleteAccountClick) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(text = stringResource(R.string.profile_delete_account))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyDataCategoryCard(
    category: PrivacyDataCategory
) {
    CashMateCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xxs)) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            category.retentionSummary?.let { retention ->
                Text(
                    text = retention,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ConsentRow(
    consent: ConsentItem,
    isUpdating: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val spacing = CashMateTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = consent.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Surface(
                    color = if (consent.required) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (consent.required) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(
                            if (consent.required) {
                                R.string.privacy_consent_required
                            } else {
                                R.string.privacy_consent_optional
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = consent.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = consent.granted,
            onCheckedChange = { checked ->
                if (!consent.required) {
                    onToggle(checked)
                }
            },
            enabled = !consent.required && !isUpdating
        )
    }
}

