package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.dto.CategoryDto
import minicla03.cashmate.data.remote.dto.CreateCategoryRequest
import minicla03.cashmate.data.remote.dto.UpdateCategoryRequest

interface CategoryRemoteDataSource {

    suspend fun getCategories(): List<CategoryDto>

    suspend fun createCategory(
        request: CreateCategoryRequest
    ): CategoryDto

    suspend fun updateCategory(
        id: String,
        request: UpdateCategoryRequest
    ): CategoryDto

    suspend fun deleteCategory(
        id: String
    )
}

