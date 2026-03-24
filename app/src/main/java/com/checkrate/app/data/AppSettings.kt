package com.checkrate.app.data

import com.checkrate.app.BuildConfig

data class AppSettings(
    val baseCurrency: String = "USD",
    val trackedCurrencies: Set<String> = setOf("USD", "GBP", "EUR", "AUD", "AED", "INR"),
    val trackedMetals: Set<String> = setOf("XAU", "XAG"),
    val thresholds: Map<String, Double> = mapOf(
        "USD" to 0.0,
        "GBP" to 0.0,
        "EUR" to 0.0,
        "AUD" to 0.0,
        "AED" to 0.0,
        "INR" to 0.0,
        "XAU" to 0.0,
        "XAG" to 0.0,
        "XAU22" to 0.0
    ),
    val alertsEnabled: Boolean = true,
    val dailySummaryEnabled: Boolean = true,
    val liveModeEnabled: Boolean = false,
    val liveModeIntervalMinutes: Int = 60,
    val monthlyRequestCount: Int = 0,
    val monthlyRequestLimit: Int = 100,
    val requestMonthKey: String = "",
    val lastRequestEpochMillis: Long = 0L,
    val authApiBaseUrl: String = BuildConfig.AUTH_API_BASE_URL.ifBlank { "https://checkforex-backend.onrender.com" }
)

data class LastRates(
    val fxRates: Map<String, Double> = emptyMap(),
    val metalsRates: Map<String, Double> = emptyMap(),
    val fxUpdatedEpochMillis: Long = 0L,
    val metalsUpdatedEpochMillis: Long = 0L
)