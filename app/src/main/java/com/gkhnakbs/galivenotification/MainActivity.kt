package com.gkhnakbs.galivenotification

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gkhnakbs.galivenotification.ui.theme.GALiveNotificationTheme
import com.gkhnakbs.galivenotification.ui.theme.LocalStatusBarController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notificationMan = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        LiveNotificationManager.initialize(this.applicationContext, notificationMan)


        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            val orderPlacedMessage = stringResource(R.string.order_placed)

            LiveNotificationPermission()

            GALiveNotificationTheme(
                activity = this
            ) {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackbarHostState)
                    },
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(text = stringResource(R.string.app_title))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black,
                                titleContentColor = Color.White
                            )
                        )
                    }
                ) { innerPadding ->
                    LiveNotificationMainScreen(
                        innerPadding,
                        onStartNotification = {
                            this@MainActivity.onCheckout()
                            scope.launch {
                                snackbarHostState.showSnackbar(orderPlacedMessage)
                            }
                        })
                }
            }
        }
    }
}

@Composable
fun LiveNotificationMainScreen(
    innerPadding: PaddingValues,
    onStartNotification: () -> Unit,
) {
    val statusBarController = LocalStatusBarController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black)
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalButton(onClick = {
            statusBarController?.setStatusBarIconColors(false)
        }) {
            Text(text = stringResource(R.string.change_status_bar_icon_color))
        }
        FilledTonalButton(onClick = onStartNotification) {
            Text(text = stringResource(R.string.send_notification))
        }
        NotificationPostPromotedPermission()
    }
}

@Composable
fun NotificationPostPromotedPermission() {
    val context = LocalContext.current
    val promotedNotificationsLabel = stringResource(R.string.promoted_notifications_label)
    val liveUpdatesLabel = stringResource(R.string.live_updates_label)
    val appName = stringResource(R.string.app_name)
    val toastOpen = stringResource(R.string.promoted_settings_toast_open, liveUpdatesLabel)
    val toastFindLiveUpdates =
        stringResource(R.string.promoted_settings_toast_find_live_updates, liveUpdatesLabel)
    val toastManual =
        stringResource(R.string.promoted_settings_toast_manual, appName, liveUpdatesLabel)
    var isPostPromotionsEnabled by remember { mutableStateOf(LiveNotificationManager.isPostPromotionsEnabled()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isPostPromotionsEnabled = LiveNotificationManager.isPostPromotionsEnabled()
    }
    if (!isPostPromotionsEnabled) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.promoted_notifications_disabled_title),
                modifier = Modifier.padding(horizontal = 10.dp),
                color = Color.Yellow
            )
            Text(
                text = stringResource(
                    R.string.promoted_notifications_permission_hint,
                    promotedNotificationsLabel
                ),
                modifier = Modifier.padding(horizontal = 10.dp),
                color = Color.White.copy(alpha = 0.7f)
            )
            Button(
                onClick = {
                    try {
                        // Yöntem 1: ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS (Android 15+)
                        val promotedIntent =
                            Intent("android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS").apply {
                                putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }

                        // Intent'in handle edilebileceğini kontrol et
                        if (promotedIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(promotedIntent)
                            Toast.makeText(
                                context,
                                toastOpen,
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            throw Exception("Promoted settings not available")
                        }
                    } catch (_: Exception) {
                        try {
                            // Yöntem 2: Normal bildirim ayarları
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            Toast.makeText(
                                context,
                                toastFindLiveUpdates,
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (_: Exception) {
                            // Yöntem 3: Genel ayarlar
                            val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(fallbackIntent)
                            Toast.makeText(
                                context,
                                toastManual,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
            ) {
                Text(text = stringResource(R.string.open_settings_button))
            }

            // Manuel aktifleştirme talimatları
            Text(
                text = stringResource(
                    R.string.promoted_manual_instructions,
                    appName,
                    liveUpdatesLabel
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                color = Color.White.copy(alpha = 0.5f),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun LiveNotificationPermission() {
    val context = LocalContext.current
    val permissionGrantedText = stringResource(R.string.permission_granted)
    val permissionDeniedText = stringResource(R.string.permission_denied)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        if (it) {
            Toast.makeText(
                context,
                permissionGrantedText,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                permissionDeniedText,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(permissionLauncher) {
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}

fun Context.onCheckout() {
    // Servisi başlat (zaten çalışıyorsa onStartCommand tekrar çağrılır)
    val serviceIntent = Intent(this, LiveNotificationService::class.java)
    startForegroundService(serviceIntent)
}
