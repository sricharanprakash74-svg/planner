package com.example.plannerapp.ui.profile

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.plannerapp.credits.CreditHubSheet
import com.example.plannerapp.credits.CreditRepository
import com.example.plannerapp.credits.CreditViewModel
import com.example.plannerapp.credits.CreditViewModelFactory
import com.example.plannerapp.data.PlannerDatabase
import com.example.plannerapp.ui.state.Resource
import java.io.File

@Composable
fun ProfileScreenWrapper(
    viewModel: ProfileViewModel,
    onSettingsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onPlanClick: (Long) -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onCreatorMonetizationClick: () -> Unit = {},
    onBecomeCreatorClick: () -> Unit = {},
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

    ProfileScreen(
        user = user,
        avatarBitmap = avatarBitmap,
        userPlans = uiState.userPlans,
        weeklyCheckins = uiState.weeklyCheckins,
        streak = uiState.streak,
        consistencyPercentage = uiState.consistencyPercentage,
        creditBalance = creditBalance,
        freezesCount = freezesCount,
        onPickPhoto = { galleryLauncher.launch("image/*") },
        onSettingsClick = onSettingsClick,
        onAnalyticsClick = onAnalyticsClick,
        onPlanClick = onPlanClick,
        onEditProfileClick = onEditProfileClick,
        onOpenCreditHub = { showCreditHub = true },
        onCreatorMonetizationClick = onCreatorMonetizationClick,
        onBecomeCreatorClick = onBecomeCreatorClick,
        modifier = modifier
    )

    if (showCreditHub) {
        CreditHubSheet(
            viewModel = creditViewModel,
            onDismissRequest = { showCreditHub = false }
        )
    }
}
