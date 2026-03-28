package com.checkrate.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkrate.app.BuildConfig
import com.checkrate.app.data.AppSettings
import com.checkrate.app.data.AuthRepository
import com.checkrate.app.data.ExchangeRateApi
import com.checkrate.app.data.GoldApiClient
import com.checkrate.app.data.LastRates
import com.checkrate.app.data.RateRepository
import com.checkrate.app.data.SettingsRepository
import com.checkrate.app.notifications.Notifier
import com.checkrate.app.util.LiveModeController
import com.checkrate.app.util.RateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RateViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepository = SettingsRepository(app)
    private val apiKey = BuildConfig.EXCHANGE_RATE_API_KEY
    private val repository = RateRepository(ExchangeRateApi(apiKey), GoldApiClient(), settingsRepository)
    private val authRepository = AuthRepository(settingsRepository)

    private val _refreshing = MutableStateFlow(false)
    private val _sendingLiveUpdate = MutableStateFlow(false)
    private val _authLoading = MutableStateFlow(false)
    private val _authError = MutableStateFlow<String?>(null)
    private val _authOtpHint = MutableStateFlow<String?>(null)
    private val _authOtpRequested = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _previousFxRates = MutableStateFlow<Map<String, Double>>(emptyMap())

    private val cooldownMinutes = 60
    private val softLimitRatio = 0.80
    private val hardLimitRatio = 0.95

    val uiState = combine(
    settingsRepository.settingsFlow,
    settingsRepository.lastRatesFlow,
    _previousFxRates,
    settingsRepository.authTokenFlow,
    settingsRepository.authEmailFlow,
    _refreshing,
    _sendingLiveUpdate,
    _authLoading,
    _authError,
    _authOtpHint,
    _authOtpRequested,
    _errorMessage
) { values ->
    val settings = values[0] as AppSettings
    val lastRates = values[1] as LastRates
    val previousFxRates = values[2] as Map<String, Double>
    val authToken = values[3] as String
    val authEmail = values[4] as String
    val refreshing = values[5] as Boolean
    val sendingLiveUpdate = values[6] as Boolean
    val authLoading = values[7] as Boolean
    val authError = values[8] as String?
    val authOtpHint = values[9] as String?
    val authOtpRequested = values[10] as Boolean
    val error = values[11] as String?

    RateUiState(
        settings = settings,
        lastRates = lastRates,
        previousFxRates = previousFxRates,
        apiKeyMissing = apiKey.isBlank(),
        authToken = authToken,
        authEmail = authEmail,
        authLoading = authLoading,
        authErrorMessage = authError,
        authOtpHint = authOtpHint,
        authOtpRequested = authOtpRequested,
        isRefreshing = refreshing,
        isSendingLiveUpdate = sendingLiveUpdate,
        errorMessage = error
    )
}.stateIn(viewModelScope, SharingStarted.Eagerly, RateUiState())

    fun refreshRates() {
        if (apiKey.isBlank()) {
            _errorMessage.value = "Missing API key"
            return
        }
        viewModelScope.launch {
            _refreshing.value = true
            _errorMessage.value = null
            try {
                val settings = settingsRepository.getSettings()
                val guard = guardRequests(settings)
                if (guard != null) {
                    _errorMessage.value = guard
                    return@launch
                }
                val previous = settingsRepository.getLastRates().fxRates
                repository.fetchAndStore(settings.baseCurrency, settings.trackedCurrencies, settings.trackedMetals)
                _previousFxRates.value = previous
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "Failed to fetch rates"
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun refreshMetalsOnly() {
        viewModelScope.launch {
            _refreshing.value = true
            _errorMessage.value = null
            try {
                val settings = settingsRepository.getSettings()
                val lastRates = settingsRepository.getLastRates()
                val base = settings.baseCurrency.uppercase()
                val usdToBase = if (base == "USD") 1.0 else lastRates.fxRates["USD"]
                if (usdToBase == null) {
                    _errorMessage.value = "USD rate missing. Run full refresh or track USD."
                    return@launch
                }
                repository.fetchMetalsOnly(settings.baseCurrency, usdToBase)
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "Failed to fetch metals"
            } finally {
                _refreshing.value = false
            }
        }
    }
    fun refreshMetalsAuto() {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.getSettings()
                val lastRates = settingsRepository.getLastRates()
                val base = settings.baseCurrency.uppercase()
                val usdToBase = if (base == "USD") 1.0 else lastRates.fxRates["USD"]
                if (usdToBase == null) {
                    return@launch
                }
                repository.fetchMetalsOnly(settings.baseCurrency, usdToBase)
            } catch (_: Exception) {
                // Ignore background refresh failures
            }
        }
    }

    fun sendLiveUpdateNow() {
        if (apiKey.isBlank()) {
            _errorMessage.value = "Missing API key"
            return
        }
        viewModelScope.launch {
            _sendingLiveUpdate.value = true
            _errorMessage.value = null
            try {
                val settings = settingsRepository.getSettings()
                val guard = guardRequests(settings)
                if (guard != null) {
                    _errorMessage.value = guard
                    return@launch
                }
                val previous = settingsRepository.getLastRates().fxRates
                repository.fetchAndStore(settings.baseCurrency, settings.trackedCurrencies, settings.trackedMetals)
                _previousFxRates.value = previous
                val lastRates = settingsRepository.getLastRates()
                val fxSummary = RateUtils.buildSummary(settings.baseCurrency, lastRates.fxRates, settings.trackedCurrencies)
                val metalSummary = RateUtils.buildMetalSummary(settings.baseCurrency, lastRates.metalsRates, setOf("XAU", "XAG", "XAU22"))
                val summary = "$fxSummary | $metalSummary"
                val notifier = Notifier(getApplication())
                notifier.updateLiveModeNotification(summary)
                notifier.sendRateAlert("Live update", summary, 4001)
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "Failed to fetch live update"
            } finally {
                _sendingLiveUpdate.value = false
            }
        }
    }

    private fun guardRequests(settings: AppSettings): String? {
        val now = System.currentTimeMillis()
        val sinceLast = now - settings.lastRequestEpochMillis
        val cooldownMs = cooldownMinutes * 60_000L
        val usageRatio = if (settings.monthlyRequestLimit <= 0) 0.0
        else settings.monthlyRequestCount.toDouble() / settings.monthlyRequestLimit.toDouble()

        if (usageRatio >= hardLimitRatio) {
            return "Monthly API limit reached. Try again next month."
        }

        if (usageRatio >= softLimitRatio) {
            _errorMessage.value = "Warning: ${"%.0f".format(usageRatio * 100)}% of monthly quota used."
        }

        return null
    }

    fun updateBaseCurrency(value: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            val newBase = value.uppercase()
            try {
                val settings = settingsRepository.getSettings()
                val oldBase = settings.baseCurrency.uppercase()
                if (oldBase == newBase) return@launch
                val lastRates = settingsRepository.getLastRates()
                val oldRates = lastRates.fxRates
                val pivot = oldRates[newBase]
                if (pivot == null || pivot == 0.0) {
                    settingsRepository.setBaseCurrency(newBase)
                    _errorMessage.value = "Missing rate for $newBase. Tap Refresh to update."
                    return@launch
                }
                val factor = 1.0 / pivot
                val rebasedFx = oldRates.mapValues { (code, rate) ->
                    if (code == newBase) 1.0 else rate * factor
                }
                val rebasedMetals = lastRates.metalsRates.mapValues { (_, rate) -> rate * factor }
                settingsRepository.setBaseCurrency(newBase)
                settingsRepository.saveRebasedRates(
                    rebasedFx + rebasedMetals,
                    lastRates.fxUpdatedEpochMillis,
                    lastRates.metalsUpdatedEpochMillis
                )
                _previousFxRates.value = rebasedFx
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "Failed to update base currency"
            }
        }
    }


    fun setAuthApiBaseUrl(value: String) {
        viewModelScope.launch { settingsRepository.setAuthApiBaseUrl(value) }
    }

    fun addCurrency(code: String) {
        viewModelScope.launch { settingsRepository.addTrackedCurrency(code) }
    }

    fun removeCurrency(code: String) {
        viewModelScope.launch { settingsRepository.removeTrackedCurrency(code) }
    }

    fun setThreshold(code: String, value: Double) {
        viewModelScope.launch { settingsRepository.setThreshold(code, value) }
    }

    fun setAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAlertsEnabled(enabled) }
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDailySummaryEnabled(enabled) }
    }

    fun setLiveModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLiveModeEnabled(enabled)
            if (enabled) {
                LiveModeController.start(getApplication())
            } else {
                LiveModeController.stop(getApplication())
            }
        }
    }

    fun setLiveModeInterval(minutes: Int) {
        viewModelScope.launch { settingsRepository.setLiveModeIntervalMinutes(minutes) }
    }

    fun sendTestNotification() {
        val notifier = Notifier(getApplication())
        notifier.sendRateAlert("Test alert", "Notifications are working", 9999)
    }

    fun requestOtp(email: String) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            _authOtpHint.value = null
            _authOtpRequested.value = false
            try {
                val otp = authRepository.requestOtp(email)
                _authOtpHint.value = if (otp.isBlank()) null else "OTP (dev): $otp"
                _authOtpRequested.value = true
            } catch (ex: Exception) {
                _authError.value = ex.message ?: "OTP request failed"
            } finally {
                _authLoading.value = false
            }
        }
    }

    fun resetOtpFlow() {
        _authOtpRequested.value = false
        _authOtpHint.value = null
        _authError.value = null
    }

    fun verifyOtp(email: String, password: String, otp: String) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                authRepository.verifyOtp(email, password, otp)
                _authOtpHint.value = null
                _authOtpRequested.value = false
            } catch (ex: Exception) {
                _authError.value = ex.message ?: "OTP verification failed"
            } finally {
                _authLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            try {
                authRepository.login(email, password)
            } catch (ex: Exception) {
                _authError.value = ex.message ?: "Login failed"
            } finally {
                _authLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}

data class RateUiState(
    val settings: AppSettings = AppSettings(),
    val lastRates: LastRates = LastRates(),
    val previousFxRates: Map<String, Double> = emptyMap(),
    val apiKeyMissing: Boolean = false,
    val authToken: String = "",
    val authEmail: String = "",
    val authLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val authOtpHint: String? = null,
    val authOtpRequested: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSendingLiveUpdate: Boolean = false,
    val errorMessage: String? = null
)