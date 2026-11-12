package minicla03.cashmate.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import minicla03.cashmate.data.repository.AccountRepositoryImpl
import minicla03.cashmate.data.repository.AuthRepositoryImpl
import minicla03.cashmate.data.repository.CategoryRepositoryImpl
import minicla03.cashmate.data.repository.InMemoryUserPreferencesRepository
import minicla03.cashmate.data.repository.ReceiptRepositoryImpl
import minicla03.cashmate.data.repository.TransactionRepositoryImpl
import minicla03.cashmate.data.repository.UserRepositoryImpl
import minicla03.cashmate.domain.repository.AccountRepository
import minicla03.cashmate.domain.repository.AuthRepository
import minicla03.cashmate.domain.repository.CategoryRepository
import minicla03.cashmate.domain.repository.ReceiptRepository
import minicla03.cashmate.domain.repository.TransactionRepository
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import minicla03.cashmate.domain.repository.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: InMemoryUserPreferencesRepository
    ): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindReceiptRepository(
        impl: ReceiptRepositoryImpl
    ): ReceiptRepository
}

