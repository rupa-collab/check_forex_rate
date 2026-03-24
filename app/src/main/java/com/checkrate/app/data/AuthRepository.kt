package com.checkrate.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val settingsRepository: SettingsRepository
) {
    private suspend fun api(): AuthApi {
        val baseUrl = settingsRepository.getSettings().authApiBaseUrl
        return AuthApi(baseUrl)
    }

    suspend fun requestOtp(email: String): String = withContext(Dispatchers.IO) {
        api().requestOtp(email)
    }

    suspend fun verifyOtp(email: String, password: String, otp: String) = withContext(Dispatchers.IO) {
        val token = api().verifyOtp(email, password, otp)
        settingsRepository.setAuthToken(token, email)
    }

    suspend fun login(email: String, password: String) = withContext(Dispatchers.IO) {
        val token = api().login(email, password)
        settingsRepository.setAuthToken(token, email)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        settingsRepository.clearAuthToken()
    }
}