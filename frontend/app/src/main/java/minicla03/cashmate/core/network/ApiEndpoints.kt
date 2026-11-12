package minicla03.cashmate.core.network

/**
 * Centralized versioned backend API endpoint paths.
 */
object ApiEndpoints {
    private const val API_PREFIX = "api/v1"

    const val TRANSACTIONS = "$API_PREFIX/transactions"
    const val TRANSACTION_BY_ID = "$API_PREFIX/transactions/{id}"

    const val CATEGORIES = "$API_PREFIX/categories"
    const val CATEGORY_BY_ID = "$API_PREFIX/categories/{id}"

    const val ACCOUNTS = "$API_PREFIX/accounts"
    const val DASHBOARD = "$API_PREFIX/dashboard"
    const val STATISTICS = "$API_PREFIX/statistics"
}

