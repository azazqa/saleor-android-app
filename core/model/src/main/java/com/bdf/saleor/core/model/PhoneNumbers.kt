package com.bdf.saleor.core.model

private const val MaxKoreanMobileDigits = 11

fun digitsOnlyMobile(input: String): String = input.filter { it.isDigit() }.take(MaxKoreanMobileDigits)

fun formatKoreanMobileNumber(raw: String): String {
    val digits = digitsOnlyMobile(raw)
    return when (digits.length) {
        0 -> ""
        in 1..3 -> digits
        in 4..6 -> "${digits.take(3)}-${digits.drop(3)}"
        in 7..10 -> "${digits.take(3)}-${digits.substring(3, 6)}-${digits.drop(6)}"
        else -> "${digits.take(3)}-${digits.substring(3, 7)}-${digits.drop(7)}"
    }
}

fun formatKoreanDisplayPhone(phone: String, countryCode: String = "KR"): String {
    if (countryCode.isNotBlank() && !countryCode.equals("KR", ignoreCase = true)) return phone
    if (phone.isBlank()) return ""
    val digits = phone.filter { it.isDigit() }
    val national = when {
        digits.startsWith("82") && digits.length >= 11 -> "0" + digits.drop(2)
        else -> digits
    }
    return formatKoreanMobileNumber(national).ifBlank { phone }
}
