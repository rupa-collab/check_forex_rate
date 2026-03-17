package com.checkrate.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.checkrate.app.BuildConfig
import com.checkrate.app.data.ExchangeRateApi
import com.checkrate.app.data.GoldApiClient
import com.checkrate.app.data.RateRepository
import com.checkrate.app.data.SettingsRepository
import com.checkrate.app.notifications.Notifier
import com.checkrate.app.util.RateUtils
import java.text.DecimalFormat

class RateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Automatic FX polling disabled to preserve exchange rate.host quota.\r
        return Result.success()
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
        const val KEY_SEND_SUMMARY = "send_summary"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        const val KEY_UNIQUE_NAME = "unique_name"
    }
}
