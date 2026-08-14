package com.bdf.saleor.ui.checkout

import com.bdf.saleor.data.model.AddressDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class KakaoPostcodeMapperTest {
    @Test
    fun mapsRoadAddressWhenUserSelectsRoad() {
        val patch = KakaoPostcodeSelection(
            zonecode = "04780",
            address = "서울 성동구 왕십리로 1",
            roadAddress = "서울 성동구 왕십리로 1",
            jibunAddress = "서울 성동구 하왕십리동 1",
            userSelectedType = "R",
            sido = "서울",
            sigungu = "성동구",
            bname = "하왕십리동",
        ).toAddressPatch()

        assertEquals("04780", patch.postalCode)
        assertEquals("서울 성동구 왕십리로 1", patch.streetAddress1)
        assertEquals("성동구", patch.city)
        assertEquals("서울", patch.countryArea)
    }

    @Test
    fun mapsJibunAddressWhenUserSelectsJibun() {
        val patch = KakaoPostcodeSelection(
            zonecode = "04780",
            address = "서울 성동구 하왕십리동 1",
            roadAddress = "서울 성동구 왕십리로 1",
            jibunAddress = "서울 성동구 하왕십리동 1",
            userSelectedType = "J",
            sido = "서울",
            sigungu = "성동구",
            bname = "하왕십리동",
        ).toAddressPatch()

        assertEquals("서울 성동구 하왕십리동 1", patch.streetAddress1)
    }

    @Test
    fun applyKakaoKeepsNamePhoneAndDetail() {
        val draft = AddressDraft(
            firstName = "김민성",
            phone = "010-0000-0000",
            streetAddress2 = "101호",
        ).applyKakao(
            KakaoAddressPatch(
                postalCode = "04780",
                streetAddress1 = "서울 성동구 왕십리로 1",
                city = "성동구",
                countryArea = "서울",
            ),
        )

        assertEquals("김민성", draft.firstName)
        assertEquals("010-0000-0000", draft.phone)
        assertEquals("101호", draft.streetAddress2)
        assertEquals("04780", draft.postalCode)
        assertEquals("서울 성동구 왕십리로 1", draft.streetAddress1)
    }
}
