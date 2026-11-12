package minicla03.cashmate.data.remote.mapper

import kotlinx.serialization.SerializationException
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.BusinessErrorReason
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Translates low-level HTTP, IO, and serialization failures into application-level [AppError] values.
 */
class NetworkErrorMapper @Inject constructor() {

    fun map(throwable: Throwable): AppError {
        if (throwable is CancellationException) throw throwable

        return when (throwable) {
            is SocketTimeoutException -> AppError.Network.Timeout
            is UnknownHostException,
            is ConnectException,
            is NoRouteToHostException -> AppError.Network.NoInternet
            is IOException -> AppError.Network.NoInternet
            is HttpException -> mapHttpStatus(throwable.code())
            is SerializationException -> AppError.Network.Unknown
            is IllegalArgumentException -> AppError.Network.Unknown
            is CategoryInUseException -> AppError.BusinessRule(BusinessErrorReason.CATEGORY_IN_USE)
            is DuplicateCategoryException -> AppError.BusinessRule(BusinessErrorReason.CATEGORY_ALREADY_EXISTS)
            is LastCategoryDeletionException -> AppError.BusinessRule(BusinessErrorReason.CANNOT_DELETE_LAST_CATEGORY)
            else -> AppError.Unknown
        }
    }

    private fun mapHttpStatus(code: Int): AppError = when (code) {
        401 -> AppError.Network.Unauthorized
        403 -> AppError.Network.Forbidden
        404 -> AppError.Network.NotFound
        409 -> AppError.BusinessRule(BusinessErrorReason.CATEGORY_IN_USE)
        in 500..599 -> AppError.Network.ServerError
        else -> AppError.Network.Unknown
    }
}

class CategoryInUseException(message: String = "Category is referenced by existing transactions") :
    IllegalStateException(message)

class DuplicateCategoryException(message: String = "Category already exists") :
    IllegalStateException(message)

class LastCategoryDeletionException(message: String = "Cannot delete the last category") :
    IllegalStateException(message)

