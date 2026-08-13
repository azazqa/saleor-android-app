package com.bdf.saleor.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.bdf.saleor.data.model.AddressDraft
import com.bdf.saleor.data.model.AddressKind
import com.bdf.saleor.data.model.AuthResult
import com.bdf.saleor.data.model.Money
import com.bdf.saleor.data.model.PointsHistoryEntry
import com.bdf.saleor.data.model.PointsPage
import com.bdf.saleor.graphql.AccountAddressCreateMutation
import com.bdf.saleor.graphql.AccountAddressDeleteMutation
import com.bdf.saleor.graphql.AccountAddressUpdateMutation
import com.bdf.saleor.graphql.AccountRequestDeletionMutation
import com.bdf.saleor.graphql.AccountSetDefaultAddressMutation
import com.bdf.saleor.graphql.CurrentUserPointsQuery
import com.bdf.saleor.graphql.type.AddressInput
import com.bdf.saleor.graphql.type.AddressTypeEnum
import com.bdf.saleor.graphql.type.CountryCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAccountRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val config: SaleorCatalogConfig,
) : AccountRepository {
    override suspend fun getPoints(first: Int, after: String?): PointsPage {
        val data = apolloClient.query(
            CurrentUserPointsQuery(
                first = Optional.present(first),
                after = Optional.presentIfNotNull(after),
            ),
        )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        val me = data.me
        val histories = me?.pointsHistories
        return PointsPage(
            balance = me?.pointsBalance?.let { Money(it.amount, it.currency) },
            entries = histories?.edges.orEmpty().mapNotNull { edge ->
                val node = edge?.node ?: return@mapNotNull null
                PointsHistoryEntry(
                    id = node.id,
                    date = node.date.toString(),
                    type = node.type.rawValue,
                    amount = Money(node.amount.amount, node.amount.currency),
                    balanceAfter = Money(node.balanceAfter.amount, node.balanceAfter.currency),
                    reason = node.reason,
                    orderId = node.order?.id,
                    orderNumber = node.order?.number,
                )
            },
            endCursor = histories?.pageInfo?.endCursor,
            hasNextPage = histories?.pageInfo?.hasNextPage == true,
            totalCount = histories?.totalCount ?: 0,
        )
    }

    override suspend fun createAddress(draft: AddressDraft, defaultKind: AddressKind?): AuthResult {
        val data = apolloClient.mutation(
            AccountAddressCreateMutation(
                input = draft.toInput(),
                type = Optional.presentIfNotNull(defaultKind?.toGraphql()),
            ),
        ).execute().dataAssertNoErrors
        val errors = data.accountAddressCreate?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            return AuthResult(false, errors.firstOrNull()?.message ?: "주소 추가에 실패했습니다")
        }
        return AuthResult(true)
    }

    override suspend fun updateAddress(id: String, draft: AddressDraft): AuthResult {
        val data = apolloClient.mutation(
            AccountAddressUpdateMutation(id = id, input = draft.toInput()),
        ).execute().dataAssertNoErrors
        val errors = data.accountAddressUpdate?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            return AuthResult(false, errors.firstOrNull()?.message ?: "주소 수정에 실패했습니다")
        }
        return AuthResult(true)
    }

    override suspend fun deleteAddress(id: String): AuthResult {
        val data = apolloClient.mutation(AccountAddressDeleteMutation(id = id))
            .execute()
            .dataAssertNoErrors
        val errors = data.accountAddressDelete?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            return AuthResult(false, errors.firstOrNull()?.message ?: "주소 삭제에 실패했습니다")
        }
        return AuthResult(true)
    }

    override suspend fun setDefaultAddress(id: String, kind: AddressKind): AuthResult {
        val data = apolloClient.mutation(
            AccountSetDefaultAddressMutation(id = id, type = kind.toGraphql()),
        ).execute().dataAssertNoErrors
        val errors = data.accountSetDefaultAddress?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            return AuthResult(false, errors.firstOrNull()?.message ?: "기본 주소 설정에 실패했습니다")
        }
        return AuthResult(true)
    }

    override suspend fun requestAccountDeletion(): AuthResult {
        val data = apolloClient.mutation(
            AccountRequestDeletionMutation(
                redirectUrl = config.accountDeleteRedirectUrl,
                channel = Optional.present(config.channel),
            ),
        ).execute().dataAssertNoErrors
        val errors = data.accountRequestDeletion?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            return AuthResult(false, errors.firstOrNull()?.message ?: "계정 삭제 요청에 실패했습니다")
        }
        return AuthResult(true, message = "삭제 확인 메일을 보냈습니다. 메일함에서 계정을 삭제하세요.")
    }
}

private fun AddressDraft.toInput(): AddressInput = AddressInput(
    firstName = Optional.present(firstName.trim()),
    lastName = Optional.present(lastName.trim()),
    companyName = Optional.present(companyName.trim()),
    streetAddress1 = Optional.present(streetAddress1.trim()),
    streetAddress2 = Optional.present(streetAddress2.trim()),
    city = Optional.present(city.trim()),
    cityArea = Optional.present(cityArea.trim()),
    postalCode = Optional.present(postalCode.trim()),
    country = Optional.present(CountryCode.safeValueOf(countryCode.ifBlank { "KR" })),
    countryArea = Optional.present(countryArea.trim()),
    phone = Optional.presentIfNotNull(phone.trim().takeIf { it.isNotEmpty() }),
)

private fun AddressKind.toGraphql(): AddressTypeEnum = when (this) {
    AddressKind.SHIPPING -> AddressTypeEnum.SHIPPING
    AddressKind.BILLING -> AddressTypeEnum.BILLING
}
