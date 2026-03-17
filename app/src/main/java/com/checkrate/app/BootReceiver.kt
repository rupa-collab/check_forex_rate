package com.checkrate.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.checkrate.app.data.SettingsRepository
import com.checkrate.app.util.LiveModeController
import com.checkrate.app.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WorkScheduler.scheduleAll(context)
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val settings = SettingsRepository(context).getSettings()
                    if (settings.liveModeEnabled) {
                        LiveModeController.start(context)
                    }
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
