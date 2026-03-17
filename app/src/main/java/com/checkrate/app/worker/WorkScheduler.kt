package com.checkrate.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val WORK_08 = "RATE_CHECK_08"
    private const val WORK_16 = "RATE_CHECK_16"
    private const val WORK_20 = "RATE_CHECK_20"

    fun scheduleAll(context: Context) {
        scheduleDaily(context, 8, 0, false, WORK_08)
        scheduleDaily(context, 16, 0, false, WORK_16)
        scheduleDaily(context, 20, 0, true, WORK_20)
    }

    fun scheduleDaily(context: Context, hour: Int, minute: Int, sendSummary: Boolean, uniqueName: String) {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val delayMillis = next.timeInMillis - now.timeInMillis

        val inputData = Data.Builder()
            .putBoolean(RateCheckWorker.KEY_SEND_SUMMARY, sendSummary)
            .putInt(RateCheckWorker.KEY_HOUR, hour)
            .putInt(RateCheckWorker.KEY_MINUTE, minute)
            .putString(RateCheckWorker.KEY_UNIQUE_NAME, uniqueName)
            .build()

        val request = OneTimeWorkRequestBuilder<RateCheckWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(inputData)
            .addTag("rate_check")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }
}
