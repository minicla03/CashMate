package minicla03.cashmate.data.repository

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.data.remote.mapper.NetworkErrorMapper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes a remote suspend call and converts technical failures into typed [AppResult.Error] values.
 */
class SafeRemoteCall @Inject constructor(
    private val errorMapper: NetworkErrorMapper
) {
    suspend operator fun <T> invoke(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        AppResult.Error(errorMapper.map(throwable))
    }
}

