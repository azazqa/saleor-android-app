package com.bdf.saleor.core.testing.fake

import com.bdf.saleor.core.data.AuthRepository
import com.bdf.saleor.core.data.CartRepository
import com.bdf.saleor.core.model.AuthState
import com.bdf.saleor.core.model.Money
import com.bdf.saleor.core.model.SaleorException
import com.bdf.saleor.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository(
    initialState: AuthState = AuthState.LoggedOut,
    private val cartRepository: CartRepository? = null,
) : AuthRepository {
    var profile: UserProfile = UserProfile(
        id = "u1",
        email = "user@test.com",
        firstName = "Test",
        lastName = "User",
        avatarUrl = null,
        dateJoined = "2024-01-01T00:00:00+00:00",
        pointsBalance = Money(5_000.0, "KRW"),
    )
    var shouldFailLogin: Boolean = false
    var shouldFailRegister: Boolean = false
    var lastLoginEmail: String? = null
    var logoutCount: Int = 0

    private val _authState = MutableStateFlow(initialState)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow(
        if (initialState is AuthState.LoggedIn) profile else null,
    )
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Result<String?> {
        lastLoginEmail = email
        if (shouldFailLogin) return Result.failure(SaleorException("login failed"))
        profile = profile.copy(email = email)
        _currentUser.value = profile
        _authState.value = AuthState.LoggedIn(email)
        cartRepository?.adoptLoggedInCart()
        return Result.success(null)
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): Result<String?> {
        if (shouldFailRegister) return Result.failure(SaleorException("register failed"))
        return Result.success("회원가입이 완료되었습니다. 로그인해 주세요.")
    }

    override suspend fun requestPasswordReset(email: String): Result<String?> {
        return Result.success("재설정 링크를 이메일로 보냈습니다.")
    }

    override suspend fun logout() {
        logoutCount += 1
        cartRepository?.releaseOnLogout()
        _currentUser.value = null
        _authState.value = AuthState.LoggedOut
    }

    override suspend fun getProfile(): UserProfile {
        _currentUser.value = profile
        return profile
    }

    override suspend fun updateName(firstName: String, lastName: String): Result<String?> {
        profile = profile.copy(firstName = firstName, lastName = lastName)
        _currentUser.value = profile
        return Result.success(null)
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<String?> {
        if (oldPassword == "wrong") return Result.failure(SaleorException("password failed"))
        return Result.success(null)
    }
}
