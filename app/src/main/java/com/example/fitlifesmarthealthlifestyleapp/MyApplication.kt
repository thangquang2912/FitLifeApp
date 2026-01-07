package com.example.fitlifesmarthealthlifestyleapp

import android.app.Application
import com.cloudinary.android.MediaManager
import com.example.fitlifesmarthealthlifestyleapp.BuildConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config: HashMap<String, String> = HashMap()

        config["cloud_name"] = BuildConfig.CLOUD_NAME
        config["api_key"] = BuildConfig.API_KEY
        config["api_secret"] = BuildConfig.API_SECRET

        MediaManager.init(this, config)
    }
}