package com.checkrate.app.data

import com.checkrate.app.util.RateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RateRepository(
    private val fxApi: ExchangeRateApi,
    private val goldApi: GoldApiClient,
    private val settingsRepository: SettingsRepository
) {
    private data class MetalsResult(
        val rates: Map<String, Double>,
        val updatedMillis: Long
    )

    suspend fun fetchAndStore(baseCurrency: String, trackedFx: Set<String>, trackedMetals: Set<String>): LastRates {
        return withContext(Dispatchers.IO) {
            val currenciesToRequest = (trackedFx + baseCurrency)
                .map { it.uppercase() }
                .filter { it != "USD" }
                .toSet()

            val fxResponse = fxApi.fetchQuotes(currenciesToRequest)
            val fxRates = computeRates(
                baseCurrency = baseCurrency.uppercase(),
                targets = trackedFx.map { it.uppercase() }.toSet(),
                quotes = fxResponse.quotes
            )

            val usdToBase = if (baseCurrency.uppercase() == "USD") 1.0 else
                fxResponse.quotes["USD${baseCurrency.uppercase()}"]
                    ?: throw IllegalStateException("Missing USD$baseCurrency quote")

            val metalsResult = fetchMetals(usdToBase)
            val allRates = fxRates + metalsResult.rates

            val timestampMillis = maxOf(fxResponse.timestamp * 1000, metalsResult.updatedMillis)
            settingsRepository.saveLastRates(allRates, timestampMillis)
            settingsRepository.incrementMonthlyRequestCount()

            LastRates(
                fxRates = fxRates,
                metalsRates = metalsResult.rates,
                fxUpdatedEpochMillis = fxResponse.timestamp * 1000,
                metalsUpdatedEpochMillis = metalsResult.updatedMillis
            )
        }
    }

    suspend fun fetchMetalsOnly(baseCurrency: String, usdToBase: Double): LastRates {
        return withContext(Dispatchers.IO) {
            val metalsResult = fetchMetals(usdToBase)
            settingsRepository.saveMetalRates(metalsResult.rates, metalsResult.updatedMillis)
            val current = settingsRepository.getLastRates()
            current.copy(
                metalsRates = metalsResult.rates,
                metalsUpdatedEpochMillis = metalsResult.updatedMillis
            )
        }
    }

    private suspend fun fetchMetals(usdToBase: Double): MetalsResult {
        val xau = goldApi.fetchPrice("XAU")
        val xag = goldApi.fetchPrice("XAG")

        val xauPerGramInBase = RateUtils.usdOunceToBasePerGram(xau.priceUsdPerOunce, usdToBase)
        val xagPerGramInBase = RateUtils.usdOunceToBasePerGram(xag.priceUsdPerOunce, usdToBase)

        val metals = mapOf(
            "XAU" to xauPerGramInBase,
            "XAG" to xagPerGramInBase,
            "XAU22" to xauPerGramInBase * RateUtils.GOLD_22K_FACTOR
        )

        val updatedMillis = maxOf(xau.timestamp, xag.timestamp) * 1000
        return MetalsResult(metals, updatedMillis)
    }

    private fun computeRates(
        baseCurrency: String,
        targets: Set<String>,
        quotes: Map<String, Double>
    ): Map<String, Double> {
        val usdToBase = if (baseCurrency == "USD") 1.0 else quotes["USD$baseCurrency"]
            ?: throw IllegalStateException("Missing USD$baseCurrency quote")

        return targets.associateWith { target ->
            when {
                target == baseCurrency -> 1.0
                target == "USD" -> usdToBase
                baseCurrency == "USD" -> {
                    val usdToTarget = quotes["USD$target"]
                        ?: throw IllegalStateException("Missing USD$target quote")
                    1.0 / usdToTarget
                }
                else -> {
                    val usdToTarget = quotes["USD$target"]
                        ?: throw IllegalStateException("Missing USD$target quote")
                    usdToBase / usdToTarget
                }
            }
        }
    }
}