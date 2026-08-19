package com.bdf.saleor.feature.checkout.checkout

import com.bdf.saleor.core.model.Address
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PaymentDisplayTest {
    @Test
    fun displayLines_dropsCountryAndUsesRegionThenPostal() {
        val address = Address(
            id = "a1",
            firstName = "bbbbb",
            lastName = "bbbbb",
            companyName = "",
            streetAddress1 = "대저동서로222번길 7",
            streetAddress2 = "501호",
            city = "강서구",
            cityArea = "",
            postalCode = "46719",
            countryCode = "KR",
            countryName = "South Korea",
            countryArea = "부산광역시",
            phone = "010-1133-3111",
            isDefaultShipping = true,
            isDefaultBilling = false,
        )

        val lines = address.displayLines()

        assertEquals("bbbbb bbbbb", lines.name)
        assertEquals("대저동서로222번길 7, 501호", lines.street)
        assertEquals("부산광역시 · 46719", lines.locality)
        assertEquals("010-1133-3111", lines.phone)
        assertFalse(lines.locality.contains("South Korea"))
        assertFalse(lines.locality.contains("강서구"))
        assertEquals(1, "46719".toRegex().findAll(lines.locality + lines.street).count())
    }

    @Test
    fun displayLines_includePhone_appendsContact() {
        val address = Address(
            id = "a1",
            firstName = "홍",
            lastName = "",
            companyName = "",
            streetAddress1 = "테헤란로 1",
            streetAddress2 = "",
            city = "서울특별시",
            cityArea = "",
            postalCode = "06236",
            countryCode = "KR",
            countryName = "South Korea",
            countryArea = "",
            phone = "010-1234-5678",
            isDefaultShipping = false,
            isDefaultBilling = false,
        )

        val lines = address.displayLines(includePhone = true)

        assertEquals("테헤란로 1", lines.street)
        assertEquals("서울특별시 · 06236 · 010-1234-5678", lines.locality)
    }

    @Test
    fun formatGroupedAmount_usesThousandsSeparator() {
        assertEquals("1,000", formatGroupedAmount(1_000.0))
        assertEquals("62,000", formatGroupedAmount(62_000.0))
    }
}
