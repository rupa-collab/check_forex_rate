package com.checkrate.app.util

import android.content.Context
import android.content.Intent
import android.os.Build
import com.checkrate.app.worker.LiveRateService

object LiveModeController {
    fun start(context: Context) {
        val intent = Intent(context, LiveRateService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        val intent = Intent(context, LiveRateService::class.java)
        context.stopService(intent)
    }
}