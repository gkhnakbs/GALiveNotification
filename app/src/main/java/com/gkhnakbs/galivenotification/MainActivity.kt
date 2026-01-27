package com.gkhnakbs.galivenotification

import android.app.Activity
import android.app.NotificationManager
import android.os.Bundle
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.gkhnakbs.galivenotification.ui.theme.GALiveNotificationTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notificationMan = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        LiveNotificationManager.initialize(this.applicationContext, notificationMan)


        enableEdgeToEdge()
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            val darkIcons = remember {
                mutableStateOf(false)
            }
            ChangeStatusBarIconColor(darkIcons = darkIcons.value)
            LiveNotificationPermission()

            GALiveNotificationTheme {
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
                    LiveNotificationMainScreen(innerPadding, onChangeStatusBarIconColor = {
                        darkIcons.value = !darkIcons.value
                    })
                }
            }
        }
    }
}

@Composable
fun LiveNotificationMainScreen(
    innerPadding: PaddingValues,
    onChangeStatusBarIconColor: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(color = Color.Black)
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalButton(onClick = onChangeStatusBarIconColor) {
            Text(text = "Change Status Bar Icon Color")
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

@Composable
fun ChangeStatusBarIconColor(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            insetsController.isAppearanceLightStatusBars = darkIcons
        }
    }
}