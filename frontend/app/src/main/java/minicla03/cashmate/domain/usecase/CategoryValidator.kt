package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.error.ValidationReason
import minicla03.cashmate.domain.model.Category
import javax.inject.Inject

/**
 * Pure domain validation for [Category] creation and editing.
 */
class CategoryValidator @Inject constructor() {

    fun validate(category: Category): AppError.Validation? {
        val errors = buildMap {
            val trimmedName = category.name.trim()
            when {
                trimmedName.isEmpty() ->
                    put(ValidationField.CATEGORY_NAME, ValidationReason.REQUIRED)
                trimmedName.length > MAX_NAME_LENGTH ->
                    put(ValidationField.CATEGORY_NAME, ValidationReason.TOO_LONG)
            }
        }
        return if (errors.isEmpty()) null else AppError.Validation(errors)
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
    }
}

