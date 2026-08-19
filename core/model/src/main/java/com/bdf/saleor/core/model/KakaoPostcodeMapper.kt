package com.bdf.saleor.core.model

data class KakaoPostcodeSelection(
    val zonecode: String,
    val address: String,
    val roadAddress: String,
    val jibunAddress: String,
    val userSelectedType: String,
    val sido: String,
    val sigungu: String,
    val bname: String,
)

data class KakaoAddressPatch(
    val postalCode: String,
    val streetAddress1: String,
    val city: String,
    val countryArea: String,
)

fun KakaoPostcodeSelection.toAddressPatch(): KakaoAddressPatch {
    val streetAddress1 = if (userSelectedType == "R") {
        roadAddress.ifBlank { address }
    } else {
        jibunAddress.ifBlank { address }
    }
    return KakaoAddressPatch(
        postalCode = zonecode,
        streetAddress1 = streetAddress1,
        city = sigungu.ifBlank { bname },
        countryArea = sido,
    )
}

fun AddressDraft.applyKakao(patch: KakaoAddressPatch): AddressDraft = copy(
    postalCode = patch.postalCode,
    streetAddress1 = patch.streetAddress1,
    city = patch.city,
    countryArea = patch.countryArea,
)
