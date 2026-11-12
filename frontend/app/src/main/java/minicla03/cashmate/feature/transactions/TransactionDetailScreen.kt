package minicla03.cashmate.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMatePrimaryButton
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.designsystem.ErrorStateView
import minicla03.cashmate.core.designsystem.LoadingStateView
import minicla03.cashmate.core.ui.CategoryIconBadge
import minicla03.cashmate.core.ui.DateFormatter
import minicla03.cashmate.core.ui.MoneyFormatter
import minicla03.cashmate.domain.model.TransactionType

@Composable
fun TransactionDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var skipInitialResume by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (skipInitialResume) {
            skipInitialResume = false
        } else {
            viewModel.onEvent(TransactionDetailEvent.Refresh)
        }
    }

    LaunchedEffect(state.actionError) {
        val actionError = state.actionError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(actionError.messageResId))
        viewModel.onEvent(TransactionDetailEvent.DismissActionError)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TransactionDetailEffect.NavigateToEdit -> onNavigateToEdit(effect.transactionId)
                TransactionDetailEffect.NavigateBackAfterDelete -> onDeleted()
            }
        }
    }

    TransactionDetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    state: TransactionDetailUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onEvent: (TransactionDetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transaction_detail_title)) },
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
            state.error != null -> ErrorStateView(
                message = stringResource(state.error.messageResId),
                onRetry = if (state.error.canRetry) {
                    { onEvent(TransactionDetailEvent.Refresh) }
                } else {
                    null
                },
                modifier = Modifier.padding(padding)
            )

            state.transaction != null -> {
                val tx = state.transaction
                val spacing = CashMateTheme.spacing
                val semantic = CashMateTheme.semanticColors
                val isIncome = tx.type == TransactionType.INCOME
                val accentColor = if (isIncome) semantic.income else semantic.expense
                val containerColor = if (isIncome) semantic.incomeContainer else semantic.expenseContainer
                val onContainerColor = if (isIncome) semantic.onIncomeContainer else semantic.onExpenseContainer
                val typeIcon = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
                val typeText = if (isIncome) {
                    stringResource(R.string.transaction_type_income)
                } else {
                    stringResource(R.string.transaction_type_expense)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    CashMateCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            CategoryIconBadge(
                                iconKey = state.category?.icon ?: "category",
                                colorHex = state.category?.color ?: "#546E7A",
                                size = 56.dp
                            )
                            Surface(
                                color = containerColor,
                                contentColor = onContainerColor,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = typeIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = typeText,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                            Text(
                                text = MoneyFormatter.formatSigned(tx.amount, tx.type, state.currency),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Text(
                                text = tx.description,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    CashMateCard(modifier = Modifier.fillMaxWidth()) {
                        DetailInfoRow(
                            label = stringResource(R.string.transaction_detail_category),
                            value = state.category?.name ?: tx.categoryId
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = spacing.sm))
                        DetailInfoRow(
                            label = stringResource(R.string.transaction_detail_date),
                            value = DateFormatter.formatLong(tx.date)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = spacing.sm))
                        DetailInfoRow(
                            label = stringResource(R.string.transaction_detail_account),
                            value = state.account?.name ?: tx.accountId
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = spacing.sm))
                        DetailInfoRow(
                            label = stringResource(R.string.transaction_detail_type),
                            value = typeText
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        OutlinedButton(
                            onClick = { onEvent(TransactionDetailEvent.DeleteClick) },
                            enabled = !state.isDeleting,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(CashMateTheme.spacing.minTouchTarget),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(text = stringResource(R.string.action_delete))
                        }

                        CashMatePrimaryButton(
                            text = stringResource(R.string.action_edit),
                            onClick = { onEvent(TransactionDetailEvent.EditClick) },
                            enabled = !state.isDeleting,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(TransactionDetailEvent.DismissDeleteDialog) },
            title = { Text(stringResource(R.string.transaction_delete_dialog_title)) },
            text = { Text(stringResource(R.string.transaction_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(TransactionDetailEvent.ConfirmDelete) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(TransactionDetailEvent.DismissDeleteDialog) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DetailInfoRow(
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
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

