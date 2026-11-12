package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.repository.CategoryRepository
import javax.inject.Inject

class CreateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val validator: CategoryValidator
) {
    suspend operator fun invoke(category: Category): AppResult<Category> {
        val sanitized = category.copy(name = category.name.trim())
        validator.validate(sanitized)?.let { return AppResult.Error(it) }
        return categoryRepository.createCategory(sanitized)
    }
}

