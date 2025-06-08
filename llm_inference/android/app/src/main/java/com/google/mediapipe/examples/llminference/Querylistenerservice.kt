package com.google.mediapipe.examples.llminference

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class QueryListenerService : Service() {

    companion object {
        const val CHANNEL_ID = "QueryListenerServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Listening for queries")
            .setContentText("This phone is standing by for questions.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // or your own icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)  // 💡 Keeps the notification persistent
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // ✅ Tell the system: if this service gets killed, restart it with the same intent
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Query Listener Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
