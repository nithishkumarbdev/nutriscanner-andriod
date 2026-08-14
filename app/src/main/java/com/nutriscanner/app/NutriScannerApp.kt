package com.nutriscanner.app

import android.app.Application
import com.google.firebase.FirebaseApp

class NutriScannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
