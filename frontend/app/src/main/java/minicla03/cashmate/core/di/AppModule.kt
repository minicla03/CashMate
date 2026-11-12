package minicla03.cashmate.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import minicla03.cashmate.data.platform.AndroidReceiptImageReader
import minicla03.cashmate.feature.receipt.presentation.ReceiptImageReader
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {

    @Binds
    @Singleton
    abstract fun bindReceiptImageReader(
        impl: AndroidReceiptImageReader
    ): ReceiptImageReader
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

