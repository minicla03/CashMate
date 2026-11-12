package minicla03.cashmate.core.network

/**
 * Abstraction for secure persistence of authentication tokens.
 * Callers must not depend on the underlying storage mechanism.
 */
interface TokenStorage {
    fun saveAccessToken(token: String)
    fun getAccessToken(): String?
    fun saveRefreshToken(token: String)
    fun getRefreshToken(): String?
    fun saveTokens(accessToken: String, refreshToken: String) {
        saveAccessToken(accessToken)
        saveRefreshToken(refreshToken)
    }
    fun clearTokens()
}

/**
 * In-memory implementation suitable for unit tests.
 */
class InMemoryTokenStorage : TokenStorage {
    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var refreshToken: String? = null

    override fun saveAccessToken(token: String) {
        accessToken = token
    }

    override fun getAccessToken(): String? = accessToken

    override fun saveRefreshToken(token: String) {
        refreshToken = token
    }

    override fun getRefreshToken(): String? = refreshToken

    override fun clearTokens() {
        accessToken = null
        refreshToken = null
    }
}

