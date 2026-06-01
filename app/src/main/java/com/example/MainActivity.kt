package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.data.SettingsManager
import com.example.ui.ExpenseViewModel
import com.example.ui.ExpenseViewModelFactory
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

        // Offline data infrastructure instantiation
    val database = AppDatabase.getDatabase(this)
    val repository = ExpenseRepository(database.expenseDao(), database.userDao())
    val settingsManager = SettingsManager(this)

    // Construct common ViewModel
    val viewModelFactory = ExpenseViewModelFactory(
      application = application,
      repository = repository,
      settingsManager = settingsManager
    )
    val viewModel = ViewModelProvider(this, viewModelFactory)[ExpenseViewModel::class.java]

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val navController = rememberNavController()
          val startDest = if (settingsManager.currentUserEmail.value != null) "dashboard" else "auth"

          NavHost(
            navController = navController,
            startDestination = startDest
          ) {
            composable("auth") {
              AuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                  navController.navigate("dashboard") {
                    popUpTo("auth") { inclusive = true }
                  }
                }
              )
            }
            composable("dashboard") {
              DashboardScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToScanner = { navController.navigate("scanner") }
              )
            }
            composable("settings") {
              SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onLoggedOut = {
                  navController.navigate("auth") {
                    popUpTo(0) { inclusive = true }
                  }
                }
              )
            }
            composable("scanner") {
              CameraScannerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
              )
            }
          }
        }
      }
    }
  }
}
