package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.dto.CategoryDto
import minicla03.cashmate.data.remote.dto.CreateCategoryRequest
import minicla03.cashmate.data.remote.dto.UpdateCategoryRequest
import javax.inject.Inject

class FakeCategoryRemoteDataSource @Inject constructor(
    private val store: FakeRemoteStore
) : CategoryRemoteDataSource {

    override suspend fun getCategories(): List<CategoryDto> =
        store.getCategories()

    override suspend fun createCategory(request: CreateCategoryRequest): CategoryDto =
        store.createCategory(request)

    override suspend fun updateCategory(
        id: String,
        request: UpdateCategoryRequest
    ): CategoryDto = store.updateCategory(id, request)

    override suspend fun deleteCategory(id: String) =
        store.deleteCategory(id)
}

