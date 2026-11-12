package minicla03.cashmate.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import minicla03.cashmate.core.network.AuthInterceptor
import minicla03.cashmate.core.network.NetworkConfig
import minicla03.cashmate.core.network.NoOpTokenProvider
import minicla03.cashmate.core.network.TokenProvider
import minicla03.cashmate.data.remote.api.CashMateApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingsModule {

    @Binds
    @Singleton
    abstract fun bindTokenProvider(impl: NoOpTokenProvider): TokenProvider
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(config: NetworkConfig): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            // BASIC logs request line and response code/time only; financial payloads are never logged.
            level = if (config.enableHttpLogging) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader(AuthInterceptor.HEADER_AUTHORIZATION)
            redactHeader("Cookie")
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        config: NetworkConfig,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        config: NetworkConfig,
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideCashMateApi(retrofit: Retrofit): CashMateApi =
        retrofit.create(CashMateApi::class.java)
}

