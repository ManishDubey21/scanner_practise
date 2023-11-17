package com.example.scanner

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App: Application() {

    // Method that initializes the two channels: channel1 used for notifications from the background service,
    // * channel2 for notifications related to media downloads.

    companion object {
        const val CHANNEL_1_ID = "channel1"
        const val CHANNEL_2_ID = "channel2"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel1 = NotificationChannel(
                CHANNEL_1_ID,
                "Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel1.description = "This is Service Channel"

            val channel2 = NotificationChannel(
                CHANNEL_2_ID,
                "Download",
                NotificationManager.IMPORTANCE_LOW
            )
            channel2.description = "This is Download Channel"

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel1)
            manager.createNotificationChannel(channel2)
        }
    }
}