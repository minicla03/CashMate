package minicla03.cashmate.core.ui

import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.TransactionType
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

object MoneyFormatter {

    fun format(
        money: Money,
        currency: Currency,
        locale: Locale = Locale.getDefault()
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val decimal = BigDecimal.valueOf(money.minorUnits, 2)
        return formatter.format(decimal)
    }

    fun formatSigned(
        money: Money,
        type: TransactionType,
        currency: Currency,
        locale: Locale = Locale.getDefault()
    ): String {
        val formatted = format(money.abs(), currency, locale)
        return when (type) {
            TransactionType.INCOME -> "+$formatted"
            TransactionType.EXPENSE -> "-$formatted"
        }
    }

    fun formatSignedDelta(
        delta: Money,
        currency: Currency,
        locale: Locale = Locale.getDefault()
    ): String {
        val formatted = format(delta.abs(), currency, locale)
        return when {
            delta.isPositive -> "+$formatted"
            delta.isNegative -> "-$formatted"
            else -> formatted
        }
    }

    fun formatBasisPointsPercent(
        basisPoints: Int,
        locale: Locale = Locale.getDefault()
    ): String {
        val whole = basisPoints / 100
        val tenth = kotlin.math.abs((basisPoints % 100) / 10)
        val decimalSep = if (locale.language == Locale.ITALIAN.language) ',' else '.'
        return "$whole$decimalSep$tenth%"
    }
}

object DateFormatter {

    fun formatShort(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    fun formatLong(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))

    fun formatMonthShort(yearMonth: YearMonth, locale: Locale = Locale.getDefault()): String =
        yearMonth.format(DateTimeFormatter.ofPattern("MMM", locale))

    fun formatMonthYear(yearMonth: YearMonth, locale: Locale = Locale.getDefault()): String =
        yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
}

