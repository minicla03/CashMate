package minicla03.cashmate.core.common

import minicla03.cashmate.domain.model.Money
import java.math.BigDecimal

/**
 * Exact decimal parser converting user input strings into [Money] without floating-point arithmetic.
 */
object MoneyParser {

    private val VALID_AMOUNT_REGEX = Regex("^\\d+(\\.\\d{1,2})?$")
    private const val MAX_MINOR_UNITS = 99_999_999_999L

    fun parse(rawInput: String): Money? {
        val trimmed = rawInput.trim().replace(" ", "")
        if (trimmed.isEmpty()) return null

        val normalized = normalizeDecimalSeparator(trimmed) ?: return null
        if (!VALID_AMOUNT_REGEX.matches(normalized)) return null

        return try {
            val minorUnits = BigDecimal(normalized).movePointRight(2).longValueExact()
            if (minorUnits > MAX_MINOR_UNITS) null else Money(minorUnits)
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun normalizeDecimalSeparator(input: String): String? {
        val commaCount = input.count { it == ',' }
        val dotCount = input.count { it == '.' }
        return when {
            commaCount == 0 && dotCount <= 1 -> input
            dotCount == 0 && commaCount == 1 -> input.replace(',', '.')
            else -> null
        }
    }
}

