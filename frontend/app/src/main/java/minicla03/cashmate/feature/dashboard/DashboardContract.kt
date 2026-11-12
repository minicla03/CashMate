package minicla03.cashmate.feature.dashboard

import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.DashboardSummary
import java.util.Currency

data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: DashboardSummary? = null,
    val currency: Currency = Currency.getInstance("EUR"),
    val error: UiError? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && error == null && summary != null &&
            summary.recentTransactions.isEmpty() && summary.currentBalance.isZero
}

sealed interface DashboardEvent {
    data object Refresh : DashboardEvent
    data object AddTransactionClick : DashboardEvent
    data object ViewAllTransactionsClick : DashboardEvent
    data class TransactionClick(val id: String) : DashboardEvent
}

sealed interface DashboardEffect {
    data object NavigateToAddTransaction : DashboardEffect
    data object NavigateToTransactions : DashboardEffect
    data class NavigateToTransactionDetail(val id: String) : DashboardEffect
}

