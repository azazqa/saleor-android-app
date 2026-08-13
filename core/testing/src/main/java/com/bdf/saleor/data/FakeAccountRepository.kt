package com.bdf.saleor.data

import com.bdf.saleor.data.model.Address
import com.bdf.saleor.data.model.AddressDraft
import com.bdf.saleor.data.model.AddressKind
import com.bdf.saleor.data.model.AuthResult
import com.bdf.saleor.data.model.Money
import com.bdf.saleor.data.model.PointsHistoryEntry
import com.bdf.saleor.data.model.PointsPage

class FakeAccountRepository : AccountRepository {
    var points: PointsPage = PointsPage(
        balance = Money(1000.0, "KRW"),
        entries = listOf(
            PointsHistoryEntry(
                id = "p1",
                date = "2026-07-31T06:40:00+00:00",
                type = "GRANTED",
                amount = Money(1000.0, "KRW"),
                balanceAfter = Money(1000.0, "KRW"),
                reason = "적립금 테스트",
                orderId = null,
                orderNumber = null,
            ),
        ),
        endCursor = null,
        hasNextPage = false,
        totalCount = 1,
    )
    var addresses: MutableList<Address> = mutableListOf()
    var shouldFailPoints: Boolean = false
    var lastDeleteRequested: Boolean = false

    override suspend fun getPoints(first: Int, after: String?): PointsPage {
        if (shouldFailPoints) error("points failed")
        return points
    }

    override suspend fun createAddress(draft: AddressDraft, defaultKind: AddressKind?): AuthResult {
        val id = "a${addresses.size + 1}"
        addresses += draft.toModel(
            id = id,
            isDefaultShipping = defaultKind == AddressKind.SHIPPING,
            isDefaultBilling = defaultKind == AddressKind.BILLING,
        )
        return AuthResult(true)
    }

    override suspend fun updateAddress(id: String, draft: AddressDraft): AuthResult {
        val index = addresses.indexOfFirst { it.id == id }
        if (index < 0) return AuthResult(false, "not found")
        val current = addresses[index]
        addresses[index] = draft.toModel(
            id = id,
            isDefaultShipping = current.isDefaultShipping,
            isDefaultBilling = current.isDefaultBilling,
        )
        return AuthResult(true)
    }

    override suspend fun deleteAddress(id: String): AuthResult {
        addresses.removeAll { it.id == id }
        return AuthResult(true)
    }

    override suspend fun setDefaultAddress(id: String, kind: AddressKind): AuthResult {
        addresses = addresses.map { address ->
            when (kind) {
                AddressKind.SHIPPING -> address.copy(isDefaultShipping = address.id == id)
                AddressKind.BILLING -> address.copy(isDefaultBilling = address.id == id)
            }
        }.toMutableList()
        return AuthResult(true)
    }

    override suspend fun requestAccountDeletion(): AuthResult {
        lastDeleteRequested = true
        return AuthResult(true, message = "삭제 확인 메일을 보냈습니다.")
    }

    private fun AddressDraft.toModel(
        id: String,
        isDefaultShipping: Boolean = false,
        isDefaultBilling: Boolean = false,
    ) = Address(
            id = id,
            firstName = firstName,
            lastName = lastName,
            companyName = companyName,
            streetAddress1 = streetAddress1,
            streetAddress2 = streetAddress2,
            city = city,
            cityArea = cityArea,
            postalCode = postalCode,
            countryCode = countryCode,
            countryName = countryCode,
            countryArea = countryArea,
            phone = phone,
            isDefaultShipping = isDefaultShipping,
            isDefaultBilling = isDefaultBilling,
        )
}
