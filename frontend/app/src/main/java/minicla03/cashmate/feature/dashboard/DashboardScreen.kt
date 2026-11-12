package minicla03.cashmate.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.designsystem.EmptyStateView
import minicla03.cashmate.core.designsystem.ErrorStateView
import minicla03.cashmate.core.designsystem.LoadingStateView
import minicla03.cashmate.core.ui.CategoryDonutChart
import minicla03.cashmate.core.ui.MoneyFormatter
import minicla03.cashmate.core.ui.TransactionRow
import minicla03.cashmate.domain.model.DashboardSummary
import minicla03.cashmate.domain.model.Money
import java.util.Currency

@Composable
fun DashboardRoute(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToScanReceipt: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var skipInitialResume by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (skipInitialResume) {
            skipInitialResume = false
        } else {
            viewModel.onEvent(DashboardEvent.Refresh)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DashboardEffect.NavigateToAddTransaction -> onNavigateToAddTransaction()
                DashboardEffect.NavigateToScanReceipt -> onNavigateToScanReceipt()
                DashboardEffect.NavigateToTransactions -> onNavigateToTransactions()
                is DashboardEffect.NavigateToTransactionDetail ->
                    onNavigateToTransactionDetail(effect.id)
            }
        }
    }

    DashboardScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> LoadingStateView(modifier = modifier)
        state.error != null && state.summary == null -> ErrorStateView(
            message = stringResource(state.error.messageResId),
            onRetry = if (state.error.canRetry) {
                { onEvent(DashboardEvent.Refresh) }
            } else {
                null
            },
            modifier = modifier
        )

        state.isEmpty -> EmptyStateView(
            title = stringResource(R.string.dashboard_empty_title),
            subtitle = stringResource(R.string.dashboard_empty_subtitle),
            actionLabel = stringResource(R.string.dashboard_add_first),
            onAction = { onEvent(DashboardEvent.AddTransactionClick) },
            modifier = modifier
        )

        state.summary != null -> DashboardContent(
            summary = state.summary,
            currency = state.currency,
            inlineErrorResId = state.error?.messageResId,
            onEvent = onEvent,
            modifier = modifier
        )
    }
}

@Composable
private fun DashboardContent(
    summary: DashboardSummary,
    currency: Currency,
    inlineErrorResId: Int?,
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    val semantic = CashMateTheme.semanticColors

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.md,
            end = spacing.md,
            top = spacing.md,
            bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        if (inlineErrorResId != null) {
            item(key = "inline_error") {
                CashMateCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(inlineErrorResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onEvent(DashboardEvent.Refresh) }) {
                            Text(text = stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }

        item(key = "balance_hero") {
            BalanceHeroCard(
                currentBalance = summary.currentBalance,
                currency = currency,
                onRefresh = { onEvent(DashboardEvent.Refresh) }
            )
        }

        item(key = "quick_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                OutlinedButton(
                    onClick = { onEvent(DashboardEvent.AddTransactionClick) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = spacing.minTouchTarget),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.dashboard_add_first),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FilledTonalButton(
                    onClick = { onEvent(DashboardEvent.ScanReceiptClick) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = spacing.minTouchTarget),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.receipt_scan_action),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item(key = "monthly_metrics") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = stringResource(R.string.dashboard_this_month),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    MetricSummaryCard(
                        label = stringResource(R.string.dashboard_monthly_income),
                        amount = summary.monthlyIncome,
                        currency = currency,
                        icon = Icons.Rounded.ArrowDownward,
                        accentColor = semantic.income,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryCard(
                        label = stringResource(R.string.dashboard_monthly_expenses),
                        amount = summary.monthlyExpenses,
                        currency = currency,
                        icon = Icons.Rounded.ArrowUpward,
                        accentColor = semantic.expense,
                        modifier = Modifier.weight(1f)
                    )
                }
                MetricSummaryCard(
                    label = stringResource(R.string.dashboard_monthly_savings),
                    amount = summary.monthlySavings,
                    currency = currency,
                    icon = Icons.Rounded.Savings,
                    accentColor = if (summary.monthlySavings.isNegative) {
                        semantic.expense
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    showSign = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(key = "expense_distribution") {
            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dashboard_expense_distribution),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(bottom = spacing.sm)
                        .semantics { heading() }
                )
                if (summary.expenseDistribution.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dashboard_no_monthly_expenses),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    CategoryDonutChart(
                        items = summary.expenseDistribution,
                        totalAmount = summary.monthlyExpenses,
                        currency = currency
                    )
                }
            }
        }

        item(key = "recent_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.dashboard_recent_transactions),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )
                TextButton(onClick = { onEvent(DashboardEvent.ViewAllTransactionsClick) }) {
                    Text(text = stringResource(R.string.action_view_all))
                }
            }
        }

        items(
            items = summary.recentTransactions,
            key = { it.id }
        ) { transaction ->
            TransactionRow(
                transaction = transaction,
                category = summary.categoriesById[transaction.categoryId],
                currency = currency,
                onClick = { onEvent(DashboardEvent.TransactionClick(transaction.id)) }
            )
        }
    }
}

@Composable
private fun BalanceHeroCard(
    currentBalance: Money,
    currency: Currency,
    onRefresh: () -> Unit
) {
    val formattedBalance = MoneyFormatter.format(currentBalance, currency)
    val balanceLabel = stringResource(R.string.dashboard_current_balance)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$balanceLabel: $formattedBalance"
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(CashMateTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = balanceLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.action_refresh)
                    )
                }
            }
            Text(
                text = formattedBalance,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetricSummaryCard(
    label: String,
    amount: Money,
    currency: Currency,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    showSign: Boolean = false
) {
    val formatted = if (showSign) {
        MoneyFormatter.formatSignedDelta(amount, currency)
    } else {
        MoneyFormatter.format(amount, currency)
    }

    CashMateCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $formatted"
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatted,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(top = CashMateTheme.spacing.xs)
        )
    }
}

