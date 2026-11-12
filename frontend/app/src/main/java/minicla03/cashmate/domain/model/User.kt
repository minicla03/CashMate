package minicla03.cashmate.domain.model

/**
 * Authenticated CashMate user profile as returned by the backend.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val emailVerified: Boolean = false,
    val createdAt: String? = null
)

