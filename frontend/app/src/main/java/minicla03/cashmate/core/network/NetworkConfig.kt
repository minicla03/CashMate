package minicla03.cashmate.core.network

import minicla03.cashmate.BuildConfig

/**
 * Centralized HTTP client configuration sourced from build configuration.
 */
data class NetworkConfig(
    val baseUrl: String = BuildConfig.API_BASE_URL,
    val connectTimeoutSeconds: Long = 15L,
    val readTimeoutSeconds: Long = 20L,
    val writeTimeoutSeconds: Long = 20L,
    val enableHttpLogging: Boolean = BuildConfig.DEBUG
)

