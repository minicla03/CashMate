package minicla03.cashmate.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose route definitions centralized for the application.
 */
sealed interface CashMateRoute {

    @Serializable
    data object Login : CashMateRoute

    @Serializable
    data object Register : CashMateRoute

    @Serializable
    data class EmailVerification(val email: String = "") : CashMateRoute

    @Serializable
    data object ForgotPassword : CashMateRoute

    @Serializable
    data class ResetPassword(val email: String = "") : CashMateRoute

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
    data object Profile : CashMateRoute

    @Serializable
    data object EditProfile : CashMateRoute

    @Serializable
    data object ChangePassword : CashMateRoute

    @Serializable
    data object Preferences : CashMateRoute

    @Serializable
    data object Privacy : CashMateRoute

    @Serializable
    data object ExportData : CashMateRoute

    @Serializable
    data object DeleteAccount : CashMateRoute

    @Serializable
    data object ReceiptFlow : CashMateRoute

    @Serializable
    data object ReceiptScan : CashMateRoute

    @Serializable
    data class ReceiptReview(val receiptId: String) : CashMateRoute
}

