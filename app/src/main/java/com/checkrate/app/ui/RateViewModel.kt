package com.checkrate.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkrate.app.BuildConfig
import com.checkrate.app.data.AppSettings
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

    private val _refreshing = MutableStateFlow(false)
    private val _sendingLiveUpdate = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val cooldownMinutes = 60
    private val softLimitRatio = 0.80
    private val hardLimitRatio = 0.95

    val uiState = combine(
        settingsRepository.settingsFlow,
        settingsRepository.lastRatesFlow,
        _refreshing,
        _sendingLiveUpdate,
        _errorMessage
    ) { settings, lastRates, refreshing, sendingLiveUpdate, error ->
        RateUiState(
            settings = settings,
            lastRates = lastRates,
            apiKeyMissing = apiKey.isBlank(),
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
                repository.fetchAndStore(settings.baseCurrency, settings.trackedCurrencies, settings.trackedMetals)
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
                val lastRates = repository.fetchAndStore(settings.baseCurrency, settings.trackedCurrencies, settings.trackedMetals)
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

        if (sinceLast in 1 until cooldownMs) {
            val minutesLeft = ((cooldownMs - sinceLast) / 60000L).coerceAtLeast(1)
            return "Please wait $minutesLeft min before the next update."
        }

        if (usageRatio >= softLimitRatio) {
            _errorMessage.value = "Warning: ${"%.0f".format(usageRatio * 100)}% of monthly quota used."
        }

        return null
    }

    fun updateBaseCurrency(value: String) {
        viewModelScope.launch { settingsRepository.setBaseCurrency(value) }
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
}

data class RateUiState(
    val settings: AppSettings = AppSettings(),
    val lastRates: LastRates = LastRates(),
    val apiKeyMissing: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSendingLiveUpdate: Boolean = false,
    val errorMessage: String? = null
)
