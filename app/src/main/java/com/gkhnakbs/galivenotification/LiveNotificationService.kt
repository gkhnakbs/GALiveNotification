package com.gkhnakbs.galivenotification

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class LiveNotificationService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val runnables = mutableListOf<Runnable>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // LiveNotificationManager'ı initialize et
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        LiveNotificationManager.initialize(applicationContext, notificationManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotification = LiveNotificationManager.getInitialNotification()

        startForeground(
            LiveNotificationManager.NOTIFICATION_ID,
            initialNotification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        // Bildirimleri başlat
        LiveNotificationManager.startWithService(
            onScheduleNotification = { notification, delay ->
                val runnable = Runnable {
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(LiveNotificationManager.NOTIFICATION_ID, notification)
                }
                runnables.add(runnable)
                handler.postDelayed(runnable, delay)
            },
            onComplete = {
                stopSelf()
            }
        )

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Tüm bekleyen handler'ları temizle
        runnables.forEach { handler.removeCallbacks(it) }
        runnables.clear()
    }
}
