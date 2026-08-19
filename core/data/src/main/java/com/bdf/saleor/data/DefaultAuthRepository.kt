package com.bdf.saleor.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.bdf.saleor.core.datastore.TokenStore
import com.bdf.saleor.core.network.ApolloCache
import com.bdf.saleor.data.model.Address
import com.bdf.saleor.data.model.AuthResult
import com.bdf.saleor.data.model.AuthState
import com.bdf.saleor.data.model.MembershipInfo
import com.bdf.saleor.data.model.Money
import com.bdf.saleor.data.model.UserProfile
import com.bdf.saleor.graphql.AccountRegisterMutation
import com.bdf.saleor.graphql.AccountUpdateMutation
import com.bdf.saleor.graphql.CurrentUserProfileQuery
import com.bdf.saleor.graphql.CurrentUserQuery
import com.bdf.saleor.graphql.PasswordChangeMutation
import com.bdf.saleor.graphql.RequestPasswordResetMutation
import com.bdf.saleor.graphql.TokenCreateMutation
import com.bdf.saleor.graphql.fragment.AddressDetails
import com.bdf.saleor.graphql.fragment.UserDetails
import com.bdf.saleor.graphql.type.AccountInput
import com.bdf.saleor.graphql.type.AccountRegisterInput
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    @Named("bare") private val bareClient: ApolloClient,
    private val tokenStore: TokenStore,
    private val apolloCache: ApolloCache,
    private val config: SaleorCatalogConfig,
    private val cartRepository: CartRepository,
) : AuthRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        scope.launch { restoreSession() }
    }

    private suspend fun restoreSession() {
        if (tokenStore.refreshToken().isNullOrBlank()) {
            _currentUser.value = null
            _authState.value = AuthState.LoggedOut
            return
        }
        runCatching { fetchCurrentUser() }
            .onSuccess { user ->
                _currentUser.value = user
                _authState.value = if (user == null) {
                    AuthState.LoggedOut
                } else {
                    AuthState.LoggedIn(user.email)
                }
                if (user != null) {
                    runCatching { cartRepository.adoptLoggedInCart() }
                }
            }
            .onFailure {
                runCatching { cartRepository.releaseOnLogout() }
                tokenStore.clear()
                runCatching { apolloCache.clear() }
                _currentUser.value = null
                _authState.value = AuthState.LoggedOut
            }
    }

    override suspend fun login(email: String, password: String): AuthResult {
        val data = bareClient.mutation(
            TokenCreateMutation(email = email.trim(), password = password),
        ).execute().dataAssertNoErrors
        val payload = data.tokenCreate
        val errors = payload?.errors.orEmpty()
        val token = payload?.token
        val refresh = payload?.refreshToken
        if (!errors.isEmpty() || token.isNullOrBlank() || refresh.isNullOrBlank()) {
            return AuthResult(
                success = false,
                message = errors.firstOrNull()?.message ?: "로그인에 실패했습니다",
            )
        }
        tokenStore.setAccessToken(token)
        tokenStore.setRefreshToken(refresh)
        val user = runCatching { fetchCurrentUser() }.getOrNull()
        _currentUser.value = user
        _authState.value = AuthState.LoggedIn(user?.email ?: email.trim())
        runCatching { cartRepository.adoptLoggedInCart() }
        return AuthResult(success = true)
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): AuthResult {
        val input = AccountRegisterInput(
            email = email.trim(),
            password = password,
            firstName = Optional.presentIfNotNull(firstName.trim().takeIf { it.isNotEmpty() }),
            lastName = Optional.presentIfNotNull(lastName.trim().takeIf { it.isNotEmpty() }),
            channel = Optional.present(config.channel),
            redirectUrl = Optional.present(config.accountConfirmRedirectUrl),
        )
        val data = bareClient.mutation(AccountRegisterMutation(input)).execute().dataAssertNoErrors
        val payload = data.accountRegister
        val errors = payload?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            return AuthResult(
                success = false,
                message = errors.firstOrNull()?.message ?: "회원가입에 실패했습니다",
            )
        }
        return AuthResult(
            success = true,
            requiresConfirmation = payload?.requiresConfirmation == true,
            message = if (payload?.requiresConfirmation == true) {
                "확인 이메일을 보냈습니다. 메일함에서 가입을 완료하세요."
            } else {
                "회원가입이 완료되었습니다. 로그인해 주세요."
            },
        )
    }

    override suspend fun requestPasswordReset(email: String): AuthResult {
        val data = bareClient.mutation(
            RequestPasswordResetMutation(
                email = email.trim(),
                redirectUrl = config.passwordResetRedirectUrl,
                channel = Optional.present(config.channel),
            ),
        ).execute().dataAssertNoErrors
        val errors = data.requestPasswordReset?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            return AuthResult(
                success = false,
                message = errors.firstOrNull()?.message ?: "비밀번호 재설정 요청에 실패했습니다",
            )
        }
        return AuthResult(
            success = true,
            message = "재설정 링크를 이메일로 보냈습니다. 메일함에서 비밀번호를 변경하세요.",
        )
    }

    override suspend fun logout() {
        runCatching { cartRepository.releaseOnLogout() }
        tokenStore.clear()
        runCatching { apolloCache.clear() }
        _currentUser.value = null
        _authState.value = AuthState.LoggedOut
    }

    override suspend fun getProfile(): UserProfile? {
        val profile = fetchCurrentUserProfile()
        if (profile != null) {
            _currentUser.value = profile
            _authState.value = AuthState.LoggedIn(profile.email)
        }
        return profile
    }

    override suspend fun updateName(firstName: String, lastName: String): AuthResult {
        val data = apolloClient.mutation(
            AccountUpdateMutation(
                AccountInput(
                    firstName = Optional.present(firstName.trim()),
                    lastName = Optional.present(lastName.trim()),
                ),
            ),
        )
            .execute()
            .dataAssertNoErrors
        val errors = data.accountUpdate?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            return AuthResult(
                success = false,
                message = errors.firstOrNull()?.message ?: "이름 수정에 실패했습니다",
            )
        }
        val details = data.accountUpdate?.user?.userDetails
        if (details != null) {
            _currentUser.value = (_currentUser.value ?: details.toProfile()).copy(
                firstName = details.firstName,
                lastName = details.lastName,
                email = details.email,
            )
            _authState.value = AuthState.LoggedIn(details.email)
        }
        return AuthResult(success = true)
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): AuthResult {
        val data = apolloClient.mutation(
            PasswordChangeMutation(
                oldPassword = oldPassword,
                newPassword = newPassword,
            ),
        ).execute().dataAssertNoErrors
        val errors = data.passwordChange?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            return AuthResult(
                success = false,
                message = errors.firstOrNull()?.message ?: "비밀번호 변경에 실패했습니다",
            )
        }
        return AuthResult(success = true)
    }

    private suspend fun fetchCurrentUser(): UserProfile? {
        val data = apolloClient.query(CurrentUserQuery())
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        return data.me?.userDetails?.toProfile()
    }

    private suspend fun fetchCurrentUserProfile(): UserProfile? {
        val data = apolloClient.query(CurrentUserProfileQuery())
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        val me = data.me ?: return null
        val details = me.userDetails
        val membership = me.membership?.let { info ->
            MembershipInfo(
                tierName = info.tier?.name,
                nextTierName = info.nextTier?.name,
                currentSpend = Money(info.currentSpend.amount, info.currentSpend.currency),
                amountToNextTier = Money(info.amountToNextTier.amount, info.amountToNextTier.currency),
                couponCode = info.couponCode,
                validFrom = info.validFrom?.toString(),
                validUntil = info.validUntil?.toString(),
                discountPercentage = info.tier?.discountPercentage,
            )
        }
        return UserProfile(
            id = details.id,
            email = details.email,
            firstName = details.firstName,
            lastName = details.lastName,
            avatarUrl = details.avatar?.url,
            dateJoined = details.dateJoined.toString(),
            pointsBalance = me.pointsBalance?.let { Money(it.amount, it.currency) },
            membership = membership,
            addresses = me.addresses.map { it.addressDetails.toAddress() },
            defaultShippingAddressId = me.defaultShippingAddress?.id,
            defaultBillingAddressId = me.defaultBillingAddress?.id,
        )
    }
}

internal fun UserDetails.toProfile(): UserProfile = UserProfile(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    avatarUrl = avatar?.url,
    dateJoined = dateJoined.toString(),
)

internal fun AddressDetails.toAddress(): Address = Address(
    id = id,
    firstName = firstName,
    lastName = lastName,
    companyName = companyName,
    streetAddress1 = streetAddress1,
    streetAddress2 = streetAddress2,
    city = city,
    cityArea = cityArea,
    postalCode = postalCode,
    countryCode = country.code,
    countryName = country.country,
    countryArea = countryArea,
    phone = phone.orEmpty(),
    isDefaultShipping = isDefaultShippingAddress == true,
    isDefaultBilling = isDefaultBillingAddress == true,
)
