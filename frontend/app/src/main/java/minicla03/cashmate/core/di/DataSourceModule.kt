package minicla03.cashmate.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import minicla03.cashmate.data.remote.datasource.AccountRemoteDataSource
import minicla03.cashmate.data.remote.datasource.CategoryRemoteDataSource
import minicla03.cashmate.data.remote.datasource.FakeAccountRemoteDataSource
import minicla03.cashmate.data.remote.datasource.FakeCategoryRemoteDataSource
import minicla03.cashmate.data.remote.datasource.FakeReceiptRemoteDataSource
import minicla03.cashmate.data.remote.datasource.FakeTransactionRemoteDataSource
import minicla03.cashmate.data.remote.datasource.ReceiptRemoteDataSource
import minicla03.cashmate.data.remote.datasource.TransactionRemoteDataSource
import javax.inject.Singleton

/**
 * Centralizes remote data source bindings.
 * Swap Fake*RemoteDataSource to Retrofit*RemoteDataSource here when connecting to Spring Boot.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRemoteDataSource(
        impl: FakeTransactionRemoteDataSource
    ): TransactionRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCategoryRemoteDataSource(
        impl: FakeCategoryRemoteDataSource
    ): CategoryRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAccountRemoteDataSource(
        impl: FakeAccountRemoteDataSource
    ): AccountRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindReceiptRemoteDataSource(
        impl: FakeReceiptRemoteDataSource
    ): ReceiptRemoteDataSource
}

