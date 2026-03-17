package com.checkrate.app.worker

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.checkrate.app.BuildConfig
import com.checkrate.app.data.ExchangeRateApi
import com.checkrate.app.data.GoldApiClient
import com.checkrate.app.data.RateRepository
import com.checkrate.app.data.SettingsRepository
import com.checkrate.app.notifications.Notifier
import com.checkrate.app.util.RateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class LiveRateService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notifier = Notifier(this)
        startForeground(NOTIFICATION_ID, notifier.buildLiveModeNotification("Starting live updates"))

        if (job?.isActive == true) return START_STICKY

        job = scope.launch {
            val settingsRepository = SettingsRepository(applicationContext)
            val apiKey = BuildConfig.EXCHANGE_RATE_API_KEY
            val repo = RateRepository(ExchangeRateApi(apiKey), GoldApiClient(), settingsRepository)

            while (isActive) {
                val settings = settingsRepository.getSettings()
                if (!settings.liveModeEnabled) {
                    stopSelf()
                    return@launch
                }

                try {
                    val cached = settingsRepository.getLastRates()
                    val base = settings.baseCurrency.uppercase()
                    val usdToBase: Double? = if (base == "USD") 1.0 else cached.fxRates["USD"]
                    val lastRates = if (usdToBase == null) {
                        cached
                    } else {
                        repo.fetchMetalsOnly(settings.baseCurrency, usdToBase)
                    }
                    val fxSummary = RateUtils.buildSummary(settings.baseCurrency, lastRates.fxRates, settings.trackedCurrencies)
                    val metalSummary = RateUtils.buildMetalSummary(settings.baseCurrency, lastRates.metalsRates, setOf("XAU", "XAG", "XAU22"))
                    notifier.updateLiveModeNotification("$fxSummary | $metalSummary")
                } catch (_: Exception) {
                    // ignore
                }

                val intervalMinutes = settings.liveModeIntervalMinutes
                delay(intervalMinutes.coerceAtLeast(15).toLong() * 60_000L)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    private fun maybeNotifyThreshold(
        notifier: Notifier,
        settings: com.checkrate.app.data.AppSettings,
        lastRates: com.checkrate.app.data.LastRates
    ) {
        val format = DecimalFormat("0.####")
        val base = settings.baseCurrency
        val allRates = lastRates.fxRates + lastRates.metalsRates

        settings.thresholds.forEach { (code, threshold) ->
            val rate = allRates[code] ?: return@forEach
            if (threshold > 0.0 && rate < threshold) {
                val title = "$code below threshold"
                val unit = if (RateUtils.isMetal(code)) "g" else ""
                val message = "1 ${if (unit.isNotEmpty()) unit + " " else ""}$code = ${format.format(rate)} $base (threshold ${format.format(threshold)})"
                notifier.sendRateAlert(title, message, code.hashCode() and 0x7fffffff)
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 3001
    }
}
