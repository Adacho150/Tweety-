package com.example.tweety2

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import org.osmdroid.config.Configuration

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(
            applicationContext,
            applicationContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }
}

