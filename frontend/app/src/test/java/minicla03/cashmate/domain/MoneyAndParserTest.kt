package minicla03.cashmate.domain

import minicla03.cashmate.core.common.MoneyParser
import minicla03.cashmate.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyAndParserTest {

    @Test
    fun `money arithmetic operates accurately on minor units`() {
        val a = Money(1234L)
        val b = Money(566L)
        assertEquals(Money(1800L), a + b)
        assertEquals(Money(668L), a - b)
        assertEquals(Money(-1234L), -a)
        assertEquals(Money(1234L), (-a).abs())
        assertTrue(a > b)
        assertEquals("12.34", a.toDecimalInputString())
    }

    @Test
    fun `money parser accepts integer dot and comma decimal inputs`() {
        assertEquals(Money(500L), MoneyParser.parse("5"))
        assertEquals(Money(1234L), MoneyParser.parse("12.34"))
        assertEquals(Money(1234L), MoneyParser.parse("12,34"))
        assertEquals(Money(1250L), MoneyParser.parse("12.5"))
        assertEquals(Money(50L), MoneyParser.parse(" 0,50 "))
    }

    @Test
    fun `money parser rejects invalid formats and excessive decimals`() {
        assertNull(MoneyParser.parse(""))
        assertNull(MoneyParser.parse("   "))
        assertNull(MoneyParser.parse("-10.00"))
        assertNull(MoneyParser.parse("12.345"))
        assertNull(MoneyParser.parse("12.3.4"))
        assertNull(MoneyParser.parse("12,3,4"))
        assertNull(MoneyParser.parse("abc"))
    }
}

