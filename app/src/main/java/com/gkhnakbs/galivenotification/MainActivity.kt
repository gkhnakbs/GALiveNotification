package com.gkhnakbs.galivenotification

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gkhnakbs.galivenotification.ui.theme.GALiveNotificationTheme
import com.gkhnakbs.galivenotification.ui.theme.LocalStatusBarController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
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
                                Text(text = "GALiveNotification")
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
                                snackbarHostState.showSnackbar("Order placed")
                            }
                        })
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@Composable
fun LiveNotificationMainScreen(
    innerPadding: PaddingValues,
    onStartNotification: () -> Unit,
) {
    val statusBarController = LocalStatusBarController.current

    Column(
        modifier = Modifier
            .background(color = Color.Black)
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalButton(onClick = {
            statusBarController?.setStatusBarIconColors(false)
        }) {
            Text(text = "Change Status Bar Icon Color")
        }
        FilledTonalButton(onClick = onStartNotification) {
            Text(text = "Send Notification")
        }
        NotificationPostPromotedPermission()
    }
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@Composable
fun NotificationPostPromotedPermission() {
    val context = LocalContext.current
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
                text = "⚠️ Promoted Notifications Devre Dışı",
                modifier = Modifier.padding(horizontal = 10.dp),
                color = Color.Yellow
            )
            Text(
                text = "Live Notification özelliğinin tam çalışması için 'Promoted Notifications' iznini açmalısınız.",
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
                                "✅ 'Promoted Notifications' ayarını açın",
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
                                "📱 Bildirim ayarlarında 'Promoted Notifications' seçeneğini bulun ve açın",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (_: Exception) {
                            // Yöntem 3: Genel ayarlar
                            val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(fallbackIntent)
                            Toast.makeText(
                                context,
                                "⚠️ Manuel olarak: Ayarlar > Bildirimler > GALiveNotification > Promoted Notifications",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
            ) {
                Text(text = "⚙️ Ayarlara Git")
            }

            // Manuel aktifleştirme talimatları
            Text(
                text = "📝 Manuel Aktifleştirme:\n" +
                        "1. Ayarlar > Bildirimler > GALiveNotification\n" +
                        "2. 'Promoted Notifications' seçeneğini açın\n" +
                        "3. Uygulamaya geri dönün",
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        if (it) {
            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(permissionLauncher) {
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
fun Context.onCheckout() {
    // Foreground Service'i başlat
    val serviceIntent = Intent(this, LiveNotificationService::class.java)
    startForegroundService(serviceIntent)
}

