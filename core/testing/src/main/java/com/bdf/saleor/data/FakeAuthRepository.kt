package com.bdf.saleor.data

import com.bdf.saleor.data.model.AuthResult
import com.bdf.saleor.data.model.AuthState
import com.bdf.saleor.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository(
    initialState: AuthState = AuthState.LoggedOut,
) : AuthRepository {
    var profile: UserProfile = UserProfile(
        id = "u1",
        email = "user@test.com",
        firstName = "Test",
        lastName = "User",
        avatarUrl = null,
        dateJoined = "2024-01-01T00:00:00+00:00",
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

    override suspend fun login(email: String, password: String): AuthResult {
        lastLoginEmail = email
        if (shouldFailLogin) return AuthResult(false, "login failed")
        profile = profile.copy(email = email)
        _currentUser.value = profile
        _authState.value = AuthState.LoggedIn(email)
        return AuthResult(true)
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): AuthResult {
        if (shouldFailRegister) return AuthResult(false, "register failed")
        return AuthResult(true, message = "회원가입이 완료되었습니다. 로그인해 주세요.")
    }

    override suspend fun requestPasswordReset(email: String): AuthResult {
        return AuthResult(true, message = "재설정 링크를 이메일로 보냈습니다.")
    }

    override suspend fun logout() {
        logoutCount += 1
        _currentUser.value = null
        _authState.value = AuthState.LoggedOut
    }

    override suspend fun getProfile(): UserProfile {
        _currentUser.value = profile
        return profile
    }

    override suspend fun updateName(firstName: String, lastName: String): AuthResult {
        profile = profile.copy(firstName = firstName, lastName = lastName)
        _currentUser.value = profile
        return AuthResult(true)
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): AuthResult {
        if (oldPassword == "wrong") return AuthResult(false, "password failed")
        return AuthResult(true)
    }
}
