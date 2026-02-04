package com.gkhnakbs.galivenotification

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class LiveNotificationService : Service() {

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val scheduledRunnables = mutableListOf<Runnable>()
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        LiveNotificationManager.initialize(applicationContext, notificationManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        cancelAllScheduledTasks()

        startForeground(
            LiveNotificationManager.NOTIFICATION_ID,
            LiveNotificationManager.getInitialNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        scheduleRemainingNotifications()

        return START_NOT_STICKY
    }

    private fun scheduleRemainingNotifications() {
        val states = LiveNotificationManager.getOrderStates()

        states.drop(1).forEachIndexed { index, state ->
            val isLastState = index == states.lastIndex - 1

            val runnable = Runnable {
                notificationManager.notify(
                    LiveNotificationManager.NOTIFICATION_ID,
                    LiveNotificationManager.buildNotificationForState(state)
                )

                if (isLastState) {
                    handler.postDelayed({ stopSelf() }, 3000)
                }
            }

            scheduledRunnables.add(runnable)
            handler.postDelayed(runnable, state.delay)
        }
    }

    private fun cancelAllScheduledTasks() {
        scheduledRunnables.forEach { handler.removeCallbacks(it) }
        scheduledRunnables.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAllScheduledTasks()
    }
}
