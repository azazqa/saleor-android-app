package com.bdf.saleor.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ParseCartQuantityTest {
    @Test
    fun clampsBelowOneToOne() {
        assertEquals(1, parseCartQuantity(0, null))
        assertEquals(1, parseCartQuantity(-3, 10))
    }

    @Test
    fun clampsToAvailableWhenKnown() {
        assertEquals(4, parseCartQuantity(4, 10))
        assertEquals(5, parseCartQuantity(9, 5))
    }

    @Test
    fun noMaxWhenAvailableUnknownOrZero() {
        assertEquals(7, parseCartQuantity(7, null))
        assertEquals(7, parseCartQuantity(7, 0))
    }
}
