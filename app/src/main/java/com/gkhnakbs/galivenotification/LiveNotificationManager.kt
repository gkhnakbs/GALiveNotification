package com.gkhnakbs.galivenotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.util.Log
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

    // Önceki completion handler'ı iptal etmek için
    private var completionRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

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

    private enum class OrderState(
        val delay: Long,
        val progress: Int?,
        @param:DrawableRes val iconResId: Int,
        @param:StringRes val contentText: Int,
        @param:StringRes val subText: Int,
        @param:StringRes val ticker: Int,
    ) {
        INITIALIZING(
            0,
            null,
            iconResId = R.drawable.live_notification_prepare,
            contentText = R.string.order_initializing_content,
            subText = R.string.order_status_initializing,
            ticker = R.string.order_initializing_ticker,
        ),
        FOOD_PREPARATION(
            5000, 25, iconResId = R.drawable.live_notification_food_pan,
            contentText = R.string.order_preparing_content,
            subText = R.string.order_status_preparing,
            ticker = R.string.order_preparing_ticker
        ),
        FOOD_ENROUTE(
            10000, 50, iconResId = R.drawable.live_notification_courier,
            contentText = R.string.order_enroute_content,
            subText = R.string.order_status_enroute,
            ticker = R.string.order_enroute_ticker
        ),
        FOOD_ARRIVING(
            15000, 75, iconResId = R.drawable.live_notification_door,
            contentText = R.string.order_arriving_content,
            subText = R.string.order_status_arriving,
            ticker = R.string.order_arriving_ticker
        ),
        ORDER_COMPLETE(
            20000, 100, iconResId = R.drawable.live_notification_check,
            contentText = R.string.order_complete_content,
            subText = R.string.order_status_complete,
            ticker = R.string.order_complete_ticker
        );

        fun buildNotification() : NotificationCompat.Builder{
            return buildBaseNotification(appContext, this)
        }


        fun buildBaseProgressStyle(orderState: OrderState): ProgressStyle {
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

            orderState.progress?.let { progress ->
                val currentIndex = (progress / 25) - 1

                // Geçilen segmentlerin rengini değiştir
                for (i in 0..currentIndex) {
                    pointList[i] = ProgressStyle.Point((i + 1) * 25).setColor(completedColor)
                    segmentList[i] = ProgressStyle.Segment(25).setColor(completedColor)
                }

                progressStyle.setProgress(progress).setProgressPoints(pointList)
                    .setProgressSegments(segmentList)
            } ?: run {
                progressStyle.setProgressIndeterminate(true)
            }
            return progressStyle
        }

        fun buildBaseNotification(
            appContext: Context,
            orderState: OrderState,
        ): NotificationCompat.Builder {
            val notificationBuilder = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setOngoing(true)
                .setRequestPromotedOngoing(true)
                .setContentTitle(appContext.getString(R.string.app_name))
                .setShortCriticalText(appContext.getString(orderState.subText))
                .setContentText(appContext.getString(orderState.contentText))
                .setTicker(appContext.getString(orderState.ticker))
                .setSmallIcon(R.mipmap.ic_food_app_launcher)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOnlyAlertOnce(true)
                .setLargeIcon(
                    IconCompat.createWithResource(
                        LiveNotificationManager.appContext, orderState.iconResId
                    ).toIcon(LiveNotificationManager.appContext)
                ).apply {
                    orderState.progress?.let {
                        setProgress(100, it, false)
                    } ?: run {
                        setProgress(100, 0, true)
                    }
                }
                .setStyle(buildBaseProgressStyle(orderState))

            val actionToApp = PendingIntent.getActivity(
                appContext, 111,
                Intent(appContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

            when (orderState) {
                ORDER_COMPLETE ->
                    notificationBuilder
                        .addAction(
                            NotificationCompat.Action.Builder(
                                null,
                                appContext.getString(R.string.notification_action_rate_delivery),
                                actionToApp
                            ).build()
                        )

                else -> {}
            }
            return notificationBuilder
        }

    }

    fun getInitialNotification(): Notification {
        return OrderState.INITIALIZING.buildNotification().build()
    }

    fun startWithService(
        onScheduleNotification: (Notification, Long) -> Unit,
        onComplete: () -> Unit,
    ) {
        // Önceki completion handler varsa iptal et
        completionRunnable?.let { mainHandler.removeCallbacks(it) }

        OrderState.entries.forEach { state ->
            val notification = state.buildNotification().build()

            Log.d("LiveNotificationManager", "Notification built: $notification")
            Log.d(
                "LiveNotificationManager",
                "canPostPromotedNotifications: ${isPostPromotionsEnabled()}"
            )
            Log.d(
                "LiveNotificationManager",
                "hasPromotableCharacteristics: ${notification.hasPromotableCharacteristics()}"
            )

            onScheduleNotification(notification, state.delay)
        }

        // Tüm bildirimler gösterildikten sonra servisi durdur
        val lastDelay = OrderState.entries.last().delay + 3000 // Son bildirimden 3 saniye sonra
        completionRunnable = Runnable { onComplete() }
        mainHandler.postDelayed(completionRunnable!!, lastDelay)
    }

    fun isPostPromotionsEnabled(): Boolean {
        return notificationManager.canPostPromotedNotifications()
    }
}