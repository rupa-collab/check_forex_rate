package com.checkrate.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.checkrate.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val baseCurrency = stringPreferencesKey("base_currency")
        val trackedCurrencies = stringSetPreferencesKey("tracked_currencies")
        val thresholdsJson = stringPreferencesKey("thresholds_json")
        val alertsEnabled = booleanPreferencesKey("alerts_enabled")
        val dailySummaryEnabled = booleanPreferencesKey("daily_summary_enabled")
        val liveModeEnabled = booleanPreferencesKey("live_mode_enabled")
        val liveModeInterval = intPreferencesKey("live_mode_interval")
        val lastRatesJson = stringPreferencesKey("last_rates_json")
        val lastUpdated = stringPreferencesKey("last_updated")
        val lastUpdatedFx = stringPreferencesKey("last_updated_fx")
        val lastUpdatedMetals = stringPreferencesKey("last_updated_metals")
        val monthlyRequestCount = intPreferencesKey("monthly_request_count")
        val monthlyRequestMonth = stringPreferencesKey("monthly_request_month")
        val lastRequestEpoch = longPreferencesKey("last_request_epoch")
        val authToken = stringPreferencesKey("auth_token")
        val authEmail = stringPreferencesKey("auth_email")
        val authApiBaseUrl = stringPreferencesKey("auth_api_base_url")
    }

    private val defaultMetals = setOf("XAU", "XAG", "XAU22")
    private val requiredTracked = setOf("USD", "GBP", "EUR", "AUD", "AED", "INR")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val base = prefs[Keys.baseCurrency] ?: "USD"
        val tracked = (prefs[Keys.trackedCurrencies] ?: requiredTracked) + requiredTracked
        val thresholds = jsonToThresholds(prefs[Keys.thresholdsJson])
        val alertsEnabled = prefs[Keys.alertsEnabled] ?: true
        val dailySummaryEnabled = prefs[Keys.dailySummaryEnabled] ?: true
        val liveModeEnabled = prefs[Keys.liveModeEnabled] ?: false
        val liveModeInterval = prefs[Keys.liveModeInterval] ?: 60
        val lastRequestEpoch = prefs[Keys.lastRequestEpoch] ?: 0L
        val authApiBaseUrl = prefs[Keys.authApiBaseUrl] ?: BuildConfig.AUTH_API_BASE_URL.ifBlank { "https://checkforex-backend.onrender.com" }

        val currentMonth = currentMonthKey()
        val storedMonth = prefs[Keys.monthlyRequestMonth] ?: currentMonth
        val count = if (storedMonth == currentMonth) {
            prefs[Keys.monthlyRequestCount] ?: 0
        } else {
            0
        }

        AppSettings(
            baseCurrency = base,
            trackedCurrencies = tracked,
            trackedMetals = setOf("XAU", "XAG"),
            thresholds = thresholds.ifEmpty { defaultThresholds(tracked, defaultMetals) },
            alertsEnabled = alertsEnabled,
            dailySummaryEnabled = dailySummaryEnabled,
            liveModeEnabled = liveModeEnabled,
            liveModeIntervalMinutes = liveModeInterval,
            monthlyRequestCount = count,
            monthlyRequestLimit = 100,
            requestMonthKey = currentMonth,
            lastRequestEpochMillis = lastRequestEpoch,
            authApiBaseUrl = authApiBaseUrl
        )
    }

    val authTokenFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.authToken] ?: ""
    }

    val authEmailFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.authEmail] ?: ""
    }

    val lastRatesFlow: Flow<LastRates> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.lastRatesJson] ?: "{}"
        val lastUpdatedFx = prefs[Keys.lastUpdatedFx]?.toLongOrNull() ?: prefs[Keys.lastUpdated]?.toLongOrNull() ?: 0L
        val lastUpdatedMetals = prefs[Keys.lastUpdatedMetals]?.toLongOrNull() ?: prefs[Keys.lastUpdated]?.toLongOrNull() ?: 0L
        val allRates = jsonToRates(json)
        val metals = allRates.filterKeys { it in defaultMetals }
        val fx = allRates.filterKeys { it !in defaultMetals }
        LastRates(
            fxRates = fx,
            metalsRates = metals,
            fxUpdatedEpochMillis = lastUpdatedFx,
            metalsUpdatedEpochMillis = lastUpdatedMetals
        )
    }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun setBaseCurrency(value: String) {
        context.dataStore.edit { it[Keys.baseCurrency] = value.uppercase() }
    }

    suspend fun setAuthApiBaseUrl(value: String) {
        val trimmed = value.trim().ifBlank { BuildConfig.AUTH_API_BASE_URL.ifBlank { "https://checkforex-backend.onrender.com" } }
        context.dataStore.edit { it[Keys.authApiBaseUrl] = trimmed }
    }

    suspend fun addTrackedCurrency(code: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.trackedCurrencies] ?: requiredTracked
            val updated = current + code.uppercase()
            prefs[Keys.trackedCurrencies] = updated
            val thresholds = jsonToThresholds(prefs[Keys.thresholdsJson])
            if (!thresholds.containsKey(code.uppercase())) {
                thresholds[code.uppercase()] = 0.0
            }
            prefs[Keys.thresholdsJson] = thresholdsToJson(thresholds)
        }
    }

    suspend fun removeTrackedCurrency(code: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.trackedCurrencies] ?: requiredTracked
            prefs[Keys.trackedCurrencies] = current - code.uppercase()
            val thresholds = jsonToThresholds(prefs[Keys.thresholdsJson])
            thresholds.remove(code.uppercase())
            prefs[Keys.thresholdsJson] = thresholdsToJson(thresholds)
        }
    }

    suspend fun setThreshold(code: String, value: Double) {
        context.dataStore.edit { prefs ->
            val thresholds = jsonToThresholds(prefs[Keys.thresholdsJson])
            thresholds[code.uppercase()] = value
            prefs[Keys.thresholdsJson] = thresholdsToJson(thresholds)
        }
    }

    suspend fun setAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.alertsEnabled] = enabled }
    }

    suspend fun setDailySummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.dailySummaryEnabled] = enabled }
    }

    suspend fun setLiveModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.liveModeEnabled] = enabled }
    }

    suspend fun setLiveModeIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.liveModeInterval] = minutes }
    }

    suspend fun saveLastRates(rates: Map<String, Double>, timestampMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.lastRatesJson] = ratesToJson(rates)
            prefs[Keys.lastUpdated] = timestampMillis.toString()
            prefs[Keys.lastUpdatedFx] = timestampMillis.toString()
            prefs[Keys.lastUpdatedMetals] = timestampMillis.toString()
            prefs[Keys.lastRequestEpoch] = timestampMillis
        }
    }

    suspend fun saveRebasedRates(
        rates: Map<String, Double>,
        fxUpdatedMillis: Long,
        metalsUpdatedMillis: Long
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.lastRatesJson] = ratesToJson(rates)
            prefs[Keys.lastUpdatedFx] = fxUpdatedMillis.toString()
            prefs[Keys.lastUpdatedMetals] = metalsUpdatedMillis.toString()
            val lastUpdated = max(fxUpdatedMillis, metalsUpdatedMillis)
            prefs[Keys.lastUpdated] = lastUpdated.toString()
        }
    }

    suspend fun saveMetalRates(metals: Map<String, Double>, timestampMillis: Long) {
        context.dataStore.edit { prefs ->
            val current = jsonToRates(prefs[Keys.lastRatesJson] ?: "{}")
            val merged = current.toMutableMap()
            merged.putAll(metals)
            prefs[Keys.lastRatesJson] = ratesToJson(merged)
            prefs[Keys.lastUpdatedMetals] = timestampMillis.toString()
        }
    }

    suspend fun getLastRates(): LastRates = lastRatesFlow.first()

    suspend fun setAuthToken(token: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.authToken] = token
            prefs[Keys.authEmail] = email
        }
    }

    suspend fun clearAuthToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.authToken)
            prefs.remove(Keys.authEmail)
        }
    }

    suspend fun incrementMonthlyRequestCount() {
        context.dataStore.edit { prefs ->
            val currentMonth = currentMonthKey()
            val storedMonth = prefs[Keys.monthlyRequestMonth]
            val currentCount = if (storedMonth == currentMonth) {
                prefs[Keys.monthlyRequestCount] ?: 0
            } else {
                0
            }
            prefs[Keys.monthlyRequestMonth] = currentMonth
            prefs[Keys.monthlyRequestCount] = currentCount + 1
        }
    }

    private fun currentMonthKey(): String {
        val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
        return formatter.format(Calendar.getInstance().time)
    }

    private fun ratesToJson(map: Map<String, Double>): String {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    private fun jsonToRates(json: String): Map<String, Double> {
        val obj = JSONObject(json)
        val keys = obj.keys()
        val result = mutableMapOf<String, Double>()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = obj.optDouble(key)
        }
        return result
    }

    private fun thresholdsToJson(map: Map<String, Double>): String {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    private fun jsonToThresholds(json: String?): MutableMap<String, Double> {
        if (json.isNullOrBlank()) return mutableMapOf()
        val obj = JSONObject(json)
        val keys = obj.keys()
        val result = mutableMapOf<String, Double>()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = obj.optDouble(key)
        }
        return result
    }

    private fun defaultThresholds(tracked: Set<String>, metals: Set<String>): Map<String, Double> {
        return (tracked + metals).associateWith { 0.0 }
    }
}