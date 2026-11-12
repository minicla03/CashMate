package minicla03.cashmate.domain.repository

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Category

interface CategoryRepository {

    suspend fun getCategories(): AppResult<List<Category>>

    suspend fun createCategory(category: Category): AppResult<Category>

    suspend fun updateCategory(category: Category): AppResult<Category>

    suspend fun deleteCategory(id: String): AppResult<Unit>
}

