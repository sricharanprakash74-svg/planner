package com.example.plannerapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.plannerapp.notifications.NotificationHelper
import com.example.plannerapp.theme.PlannerAppTheme

class MainActivity : ComponentActivity() {

    // Runtime permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Permission result handled — no blocking UI needed
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the notification channel (idempotent — safe to call every launch)
        NotificationHelper.createChannel(this)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        handleDeepLinkIntent(intent)

        enableEdgeToEdge()
        setContent {
            PlannerAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "plannerapp" && uri.host == "share") {
            com.example.plannerapp.sharing.DeepLinkState.pendingDeepLink.value = uri
        }
    }
}
