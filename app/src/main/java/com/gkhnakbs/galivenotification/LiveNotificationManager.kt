package com.gkhnakbs.galivenotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.ProgressStyle
import androidx.core.graphics.drawable.IconCompat

object LiveNotificationManager {

    private lateinit var notificationManager: NotificationManager
    private lateinit var appContext: Context

    const val CHANNEL_ID = "live_notification_channel_id"
    const val NOTIFICATION_ID = 1234567

    fun initialize(cntxt: Context, notifManager: NotificationManager) {
        notificationManager = notifManager
        appContext = cntxt.applicationContext
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = appContext.getString(R.string.channel_description)
            setShowBadge(true)
            vibrationEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        }
        notificationManager.createNotificationChannel(channel)
    }

    data class OrderStateData(
        val delay: Long,
        val progress: Int?,
        @param:DrawableRes val iconResId: Int,
        @param:StringRes val contentText: Int,
        @param:StringRes val subText: Int,
        @param:StringRes val ticker: Int
    )

    // Tüm sipariş durumlarını döndür
    fun getOrderStates(): List<OrderStateData> {
        return listOf(
            OrderStateData(
                delay = 0,
                progress = null,
                iconResId = R.drawable.live_notification_prepare,
                contentText = R.string.order_initializing_content,
                subText = R.string.order_status_initializing,
                ticker = R.string.order_initializing_ticker
            ),
            OrderStateData(
                delay = 5000,
                progress = 25,
                iconResId = R.drawable.live_notification_food_pan,
                contentText = R.string.order_preparing_content,
                subText = R.string.order_status_preparing,
                ticker = R.string.order_preparing_ticker
            ),
            OrderStateData(
                delay = 10000,
                progress = 50,
                iconResId = R.drawable.live_notification_courier,
                contentText = R.string.order_enroute_content,
                subText = R.string.order_status_enroute,
                ticker = R.string.order_enroute_ticker
            ),
            OrderStateData(
                delay = 15000,
                progress = 75,
                iconResId = R.drawable.live_notification_door,
                contentText = R.string.order_arriving_content,
                subText = R.string.order_status_arriving,
                ticker = R.string.order_arriving_ticker
            ),
            OrderStateData(
                delay = 20000,
                progress = 100,
                iconResId = R.drawable.live_notification_check,
                contentText = R.string.order_complete_content,
                subText = R.string.order_status_complete,
                ticker = R.string.order_complete_ticker
            )
        )
    }

    fun getInitialNotification(): Notification {
        return buildNotificationForState(getOrderStates().first())
    }

    fun buildNotificationForState(state: OrderStateData): Notification {
        val notificationBuilder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setContentTitle(appContext.getString(R.string.app_name))
            .setShortCriticalText(appContext.getString(state.subText))
            .setContentText(appContext.getString(state.contentText))
            .setTicker(appContext.getString(state.ticker))
            .setSmallIcon(R.mipmap.ic_food_app_launcher)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setLargeIcon(
                IconCompat.createWithResource(appContext, state.iconResId)
                    .toIcon(appContext)
            )
            .apply {
                state.progress?.let {
                    setProgress(100, it, false)
                } ?: run {
                    setProgress(100, 0, true)
                }
            }
            .setStyle(buildProgressStyle(state))

        if (state.progress == 100) {
            val actionToApp = PendingIntent.getActivity(
                appContext, 111,
                Intent(appContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(
                NotificationCompat.Action.Builder(
                    null,
                    appContext.getString(R.string.notification_action_rate_delivery),
                    actionToApp
                ).build()
            )
        }

        return notificationBuilder.build()
    }

    private fun buildProgressStyle(state: OrderStateData): ProgressStyle {
        val pointColor = Color.Blue.toArgb()
        val segmentColor = Color.Yellow.toArgb()
        val completedColor = Color.Green.toArgb()

        val pointList = mutableListOf(
            ProgressStyle.Point(25).setColor(pointColor),
            ProgressStyle.Point(50).setColor(pointColor),
            ProgressStyle.Point(75).setColor(pointColor),
            ProgressStyle.Point(100).setColor(pointColor)
        )

        val segmentList = mutableListOf(
            ProgressStyle.Segment(25).setColor(segmentColor),
            ProgressStyle.Segment(25).setColor(segmentColor),
            ProgressStyle.Segment(25).setColor(segmentColor),
            ProgressStyle.Segment(25).setColor(segmentColor)
        )

        val progressStyle = ProgressStyle()

        state.progress?.let { progress ->
            val currentIndex = (progress / 25) - 1

            for (i in 0..currentIndex) {
                pointList[i] = ProgressStyle.Point((i + 1) * 25).setColor(completedColor)
                segmentList[i] = ProgressStyle.Segment(25).setColor(completedColor)
            }

            progressStyle.setProgress(progress)
                .setProgressPoints(pointList)
                .setProgressSegments(segmentList)
        } ?: run {
            progressStyle.setProgressIndeterminate(true)
        }

        return progressStyle
    }


    fun isPostPromotionsEnabled(): Boolean {
        return notificationManager.canPostPromotedNotifications()
    }

    private val notificationHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }
    private val scheduledRunnables = mutableListOf<Runnable>()

    fun startNotificationWithoutService() {
        cancelScheduledTasks()

        val states = getOrderStates()

        // İlk bildirimi hemen göster
        notificationManager.notify(NOTIFICATION_ID, buildNotificationForState(states.first()))

        // Sonraki bildirimleri zamanla
        states.drop(1).forEachIndexed { index, state ->
            val isLastState = index == states.lastIndex - 1

            val runnable = Runnable {
                notificationManager.notify(NOTIFICATION_ID, buildNotificationForState(state))

                // Son bildirimse, 3 saniye sonra bildirimi kaldır
                if (isLastState) {
                    notificationHandler.postDelayed({ cancelNotification() }, 3000)
                }
            }

            scheduledRunnables.add(runnable)
            notificationHandler.postDelayed(runnable, state.delay)
        }
    }

    fun cancelScheduledTasks() {
        scheduledRunnables.forEach { notificationHandler.removeCallbacks(it) }
        scheduledRunnables.clear()
    }

    fun cancelNotification() {
        cancelScheduledTasks()
        notificationManager.cancel(NOTIFICATION_ID)
    }
}