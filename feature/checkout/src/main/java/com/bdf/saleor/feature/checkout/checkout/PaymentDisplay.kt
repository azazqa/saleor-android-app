package com.bdf.saleor.feature.checkout.checkout

import com.bdf.saleor.core.model.Address
import com.bdf.saleor.core.model.AddressDraft

internal const val PointsMinUnit = 100.0

internal data class AddressDisplayLines(
    val name: String,
    val street: String,
    val locality: String,
    val phone: String,
)

internal fun Address.displayLines(includePhone: Boolean = false): AddressDisplayLines {
    val street = if (streetAddress2.isNotBlank()) {
        "$streetAddress1, $streetAddress2"
    } else {
        streetAddress1
    }
    val region = countryArea.ifBlank { city }
    val locality = buildList {
        if (region.isNotBlank()) add(region)
        if (postalCode.isNotBlank()) add(postalCode)
        if (includePhone) {
            val phone = displayPhone()
            if (phone.isNotBlank()) add(phone)
        }
    }.joinToString(" · ")
    return AddressDisplayLines(
        name = recipientName,
        street = street,
        locality = locality,
        phone = displayPhone(),
    )
}

internal fun AddressDraft.displayLines(includePhone: Boolean = false): AddressDisplayLines {
    val street = if (streetAddress2.isNotBlank()) {
        "$streetAddress1, $streetAddress2"
    } else {
        streetAddress1
    }
    val region = countryArea.ifBlank { city }
    val locality = buildList {
        if (region.isNotBlank()) add(region)
        if (postalCode.isNotBlank()) add(postalCode)
        if (includePhone && phone.isNotBlank()) add(phone)
    }.joinToString(" · ")
    return AddressDisplayLines(
        name = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" "),
        street = street,
        locality = locality,
        phone = phone,
    )
}

internal fun Address.toFormDraft() = AddressDraft(
    firstName = firstName,
    lastName = lastName,
    companyName = companyName,
    streetAddress1 = streetAddress1,
    streetAddress2 = streetAddress2,
    city = city,
    cityArea = cityArea,
    postalCode = postalCode,
    countryCode = countryCode.ifBlank { "KR" },
    countryArea = countryArea,
    phone = displayPhone(),
)

internal fun formatGroupedAmount(amount: Double): String =
    "%,d".format(amount.toInt().coerceAtLeast(0))

internal fun parseGroupedAmount(value: String): Double? =
    value.replace(",", "").replace(" ", "").filter { it.isDigit() || it == '.' }
        .toDoubleOrNull()
