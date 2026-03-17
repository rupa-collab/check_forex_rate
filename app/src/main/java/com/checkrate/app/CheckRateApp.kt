package com.checkrate.app

import android.app.Application
import com.checkrate.app.data.SettingsRepository
import com.checkrate.app.util.LiveModeController
import com.checkrate.app.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CheckRateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WorkScheduler.scheduleAll(this)

        CoroutineScope(Dispatchers.Default).launch {
            val settings = SettingsRepository(this@CheckRateApp).getSettings()
            if (settings.liveModeEnabled) {
                LiveModeController.start(this@CheckRateApp)
            }
        }
    }
}
