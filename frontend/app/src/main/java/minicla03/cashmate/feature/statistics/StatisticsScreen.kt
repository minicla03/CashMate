package minicla03.cashmate.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import minicla03.cashmate.core.ui.CategoryIconBadge
import minicla03.cashmate.core.ui.CategoryVisuals
import minicla03.cashmate.core.ui.DateFormatter
import minicla03.cashmate.core.ui.MoneyFormatter
import minicla03.cashmate.core.ui.MonthlyTrendBarChart
import minicla03.cashmate.domain.model.MonthComparison
import minicla03.cashmate.domain.model.StatisticsSummary
import java.util.Currency

@Composable
fun StatisticsRoute(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var skipInitialResume by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (skipInitialResume) {
            skipInitialResume = false
        } else {
            viewModel.onEvent(StatisticsEvent.Refresh)
        }
    }

    StatisticsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onEvent: (StatisticsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> LoadingStateView(modifier = modifier)
        state.error != null && state.summary == null -> ErrorStateView(
            message = stringResource(state.error.messageResId),
            onRetry = if (state.error.canRetry) {
                { onEvent(StatisticsEvent.Refresh) }
            } else {
                null
            },
            modifier = modifier
        )

        state.isEmpty -> EmptyStateView(
            title = stringResource(R.string.statistics_empty_title),
            subtitle = stringResource(R.string.statistics_empty_subtitle),
            modifier = modifier
        )

        state.summary != null -> StatisticsContent(
            summary = state.summary,
            currency = state.currency,
            modifier = modifier
        )
    }
}

@Composable
private fun StatisticsContent(
    summary: StatisticsSummary,
    currency: Currency,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        item(key = "month_comparison") {
            MonthComparisonCard(
                comparison = summary.monthComparison,
                currency = currency
            )
        }

        item(key = "monthly_trend") {
            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.statistics_monthly_trend),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(bottom = spacing.sm)
                        .semantics { heading() }
                )
                MonthlyTrendBarChart(
                    points = summary.monthlyTrend,
                    currency = currency
                )
            }
        }

        item(key = "expenses_by_category") {
            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.statistics_expenses_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(bottom = spacing.sm)
                        .semantics { heading() }
                )
                if (summary.expensesByCategory.isEmpty()) {
                    Text(
                        text = stringResource(R.string.statistics_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    CategoryDonutChart(
                        items = summary.expensesByCategory,
                        totalAmount = summary.totalExpenses,
                        currency = currency
                    )
                }
            }
        }

        if (summary.expensesByCategory.isNotEmpty()) {
            item(key = "category_breakdown_bars") {
                CashMateCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        summary.expensesByCategory.forEach { stat ->
                            val tint = CategoryVisuals.parseColor(stat.category.color)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                                ) {
                                    CategoryIconBadge(
                                        iconKey = stat.category.icon,
                                        colorHex = stat.category.color,
                                        size = 32.dp
                                    )
                                    Text(
                                        text = stat.category.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = MoneyFormatter.format(stat.amount, currency),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (stat.shareBasisPoints / 10_000f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = tint,
                                    trackColor = tint.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthComparisonCard(
    comparison: MonthComparison,
    currency: Currency
) {
    val spacing = CashMateTheme.spacing
    val semantic = CashMateTheme.semanticColors

    val delta = comparison.expenseDelta
    val trendIcon = when {
        delta.isPositive -> Icons.AutoMirrored.Rounded.TrendingUp
        delta.isNegative -> Icons.AutoMirrored.Rounded.TrendingDown
        else -> Icons.AutoMirrored.Rounded.TrendingFlat
    }
    val trendColor = when {
        delta.isPositive -> semantic.expense
        delta.isNegative -> semantic.income
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val deltaAbsFormatted = MoneyFormatter.format(delta.abs(), currency)
    val percentSuffix = comparison.changeBasisPoints?.let { bps ->
        " (${MoneyFormatter.formatBasisPointsPercent(kotlin.math.abs(bps))})"
    }.orEmpty()

    val comparisonSummary = when {
        comparison.previousExpenses.isZero ->
            stringResource(R.string.statistics_no_previous_baseline)
        delta.isPositive ->
            stringResource(R.string.statistics_vs_previous_higher, "$deltaAbsFormatted$percentSuffix")
        delta.isNegative ->
            stringResource(R.string.statistics_vs_previous_lower, "$deltaAbsFormatted$percentSuffix")
        else ->
            stringResource(R.string.statistics_vs_previous_same)
    }

    CashMateCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.statistics_month_comparison),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateFormatter.formatMonthYear(comparison.currentMonth),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyFormatter.format(comparison.currentExpenses, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateFormatter.formatMonthYear(comparison.previousMonth),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyFormatter.format(comparison.previousExpenses, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Icon(
                    imageVector = trendIcon,
                    contentDescription = null,
                    tint = trendColor
                )
                Text(
                    text = comparisonSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = trendColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
