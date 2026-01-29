package com.gkhnakbs.galivenotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.ProgressStyle
import androidx.core.graphics.drawable.IconCompat

object LiveNotificationManager {

    private lateinit var notificationManager: NotificationManager

    private lateinit var appContext: Context

    const val CHANNEL_ID = "live_notification_channel_id"
    private const val CHANNEL_NAME = "Live Updates Channel"
    const val NOTIFICATION_ID = 1234567

    fun initialize(cntxt: Context, notifManager: NotificationManager) {
        notificationManager = notifManager
        appContext = cntxt.applicationContext
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Live updates for order tracking"
            enableVibration(false) // Promoted notification'lar için titreşimi kapat
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private enum class CourierState(val delay: Long, val progress: Int?) {
        INITIALIZING(0, null) {
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            override fun buildNotification(): NotificationCompat.Builder {
                return buildBaseNotification(appContext, INITIALIZING)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentText("🛍️ Adım 1/5: Sipariş onaylanıyor...")
                    .setSubText("Onaylanıyor")
                    .setTicker("Siparişiniz alındı!")
                    .setLargeIcon(
                        IconCompat.createWithResource(
                            appContext, R.drawable.shopping_bag
                        ).toIcon(appContext)
                    )
                    .setStyle(buildBaseProgressStyle(INITIALIZING))
            }
        },
        FOOD_PREPARATION(9000, 25) {
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            override fun buildNotification(): NotificationCompat.Builder {
                return buildBaseNotification(appContext, FOOD_PREPARATION)
                    .setContentText("👨‍🍳 Adım 2/5: Hazırlanıyor (%25)")
                    .setSubText("Hazırlanıyor")
                    .setTicker("Siparişiniz hazırlanmaya başlandı!")
                    .setLargeIcon(
                        IconCompat.createWithResource(
                            appContext, R.drawable.cupcake
                        ).toIcon(appContext)
                    )
                    .setStyle(buildBaseProgressStyle(FOOD_PREPARATION).setProgress(25))
            }
        },
        FOOD_ENROUTE(13000, 50) {
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            override fun buildNotification(): NotificationCompat.Builder {
                return buildBaseNotification(appContext, FOOD_ENROUTE)
                    .setContentText("🚴 Adım 3/5: Yolda (%50) - 11 dk kaldı")
                    .setSubText("Yolda")
                    .setTicker("Kurye siparişiniz ile yola çıktı!")
                    .setStyle(buildBaseProgressStyle(FOOD_ENROUTE))
                    .setLargeIcon(
                        IconCompat.createWithResource(
                            appContext, R.drawable.delivery_truck
                        ).toIcon(appContext)
                    )
                    .setWhen(System.currentTimeMillis().plus(11 * 60 * 1000 /* 11 min */))
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
            }
        },
        FOOD_ARRIVING(18000, 75) {
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            override fun buildNotification(): NotificationCompat.Builder {
                return buildBaseNotification(appContext, FOOD_ARRIVING)
                    .setContentText("📦 Adım 4/5: Kapınızda (%75)")
                    .setSubText("Kapıda")
                    .setTicker("Siparişiniz kapınıza bırakıldı!")
                    .setStyle(buildBaseProgressStyle(FOOD_ARRIVING))
                    .setLargeIcon(
                        IconCompat.createWithResource(
                            appContext, R.drawable.check_circle
                        ).toIcon(appContext)
                    )
                    .setWhen(System.currentTimeMillis().plus(5 * 60 * 1000 /* 5 min */))
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
            }
        },
        ORDER_COMPLETE(21000, 100) {
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            override fun buildNotification(): NotificationCompat.Builder {
                return buildBaseNotification(appContext, ORDER_COMPLETE)
                    .apply {
                        progress?.let {
                            setProgress(100, it, false)
                        } ?: run {
                            setProgress(100, 0, true)
                        }
                    }
                    .setContentText("✅ Adım 5/5: Tamamlandı (%100)")
                    .setSubText("Tamamlandı")
                    .setTicker("Siparişiniz başarıyla tamamlandı!")
                    .setStyle(buildBaseProgressStyle(ORDER_COMPLETE))
                    .setLargeIcon(
                        IconCompat.createWithResource(
                            appContext, R.drawable.cupcake
                        ).toIcon(appContext)
                    )
            }
        },
        FINISH(24000, 100) {
            override fun buildNotification(): NotificationCompat.Builder {
                notificationManager.cancel(NOTIFICATION_ID)
                return buildBaseNotification(appContext, FINISH)
            }
        };


        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        fun buildBaseProgressStyle(courierState: CourierState): ProgressStyle {
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

            courierState.progress?.let { progress ->
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
            orderState: CourierState,
        ): NotificationCompat.Builder {
            // Durum çubuğunda gösterilecek kısa özet metin
            val statusBarText = when (orderState) {
                INITIALIZING -> "Onaylanıyor"
                FOOD_PREPARATION -> "Hazırlanıyor"
                FOOD_ENROUTE -> "Yolda"
                FOOD_ARRIVING -> "Kapıda"
                ORDER_COMPLETE -> "Tamamlandı"
                FINISH -> "Sipariş Tamamlandı"
            }

            val notificationBuilder = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setOngoing(true)
                .setRequestPromotedOngoing(true)
                .setShortCriticalText(statusBarText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("JetSnack Sipariş")
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOnlyAlertOnce(true)

            val actionToApp = PendingIntent.getActivity(
                appContext, 111,
                Intent(appContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

            when (orderState) {
                INITIALIZING ->
                    notificationBuilder
                        .addAction(
                            NotificationCompat.Action.Builder(
                                null,
                                "Bildirimleri Aç",
                                actionToApp
                            )
                                .build()
                        )

                FOOD_PREPARATION ->
                    notificationBuilder
                        .addAction(
                            NotificationCompat.Action.Builder(
                                null,
                                "Durumu Görüntüle",
                                actionToApp
                            ).build()
                        )

                FOOD_ENROUTE ->
                    notificationBuilder
                        .addAction(
                            NotificationCompat.Action.Builder(
                                null,
                                "Kuryeyi Takip Et",
                                actionToApp
                            )
                                .build()
                        )
                        .addAction(
                            NotificationCompat.Action.Builder(null, "İletişim", actionToApp)
                                .build()
                        )

                FOOD_ARRIVING ->
                    notificationBuilder
                        .addAction(
                            NotificationCompat.Action.Builder(null, "Teslim Aldım", actionToApp)
                                .build()
                        )
                        .addAction(
                            NotificationCompat.Action.Builder(null, "Bahşiş Ver", actionToApp)
                                .build()
                        )

                ORDER_COMPLETE ->
                    notificationBuilder
                        .addAction(
                            NotificationCompat.Action.Builder(
                                null, "Teslimatı Değerlendir", actionToApp
                            ).build()
                        )
                        .addAction(
                            NotificationCompat.Action.Builder(
                                null, "Tekrar Sipariş Ver", actionToApp
                            ).build()
                        )

                else -> {
                    notificationBuilder.setContentText("Sipariş Tamamlandı")
                }
            }
            return notificationBuilder
        }

        abstract fun buildNotification(): NotificationCompat.Builder
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun getInitialNotification(): Notification {
        return CourierState.INITIALIZING.buildNotification().build()
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun startWithService(
        onScheduleNotification: (Notification, Long) -> Unit,
        onComplete: () -> Unit
    ) {
        CourierState.entries.forEachIndexed { index, state ->
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

            if (index != CourierState.entries.lastIndex) {
                onScheduleNotification(notification, state.delay)
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    notificationManager.cancel(NOTIFICATION_ID)
                    onComplete()
                }, state.delay)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun start() {
        CourierState.entries.forEachIndexed { index, state ->
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

            Handler(Looper.getMainLooper()).postDelayed({
                if (index != CourierState.entries.lastIndex)
                    notificationManager.notify(NOTIFICATION_ID, notification)
                else
                    notificationManager.cancel(NOTIFICATION_ID)
            }, state.delay)
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun isPostPromotionsEnabled(): Boolean {
        return notificationManager.canPostPromotedNotifications()
    }
}