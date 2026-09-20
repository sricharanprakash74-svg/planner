package com.example.plannerapp.smartlink

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.example.plannerapp.sharing.DeepLinkState

/**
 * Safely opens links using Chrome Custom Tabs, native deep links, or standard browser Intents.
 */
object SmartLinkOpener {

    fun openUrl(context: Context, url: String) {
        try {
            val uri = Uri.parse(url)

            // 1. Handle native internal deep links
            if (uri.scheme == "plannerapp" || uri.host == "plannerapp.com") {
                DeepLinkState.pendingDeepLink.value = uri
                return
            }

            // 2. Open via Chrome Custom Tabs (safe, sandboxed, user-friendly)
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()

            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            // 3. Fallback to standard ACTION_VIEW
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (err: Exception) {
                // Ignore if no browser available
            }
        }
    }
}
