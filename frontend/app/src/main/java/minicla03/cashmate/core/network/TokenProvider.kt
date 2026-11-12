package minicla03.cashmate.core.network

import javax.inject.Inject

/**
 * Abstraction supplying an access token for authenticated backend requests.
 * For the unauthenticated MVP, [NoOpTokenProvider] supplies null so no Authorization header is sent.
 */
interface TokenProvider {
    fun getAccessToken(): String?
}

class NoOpTokenProvider @Inject constructor() : TokenProvider {
    override fun getAccessToken(): String? = null
}

