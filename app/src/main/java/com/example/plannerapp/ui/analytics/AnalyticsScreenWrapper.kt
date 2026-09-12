package com.example.plannerapp.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.ui.components.AnalyticsSkeleton
import com.example.plannerapp.ui.profile.ProfileViewModel
import com.example.plannerapp.ui.state.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreenWrapper(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiStateResource by viewModel.uiState.collectAsState()

    val uiState = when (val state = uiStateResource) {
        is Resource.Loading -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Analytics & Insights", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                },
                modifier = modifier
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(AppDimens.Space16)
                ) {
                    AnalyticsSkeleton()
                }
            }
            return
        }
        is Resource.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
            return
        }
        is Resource.Success -> state.data
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics & Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.Space16),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stock Price-Style Consistency Graph
            ConsistencyStockChartCard(
                weeklyCheckins = uiState.weeklyCheckins,
                streak = uiState.streak
            )

            Spacer(modifier = Modifier.height(AppDimens.Space24))

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space16)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(AppDimens.CornerCard)
                ) {
                    Column(modifier = Modifier.padding(AppDimens.Space16)) {
                        Text(
                            text = "Streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${uiState.streak} Days",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(AppDimens.CornerCard)
                ) {
                    Column(modifier = Modifier.padding(AppDimens.Space16)) {
                        Text(
                            text = "Consistency",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${uiState.consistencyPercentage}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.Space24))

            // Detailed breakdown dashboard (Bars & Heatmap)
            StatisticsDashboard(weeklyCheckins = uiState.weeklyCheckins)
        }
    }
}
