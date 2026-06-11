package com.baulsanitario

import android.app.Application

class BaulSanitarioApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}

class AppContainer
