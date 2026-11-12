package minicla03.cashmate.feature.receipt.presentation

import minicla03.cashmate.domain.model.Category

/**
 * Matches backend category identifiers or labels (e.g. "FOOD", "cat-food") against the user's
 * configured CashMate [Category] list for display and selection.
 */
object ReceiptCategoryResolver {

    private val aliasMap = mapOf(
        "FOOD" to listOf("cat-food", "food", "groceries"),
        "GROCERIES" to listOf("cat-food", "food", "groceries"),
        "TRANSPORT" to listOf("cat-transport", "transport"),
        "HOUSING" to listOf("cat-housing", "housing", "home"),
        "ENTERTAINMENT" to listOf("cat-entertainment", "entertainment"),
        "HEALTH" to listOf("cat-health", "health", "pharmacy"),
        "SHOPPING" to listOf("cat-shopping", "shopping"),
        "OTHER" to listOf("cat-other", "other"),
        "SALARY" to listOf("cat-salary", "salary")
    )

    fun resolve(rawCategory: String?, categories: List<Category>): Category? {
        if (rawCategory.isNullOrBlank() || categories.isEmpty()) return null
        val trimmed = rawCategory.trim()

        categories.firstOrNull { it.id.equals(trimmed, ignoreCase = true) }?.let { return it }
        categories.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.let { return it }

        val normalized = trimmed.uppercase()
        val aliases = aliasMap[normalized].orEmpty() + listOf(trimmed.lowercase())
        return categories.firstOrNull { category ->
            aliases.any { alias ->
                category.id.equals(alias, ignoreCase = true) ||
                    category.name.contains(alias, ignoreCase = true)
            }
        }
    }
}

