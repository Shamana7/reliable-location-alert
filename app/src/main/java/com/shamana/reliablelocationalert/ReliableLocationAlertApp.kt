package com.shamana.reliablelocationalert

import android.app.Application
import com.shamana.reliablelocationalert.core.system.di.AppContainer

class ReliableLocationAlertApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
