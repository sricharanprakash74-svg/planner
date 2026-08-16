package com.example.plannerapp

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.plannerapp.data.PlannerDatabase
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.ui.main.MainScreen
import com.example.plannerapp.ui.main.PlannerViewModel
import com.example.plannerapp.ui.main.PlannerViewModelFactory

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          val context = LocalContext.current
          val database = PlannerDatabase.getDatabase(context)
          val repository = PlannerRepository(database.plannerDao())
          val viewModel: PlannerViewModel = viewModel(factory = PlannerViewModelFactory(repository))
          
          MainScreen(
              onItemClick = { navKey -> backStack.add(navKey) },
              modifier = Modifier.safeDrawingPadding().padding(16.dp),
              viewModel = viewModel
          )
        }
      },
  )
}
