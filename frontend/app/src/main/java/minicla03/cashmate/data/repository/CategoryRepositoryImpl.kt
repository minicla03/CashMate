package minicla03.cashmate.data.repository

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.data.remote.datasource.CategoryRemoteDataSource
import minicla03.cashmate.data.remote.mapper.toCreateRequest
import minicla03.cashmate.data.remote.mapper.toDomain
import minicla03.cashmate.data.remote.mapper.toUpdateRequest
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.repository.CategoryRepository
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val remoteDataSource: CategoryRemoteDataSource,
    private val safeRemoteCall: SafeRemoteCall
) : CategoryRepository {

    override suspend fun getCategories(): AppResult<List<Category>> =
        safeRemoteCall {
            remoteDataSource.getCategories().map { it.toDomain() }
        }

    override suspend fun createCategory(category: Category): AppResult<Category> =
        safeRemoteCall {
            remoteDataSource.createCategory(category.toCreateRequest()).toDomain()
        }

    override suspend fun updateCategory(category: Category): AppResult<Category> =
        safeRemoteCall {
            remoteDataSource.updateCategory(
                id = category.id,
                request = category.toUpdateRequest()
            ).toDomain()
        }

    override suspend fun deleteCategory(id: String): AppResult<Unit> =
        safeRemoteCall {
            remoteDataSource.deleteCategory(id)
        }
}

