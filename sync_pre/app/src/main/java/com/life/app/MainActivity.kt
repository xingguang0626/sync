package com.life.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.life.app.ui.diary.DiaryScreen
import com.life.app.ui.home.HomeScreen
import com.life.app.ui.home.components.HomeTab
import com.life.app.ui.home.components.NluDraftPreview
import com.life.app.ui.mine.LifestyleScreen
import com.life.app.ui.mine.MineScreen
import com.life.app.ui.newschedule.NewScheduleScreen
import com.life.app.ui.plan.PlanScreen
import com.life.app.ui.scheduledetail.ScheduleDetailScreen
import com.life.app.ui.stats.StatsScreen
import com.life.app.ui.theme.SyncTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SyncApp()
                }
            }
        }
    }
}

@Composable
fun SyncApp() {
    val navController = rememberNavController()
    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentTab = HomeTab.entries.getOrElse(currentTabIndex) { HomeTab.HOME }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // ── 首页 ──
        composable("home") {
            HomeScreen(
                onNavigateToNewSchedule = { draft: NluDraftPreview? ->
                    if (draft != null) {
                        val title = Uri.encode(draft.title)
                        navController.navigate(
                            "new_schedule?nluTitle=$title&nluDate=${draft.date}" +
                            "&nluStartTime=${draft.startTime}&nluEndTime=${draft.endTime}" +
                            "&nluPriority=${draft.priority}"
                        )
                    } else {
                        navController.navigate("new_schedule")
                    }
                },
                onNavigateToLifestyle = { navController.navigate("lifestyle") },
                onNavigateToPreset = { navController.navigate("preset_select") },
                onNavigateToScheduleDetail = { id -> navController.navigate("schedule_detail/$id") },
                onTabSelected = { tab ->
                    currentTabIndex = tab.ordinal
                    navController.navigate(tab.route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                currentTab = currentTab
            )
        }

        // ── 计划 Tab ──
        composable("plan") {
            TabPageScaffold(
                currentTab = HomeTab.PLAN,
                onTabSelected = { tab ->
                    currentTabIndex = tab.ordinal
                    navController.navigate(tab.route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                PlanScreen()
            }
        }

        // ── 统计 Tab ──
        composable("stats") {
            TabPageScaffold(
                currentTab = HomeTab.STATS,
                onTabSelected = { tab ->
                    currentTabIndex = tab.ordinal
                    navController.navigate(tab.route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                StatsScreen()
            }
        }

        // ── 日记 Tab ──
        composable("diary") {
            TabPageScaffold(
                currentTab = HomeTab.DIARY,
                onTabSelected = { tab ->
                    currentTabIndex = tab.ordinal
                    navController.navigate(tab.route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                DiaryScreen()
            }
        }

        // ── 我的 Tab ──
        composable("me") {
            TabPageScaffold(
                currentTab = HomeTab.ME,
                onTabSelected = { tab ->
                    currentTabIndex = tab.ordinal
                    navController.navigate(tab.route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                MineScreen(
                    onNavigateHome = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }
        }

        // ── 新建日程（可携带 NLU 预填数据） ──
        composable(
            route = "new_schedule?nluTitle={nluTitle}&nluDate={nluDate}&nluStartTime={nluStartTime}&nluEndTime={nluEndTime}&nluPriority={nluPriority}",
            arguments = listOf(
                navArgument("nluTitle") { type = NavType.StringType; defaultValue = "" },
                navArgument("nluDate") { type = NavType.StringType; defaultValue = "" },
                navArgument("nluStartTime") { type = NavType.StringType; defaultValue = "" },
                navArgument("nluEndTime") { type = NavType.StringType; defaultValue = "" },
                navArgument("nluPriority") { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            NewScheduleScreen(
                onNavigateBack = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        // ── 编辑日程 ──
        composable(
            route = "edit_schedule/{scheduleId}",
            arguments = listOf(navArgument("scheduleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getLong("scheduleId") ?: return@composable
            NewScheduleScreen(
                scheduleId = scheduleId,
                onNavigateBack = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        // ── 日程详情 ──
        composable(
            route = "schedule_detail/{scheduleId}",
            arguments = listOf(navArgument("scheduleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getLong("scheduleId") ?: return@composable
            ScheduleDetailScreen(
                scheduleId = scheduleId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("edit_schedule/$id") }
            )
        }

        // ── 生活管家 ──
        composable("lifestyle") {
            LifestyleScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── 预设模式选择 ──
        composable("preset_select") {
            com.life.app.ui.modes.ModeSelectScreen(
                onNavigateBack = { navController.popBackStack() },
                onModeSelected = { mode ->
                    navController.navigate("preset_timer/${mode.name}")
                }
            )
        }

        // ── 预设模式倒计时 ──
        composable(
            route = "preset_timer/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = try {
                com.life.app.ui.modes.ModeType.valueOf(
                    backStackEntry.arguments?.getString("mode") ?: ""
                )
            } catch (_: Exception) { com.life.app.ui.modes.ModeType.STUDY }
            com.life.app.ui.modes.ModeTimerScreen(
                mode = mode,
                onEnd = { navController.navigate("preset_feedback/${mode.name}") },
                onCancel = { navController.popBackStack("preset_select", false) }
            )
        }

        // ── 预设模式反馈 ──
        composable(
            route = "preset_feedback/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = try {
                com.life.app.ui.modes.ModeType.valueOf(
                    backStackEntry.arguments?.getString("mode") ?: ""
                )
            } catch (_: Exception) { com.life.app.ui.modes.ModeType.STUDY }
            com.life.app.ui.modes.ModeFeedbackScreen(
                mode = mode,
                onBackToHome = { navController.popBackStack("home", false) }
            )
        }

    }
}

private val HomeTab.route: String get() = when (this) {
    HomeTab.HOME -> "home"
    HomeTab.PLAN -> "plan"
    HomeTab.STATS -> "stats"
    HomeTab.DIARY -> "diary"
    HomeTab.ME -> "me"
}

@Composable
private fun TabPageScaffold(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            com.life.app.ui.home.components.BottomTabBar(
                current = currentTab,
                onTabSelected = onTabSelected
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            content()
        }
    }
}
