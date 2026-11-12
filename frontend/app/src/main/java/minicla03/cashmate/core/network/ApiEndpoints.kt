package minicla03.cashmate.core.network

/**
 * Centralized versioned backend API endpoint paths.
 */
object ApiEndpoints {
    private const val API_PREFIX = "api/v1"

    const val AUTH_REGISTER = "$API_PREFIX/auth/register"
    const val AUTH_LOGIN = "$API_PREFIX/auth/login"
    const val AUTH_LOGOUT = "$API_PREFIX/auth/logout"
    const val AUTH_REFRESH = "$API_PREFIX/auth/refresh"
    const val AUTH_VERIFY_EMAIL = "$API_PREFIX/auth/verify-email"
    const val AUTH_RESEND_VERIFICATION = "$API_PREFIX/auth/verify-email/resend"
    const val AUTH_PASSWORD_RESET_REQUEST = "$API_PREFIX/auth/password-reset/request"
    const val AUTH_PASSWORD_RESET_CONFIRM = "$API_PREFIX/auth/password-reset/confirm"

    const val ME = "$API_PREFIX/me"
    const val ME_PASSWORD = "$API_PREFIX/me/password"
    const val ME_PREFERENCES = "$API_PREFIX/me/preferences"
    const val ME_PRIVACY = "$API_PREFIX/me/privacy"
    const val ME_CONSENTS = "$API_PREFIX/me/consents"
    const val ME_CONSENT_BY_ID = "$API_PREFIX/me/consents/{id}"
    const val ME_EXPORT = "$API_PREFIX/me/export"

    const val TRANSACTIONS = "$API_PREFIX/transactions"
    const val TRANSACTION_BY_ID = "$API_PREFIX/transactions/{id}"

    const val CATEGORIES = "$API_PREFIX/categories"
    const val CATEGORY_BY_ID = "$API_PREFIX/categories/{id}"

    const val ACCOUNTS = "$API_PREFIX/accounts"
    const val DASHBOARD = "$API_PREFIX/dashboard"
    const val STATISTICS = "$API_PREFIX/statistics"

    const val RECEIPT_PROCESS = "$API_PREFIX/receipts/process"
    const val RECEIPT_CONFIRM = "$API_PREFIX/receipts/{id}/confirm"
}

