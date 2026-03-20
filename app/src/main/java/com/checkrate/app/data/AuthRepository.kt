package com.checkrate.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val api: AuthApi,
    private val settingsRepository: SettingsRepository
) {
    suspend fun signupAndLogin(email: String, password: String) = withContext(Dispatchers.IO) {
        api.signup(email, password)
        val token = api.login(email, password)
        settingsRepository.setAuthToken(token, email)
    }

    suspend fun login(email: String, password: String) = withContext(Dispatchers.IO) {
        val token = api.login(email, password)
        settingsRepository.setAuthToken(token, email)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        settingsRepository.clearAuthToken()
    }
}
