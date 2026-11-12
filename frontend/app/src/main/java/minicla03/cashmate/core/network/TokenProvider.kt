package minicla03.cashmate.core.network

import javax.inject.Inject

/**
 * Abstraction supplying an access token for authenticated backend requests.
 */
interface TokenProvider {
    fun getAccessToken(): String?
}

class StorageTokenProvider @Inject constructor(
    private val tokenStorage: TokenStorage
) : TokenProvider {
    override fun getAccessToken(): String? = tokenStorage.getAccessToken()
}

class NoOpTokenProvider @Inject constructor() : TokenProvider {
    override fun getAccessToken(): String? = null
}

