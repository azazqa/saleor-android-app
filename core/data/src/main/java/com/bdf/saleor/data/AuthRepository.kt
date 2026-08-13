package com.bdf.saleor.data

import com.bdf.saleor.data.model.AuthResult
import com.bdf.saleor.data.model.AuthState
import com.bdf.saleor.data.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentUser: StateFlow<UserProfile?>

    suspend fun login(email: String, password: String): AuthResult

    suspend fun register(
        email: String,
        password: String,
        firstName: String = "",
        lastName: String = "",
    ): AuthResult

    suspend fun requestPasswordReset(email: String): AuthResult

    suspend fun logout()

    suspend fun getProfile(): UserProfile?

    suspend fun updateName(firstName: String, lastName: String): AuthResult

    suspend fun changePassword(oldPassword: String, newPassword: String): AuthResult
}
