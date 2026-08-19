package com.bdf.saleor.core.data

import com.bdf.saleor.core.model.AuthState
import com.bdf.saleor.core.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentUser: StateFlow<UserProfile?>

    suspend fun login(email: String, password: String): Result<String?>

    suspend fun register(
        email: String,
        password: String,
        firstName: String = "",
        lastName: String = "",
    ): Result<String?>

    suspend fun requestPasswordReset(email: String): Result<String?>

    suspend fun logout()

    suspend fun getProfile(): UserProfile?

    suspend fun updateName(firstName: String, lastName: String): Result<String?>

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<String?>
}
