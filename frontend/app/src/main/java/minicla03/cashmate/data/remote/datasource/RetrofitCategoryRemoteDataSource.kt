package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.api.CashMateApi
import minicla03.cashmate.data.remote.dto.CategoryDto
import minicla03.cashmate.data.remote.dto.CreateCategoryRequest
import minicla03.cashmate.data.remote.dto.UpdateCategoryRequest
import retrofit2.HttpException
import javax.inject.Inject

class RetrofitCategoryRemoteDataSource @Inject constructor(
    private val api: CashMateApi
) : CategoryRemoteDataSource {

    override suspend fun getCategories(): List<CategoryDto> = api.getCategories()

    override suspend fun createCategory(request: CreateCategoryRequest): CategoryDto =
        api.createCategory(request)

    override suspend fun updateCategory(
        id: String,
        request: UpdateCategoryRequest
    ): CategoryDto = api.updateCategory(id, request)

    override suspend fun deleteCategory(id: String) {
        val response = api.deleteCategory(id)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }
}

