package minicla03.cashmate.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose route definitions centralized for the application.
 */
sealed interface CashMateRoute {

    @Serializable
    data object Dashboard : CashMateRoute

    @Serializable
    data object Transactions : CashMateRoute

    @Serializable
    data class TransactionDetail(val transactionId: String) : CashMateRoute

    @Serializable
    data class TransactionForm(val transactionId: String? = null) : CashMateRoute

    @Serializable
    data object Categories : CashMateRoute

    @Serializable
    data object Statistics : CashMateRoute

    @Serializable
    data object Settings : CashMateRoute

    @Serializable
    data object ReceiptFlow : CashMateRoute

    @Serializable
    data object ReceiptScan : CashMateRoute

    @Serializable
    data class ReceiptReview(val receiptId: String) : CashMateRoute
}

