package com.example.plannerapp.ui.profile

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.plannerapp.credits.CreditHubSheet
import com.example.plannerapp.credits.CreditRepository
import com.example.plannerapp.credits.CreditViewModel
import com.example.plannerapp.credits.CreditViewModelFactory
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerDatabase
import com.example.plannerapp.ui.components.PlanListItem
import com.example.plannerapp.ui.state.Resource
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onSettingsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onPlanClick: (Long) -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiStateResource by viewModel.uiState.collectAsState()
    val avatarVersion by viewModel.avatarVersion.collectAsState()

    val uiState = when (val state = uiStateResource) {
        is Resource.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        is Resource.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            return
        }
        is Resource.Success -> state.data
    }

    val user = uiState.user

    // Gallery launcher for selecting profile photo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateProfilePicture(context, it) }
    }

    // Memory-safe decoded local avatar bitmap
    val avatarBitmap = remember(user?.avatarUrl, avatarVersion) {
        user?.avatarUrl?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(path, options)
                    var inSampleSize = 1
                    val reqSize = 300
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

    // Credit ViewModel for credit hub
    var showCreditHub by remember { mutableStateOf(false) }
    val creditViewModel: CreditViewModel = viewModel(
        factory = remember {
            val db = PlannerDatabase.getDatabase(context)
            CreditViewModelFactory(CreditRepository(db.creditDao()), db.userDao())
        }
    )
    val creditBalance by creditViewModel.balanceFlow.collectAsState()
    val freezesCount by creditViewModel.freezesFlow.collectAsState()

    // Tabs: 0 -> Posts, 1 -> Comments, 2 -> Plans (default to Plans so followed plans are immediately visible)
    var selectedTab by remember { mutableIntStateOf(2) }

    // Down sliders state: Following plans open by default per user specification
    var followingExpanded by remember { mutableStateOf(true) }
    var savedPublicExpanded by remember { mutableStateOf(false) }

    val handle = remember(user?.displayName) {
        val raw = user?.displayName ?: "user"
        raw.trim().lowercase().replace("\\s+".toRegex(), "_")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // ── 1. Top Bar: Handle Dropdown + Settings Icon (Instagram Style) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onSettingsClick() }
            ) {
                Text(
                    text = handle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Account Options",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 2. Profile Info Row: Avatar + Stats Columns (Instagram Style) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar with Gallery Picker Badge
            Box(
                modifier = Modifier.size(84.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = user?.displayName?.firstOrNull()?.toString()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Camera Badge to indicate photo selection
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change profile photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 3-Column Stats Row
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStatColumn(
                    value = uiState.userPlans.size.toString(),
                    label = "Plans"
                )
                ProfileStatColumn(
                    value = uiState.streak.toString(),
                    label = "Streak"
                )
                ProfileStatColumn(
                    value = "${uiState.consistencyPercentage}%",
                    label = "Score"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── 3. Name, Account Status & Bio ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = user?.displayName ?: "Guest User",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (user?.cloudUserId != null) user.email ?: "Cloud Account" else "Offline Local Account",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Daily habit builder & consistency tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── 4. Action Buttons Row: Edit Profile & Share Profile ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onEditProfileClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Edit profile",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = {
                    val shareText = "I'm tracking consistency with PlannerApp! Current streak: ${uiState.streak} days, consistency: ${uiState.consistencyPercentage}%."
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val chooser = Intent.createChooser(sendIntent, "Share Profile")
                    context.startActivity(chooser)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Share profile",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── 5. Compact Credits & Streak Freezes Banner ──
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCreditHub = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Paid,
                        contentDescription = "Credits",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$creditBalance Credits",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Streak Freezes",
                        tint = if (freezesCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (freezesCount > 0) "$freezesCount Freezes" else "0 Freezes",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (freezesCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showCreditHub) {
            CreditHubSheet(
                viewModel = creditViewModel,
                onDismissRequest = { showCreditHub = false }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── 6. Analytics Section (In Place of Account Suggestions Carousel) ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Personal Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Consistency & performance curve",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(onClick = onAnalyticsClick) {
                        Text(
                            text = "Full View",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Embedded Stock Chart Card
                ConsistencyStockChartCard(
                    weeklyCheckins = uiState.weeklyCheckins,
                    streak = uiState.streak
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── 7. Reddit-Style Tabs: Posts | Comments | Plans (replacing "About") ──
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Posts",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Comments",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        text = "Plans",
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── 8. Tab Content ──
        when (selectedTab) {
            0 -> {
                // Authentic Empty State for Posts (No mock data)
                EmptySectionState(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    title = "No posts published yet",
                    description = "When you publish updates or share plans with the community, they will be listed here."
                )
            }
            1 -> {
                // Authentic Empty State for Comments (No mock data)
                EmptySectionState(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "No comments yet",
                    description = "Comments and replies on community discussions will show up here."
                )
            }
            2 -> {
                // Plans Tab: Two Down Sliders (Following Plans & Saved/Public Plans)
                PlansTabContent(
                    userPlans = uiState.userPlans,
                    consistencyPercentage = uiState.consistencyPercentage,
                    followingExpanded = followingExpanded,
                    onToggleFollowing = { followingExpanded = !followingExpanded },
                    savedPublicExpanded = savedPublicExpanded,
                    onToggleSavedPublic = { savedPublicExpanded = !savedPublicExpanded },
                    onPlanClick = onPlanClick
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Profile Stat Column Component ───────────────────────────────────────────
@Composable
private fun ProfileStatColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Plans Tab Content with Down Sliders ──────────────────────────────────────
@Composable
private fun PlansTabContent(
    userPlans: List<PlanEntity>,
    consistencyPercentage: Int,
    followingExpanded: Boolean,
    onToggleFollowing: () -> Unit,
    savedPublicExpanded: Boolean,
    onToggleSavedPublic: () -> Unit,
    onPlanClick: (Long) -> Unit
) {
    val followingPlans = remember(userPlans) {
        userPlans.filter { it.sourcePlanId == null }
    }

    val savedAndPublicPlans = remember(userPlans) {
        userPlans.filter { it.sourcePlanId != null || it.isPublic }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Down Slider 1: Following Plans (Default Open) ──
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header (Clickable down slider toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleFollowing)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Following Plans",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${followingPlans.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = if (followingExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (followingExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Slider content (Animated expand/collapse)
                AnimatedVisibility(
                    visible = followingExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (followingPlans.isEmpty()) {
                            Text(
                                text = "You are not following any active plans yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            followingPlans.forEach { plan ->
                                PlanListItem(
                                    heading = plan.heading,
                                    dateRange = "${plan.startDate} – ${plan.endDate}",
                                    consistencyPercent = consistencyPercentage,
                                    onClick = { onPlanClick(plan.planId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Down Slider 2: Saved & Public Plans (Collapsible) ──
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header (Clickable down slider toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleSavedPublic)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Saved & Public Plans",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${savedAndPublicPlans.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = if (savedPublicExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (savedPublicExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Slider content (Animated expand/collapse)
                AnimatedVisibility(
                    visible = savedPublicExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (savedAndPublicPlans.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No saved or public plans yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Plans saved from creators and your publicly shared plans will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            savedAndPublicPlans.forEach { plan ->
                                PlanListItem(
                                    heading = plan.heading,
                                    dateRange = "${plan.startDate} – ${plan.endDate}",
                                    consistencyPercent = consistencyPercentage,
                                    onClick = { onPlanClick(plan.planId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Generic Empty Section State ─────────────────────────────────────────────
@Composable
private fun EmptySectionState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
    }
}

