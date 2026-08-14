package com.bdf.saleor.ui.checkout

import com.bdf.saleor.data.model.digitsOnlyMobile
import com.bdf.saleor.data.model.formatKoreanDisplayPhone
import com.bdf.saleor.data.model.formatKoreanMobileNumber
import org.junit.Assert.assertEquals
import org.junit.Test

class KoreanMobileNumberTest {
    @Test
    fun keepsOnlyDigitsAndCapsAtEleven() {
        assertEquals("01012341234", digitsOnlyMobile("010-1234-1234abc"))
        assertEquals("01012341234", digitsOnlyMobile("010123412345"))
    }

    @Test
    fun formatsAsTyped() {
        assertEquals("010", formatKoreanMobileNumber("010"))
        assertEquals("010-1", formatKoreanMobileNumber("0101"))
        assertEquals("010-123", formatKoreanMobileNumber("010123"))
        assertEquals("010-123-1", formatKoreanMobileNumber("0101231"))
        assertEquals("010-123-1234", formatKoreanMobileNumber("0101231234"))
        assertEquals("010-1231-2341", formatKoreanMobileNumber("01012312341"))
    }

    @Test
    fun formatsTenAndElevenDigitPatterns() {
        assertEquals("010-123-1234", formatKoreanMobileNumber("0101231234"))
        assertEquals("010-1234-1234", formatKoreanMobileNumber("01012341234"))
    }

    @Test
    fun displaysKoreanPhoneInsteadOfCountryCode() {
        assertEquals("010-1234-1234", formatKoreanDisplayPhone("+82 1012341234", "KR"))
        assertEquals("010-1234-1234", formatKoreanDisplayPhone("+821012341234", "KR"))
        assertEquals("010-1234-1234", formatKoreanDisplayPhone("010-1234-1234", "KR"))
        assertEquals("010-123-1234", formatKoreanDisplayPhone("+82 101231234", "KR"))
    }

    @Test
    fun offsetMappingTenDigits() {
        val mapping = KoreanMobileOffsetMapping(10)
        assertEquals(3, mapping.originalToTransformed(3))
        assertEquals(5, mapping.originalToTransformed(4))
        assertEquals(7, mapping.originalToTransformed(6))
        assertEquals(9, mapping.originalToTransformed(7))
        assertEquals(12, mapping.originalToTransformed(10))
        assertEquals(3, mapping.transformedToOriginal(3))
        assertEquals(3, mapping.transformedToOriginal(4))
        assertEquals(6, mapping.transformedToOriginal(7))
        assertEquals(6, mapping.transformedToOriginal(8))
        assertEquals(10, mapping.transformedToOriginal(12))
    }

    @Test
    fun offsetMappingElevenDigits() {
        val mapping = KoreanMobileOffsetMapping(11)
        assertEquals(3, mapping.originalToTransformed(3))
        assertEquals(5, mapping.originalToTransformed(4))
        assertEquals(8, mapping.originalToTransformed(7))
        assertEquals(10, mapping.originalToTransformed(8))
        assertEquals(13, mapping.originalToTransformed(11))
        assertEquals(3, mapping.transformedToOriginal(4))
        assertEquals(7, mapping.transformedToOriginal(8))
        assertEquals(7, mapping.transformedToOriginal(9))
        assertEquals(11, mapping.transformedToOriginal(13))
    }
}
