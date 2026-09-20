package com.example.plannerapp.ui.navigation

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.plannerapp.theme.PhysicsSpec
import java.io.File

enum class BottomNavTab(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    HOME(Icons.Filled.Home, Icons.Outlined.Home, "Today"),
    EXPLORE(Icons.Filled.Explore, Icons.Outlined.Explore, "Explore"),
    PROFILE(Icons.Filled.Person, Icons.Outlined.Person, "Profile");

    val contentDescription: String get() = label
}

@Composable
fun AppBottomNavBar(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    onAddClick: () -> Unit = {},
    avatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val fabInteractionSource = remember { MutableInteractionSource() }
    val isFabPressed by fabInteractionSource.collectIsPressedAsState()
    val fabScale by animateFloatAsState(
        targetValue = if (isFabPressed) 0.92f else 1.0f,
        animationSpec = if (isFabPressed) PhysicsSpec.PressDown else PhysicsSpec.PressRelease,
        label = "fab_scale"
    )

    // Memory-safe decoded local avatar bitmap
    val avatarBitmap = remember(avatarUrl) {
        avatarUrl?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(path, options)
                    var inSampleSize = 1
                    val reqSize = 80
                    if (options.outHeight > reqSize || options.outWidth > reqSize) {
                        val halfH = options.outHeight / 2
                        val halfW = options.outWidth / 2
                        while ((halfH / inSampleSize) >= reqSize && (halfW / inSampleSize) >= reqSize) {
                            inSampleSize *= 2
                        }
                    }
                    val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                    BitmapFactory.decodeFile(path, decodeOptions)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    // Docked minimal container: sits cleanly on the bottom edge with subtle hairline border
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = androidx.compose.ui.graphics.RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Today & Explore
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabItem(
                    selected = currentTab == BottomNavTab.HOME,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    contentDescription = "Today",
                    onClick = { onTabSelected(BottomNavTab.HOME) }
                )

                NavTabItem(
                    selected = currentTab == BottomNavTab.EXPLORE,
                    selectedIcon = Icons.Filled.Explore,
                    unselectedIcon = Icons.Outlined.Explore,
                    contentDescription = "Explore",
                    onClick = { onTabSelected(BottomNavTab.EXPLORE) }
                )
            }

            // Central Creation FAB: 48x48 squircle (radius 14), scale 0.92 on press
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddClick()
                },
                interactionSource = fabInteractionSource,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create Plan",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Right Group: Profile
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                NavTabItem(
                    selected = currentTab == BottomNavTab.PROFILE,
                    selectedIcon = Icons.Filled.Person,
                    unselectedIcon = Icons.Outlined.Person,
                    contentDescription = "Profile",
                    onClick = { onTabSelected(BottomNavTab.PROFILE) },
                    customContent = if (avatarBitmap != null) {
                        {
                            val isProfileSelected = currentTab == BottomNavTab.PROFILE
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isProfileSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                                        else Modifier
                                    )
                            ) {
                                Image(
                                    bitmap = avatarBitmap,
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    customContent: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "navTabBgColor_${contentDescription}"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "navTabIconColor_${contentDescription}"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        modifier = Modifier
            .width(58.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (customContent != null) {
                customContent()
            } else {
                Icon(
                    imageVector = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = contentDescription,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
