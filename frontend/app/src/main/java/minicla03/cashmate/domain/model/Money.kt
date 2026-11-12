package minicla03.cashmate.domain.model

import java.math.BigDecimal

/**
 * Exact monetary representation stored in the currency's smallest minor unit (e.g. 1234 -> €12.34).
 * Avoids floating-point rounding errors in financial calculations.
 */
@JvmInline
value class Money(val minorUnits: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(minorUnits + other.minorUnits)

    operator fun minus(other: Money): Money = Money(minorUnits - other.minorUnits)

    operator fun unaryMinus(): Money = Money(-minorUnits)

    override fun compareTo(other: Money): Int = minorUnits.compareTo(other.minorUnits)

    val isPositive: Boolean get() = minorUnits > 0L
    val isNegative: Boolean get() = minorUnits < 0L
    val isZero: Boolean get() = minorUnits == 0L

    fun abs(): Money = Money(kotlin.math.abs(minorUnits))

    /**
     * Returns an exact decimal string suitable for editing in form inputs (e.g., "12.34").
     */
    fun toDecimalInputString(): String = BigDecimal.valueOf(minorUnits, 2).toPlainString()

    companion object {
        val ZERO = Money(0L)
    }
}

