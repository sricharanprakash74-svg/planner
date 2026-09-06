package com.example.plannerapp.sharing

import android.net.Uri
import androidx.compose.runtime.mutableStateOf

object DeepLinkState {
    val pendingDeepLink = mutableStateOf<Uri?>(null)

    fun clear() {
        pendingDeepLink.value = null
    }
}
