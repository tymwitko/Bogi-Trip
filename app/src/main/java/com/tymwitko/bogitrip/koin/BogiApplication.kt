package com.tymwitko.bogitrip.koin

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.loadKoinModules

class BogiApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin{
            androidContext(this@BogiApplication)
            appModule
        }
        loadKoinModules(appModule)
    }
}