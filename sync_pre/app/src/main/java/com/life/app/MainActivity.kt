package com.life.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.life.app.ui.home.HomeScreen
import com.life.app.ui.theme.LifeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LifeApp()
                }
            }
        }
    }
}

@Composable
fun LifeApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToNewSchedule = { /* TODO 第二阶段：跳 NewSchedulePage */ },
                onNavigateToLifestyle = { /* TODO 第二阶段 */ },
                onNavigateToPreset = { /* TODO 第四阶段 */ },
                onNavigateToScheduleDetail = { _ -> /* TODO 第二阶段 */ },
                onNavigateToConflictMenu = { _ -> /* TODO 第二阶段 */ }
            )
        }
    }
}