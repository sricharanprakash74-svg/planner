package com.example.plannerapp.smartlink.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.plannerapp.smartlink.SmartLinkCache
import com.example.plannerapp.smartlink.SmartLinkMetadata
import com.example.plannerapp.smartlink.SmartLinkParser
import com.example.plannerapp.smartlink.SmartLinkProvider
import com.example.plannerapp.smartlink.SmartLinkResolver

/**
 * Master Smart Link / Embedded Resource Card composable.
 * Instantly displays cached preview or shows a skeleton while resolving metadata asynchronously.
 */
@Composable
fun SmartLinkCard(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SmartLinkCache.init(context)
    }

    var metadata by remember(url) {
        mutableStateOf<SmartLinkMetadata?>(SmartLinkCache.get(url))
    }

    LaunchedEffect(url) {
        if (metadata == null) {
            val resolved = SmartLinkResolver.resolve(url)
            metadata = resolved
        }
    }

    val currentMeta = metadata
    if (currentMeta == null) {
        val domain = remember(url) { SmartLinkParser.extractDomain(url) }
        SmartLinkSkeleton(domain = domain, modifier = modifier)
    } else {
        when (currentMeta.provider) {
            SmartLinkProvider.YOUTUBE -> YouTubeResourceCard(metadata = currentMeta, modifier = modifier)
            SmartLinkProvider.IMAGE -> ImageResourceCard(metadata = currentMeta, modifier = modifier)
            SmartLinkProvider.PDF -> PdfResourceCard(metadata = currentMeta, modifier = modifier)
            SmartLinkProvider.INTERNAL -> InternalResourceCard(metadata = currentMeta, modifier = modifier)
            SmartLinkProvider.WEBPAGE, SmartLinkProvider.UNKNOWN -> WebpageResourceCard(metadata = currentMeta, modifier = modifier)
        }
    }
}
