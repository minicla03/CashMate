package minicla03.cashmate.feature.statistics

import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.StatisticsSummary
import java.util.Currency

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val summary: StatisticsSummary? = null,
    val currency: Currency = Currency.getInstance("EUR"),
    val error: UiError? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && error == null && summary != null &&
            summary.totalExpenses.isZero && summary.totalIncome.isZero
}

sealed interface StatisticsEvent {
    data object Refresh : StatisticsEvent
}

