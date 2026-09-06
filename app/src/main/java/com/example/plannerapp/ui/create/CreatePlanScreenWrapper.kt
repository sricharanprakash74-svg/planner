package com.example.plannerapp.ui.create

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CreatePlanScreenWrapper(
    viewModel: CreatePlanViewModel,
    onClose: () -> Unit,
    onPlanCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    CreatePlanScreen(
        viewModel = viewModel,
        onClose = onClose,
        onPlanCreated = onPlanCreated,
        modifier = modifier
    )
}
