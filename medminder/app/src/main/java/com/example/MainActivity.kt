package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alarm.AlarmScheduler
import com.example.alarm.AlarmSoundManager
import com.example.alarm.AlarmRingingService
import com.example.model.DoseStatus
import com.example.ui.components.AdMobBanner
import com.example.ui.components.AlarmRingingDialog
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.medicines.AddEditMedicineScreen
import com.example.ui.medicines.AddEditViewModel
import com.example.ui.medicines.MedicineDetailsScreen
import com.example.ui.medicines.MedicineDetailsViewModel
import com.example.ui.medicines.MedicinesScreen
import com.example.ui.medicines.MedicinesViewModel
import com.example.ui.navigation.Screen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            val app = application as MedTimeApplication
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()

            val isDark = when (settings.isDarkMode) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MedTimeAppMain()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val openRinging = intent.getBooleanExtra("OPEN_RINGING_DIALOG", false)
        if (openRinging) {
            val medicineId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1L)
            val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: "Medicine"
            val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_DOSAGE) ?: ""
            val scheduledTime = intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME) ?: ""
            val instruction = intent.getStringExtra(AlarmScheduler.EXTRA_INSTRUCTION) ?: ""
            val notifId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 0)

            if (!AlarmSoundManager.isRinging()) {
                AlarmSoundManager.startAlarm(
                    context = this,
                    medicineInfo = AlarmSoundManager.RingingMedicineInfo(
                        medicineId = medicineId,
                        medicineName = medicineName,
                        dosage = dosage,
                        scheduledTime = scheduledTime,
                        instruction = instruction,
                        type = "Tablet",
                        notificationId = notifId
                    )
                )
            }
        }
    }
}

data class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

@Composable
fun MedTimeAppMain() {
    val context = LocalContext.current
    val app = context.applicationContext as MedTimeApplication
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Active Ringing Alarm state
    val ringingInfo by AlarmSoundManager.currentRingingMedicine.collectAsStateWithLifecycle()

    val navItems = listOf(
        BottomNavItem(
            screen = Screen.Today,
            selectedIcon = Icons.Filled.CalendarToday,
            unselectedIcon = Icons.Outlined.CalendarToday,
            label = "Today"
        ),
        BottomNavItem(
            screen = Screen.Medicines,
            selectedIcon = Icons.Filled.Medication,
            unselectedIcon = Icons.Outlined.Medication,
            label = "Cabinet"
        ),
        BottomNavItem(
            screen = Screen.History,
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History,
            label = "History"
        ),
        BottomNavItem(
            screen = Screen.Settings,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            label = "Settings"
        )
    )

    val showBottomBar = currentRoute in navItems.map { it.screen.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AdMobBanner()
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_item_${item.label.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Today (Home)
            composable(Screen.Today.route) {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAddMedicine = {
                        navController.navigate(Screen.AddMedicine.route)
                    },
                    onNavigateToMedicineDetails = { medicineId ->
                        navController.navigate(Screen.MedicineDetails.createRoute(medicineId))
                    }
                )
            }

            // 2. Cabinet (Medicines List)
            composable(Screen.Medicines.route) {
                val medicinesViewModel: MedicinesViewModel = viewModel()
                MedicinesScreen(
                    viewModel = medicinesViewModel,
                    onNavigateToAddMedicine = {
                        navController.navigate(Screen.AddMedicine.route)
                    },
                    onNavigateToMedicineDetails = { medicineId ->
                        navController.navigate(Screen.MedicineDetails.createRoute(medicineId))
                    }
                )
            }

            // 3. History
            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel()
                HistoryScreen(viewModel = historyViewModel)
            }

            // 4. Settings
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel()
                SettingsScreen(viewModel = settingsViewModel)
            }

            // 5. Add Medicine
            composable(Screen.AddMedicine.route) {
                val addViewModel: AddEditViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return AddEditViewModel(app, null) as T
                        }
                    }
                )
                AddEditMedicineScreen(
                    viewModel = addViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { _ ->
                        navController.popBackStack()
                    }
                )
            }

            // 6. Edit Medicine
            composable(
                route = Screen.EditMedicine.route,
                arguments = listOf(navArgument("medicineId") { type = NavType.LongType })
            ) { backStackEntry ->
                val medId = backStackEntry.arguments?.getLong("medicineId") ?: 0L
                val editViewModel: AddEditViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return AddEditViewModel(app, medId) as T
                        }
                    }
                )
                AddEditMedicineScreen(
                    viewModel = editViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { _ ->
                        navController.popBackStack()
                    }
                )
            }

            // 7. Medicine Details
            composable(
                route = Screen.MedicineDetails.route,
                arguments = listOf(navArgument("medicineId") { type = NavType.LongType })
            ) { backStackEntry ->
                val medId = backStackEntry.arguments?.getLong("medicineId") ?: 0L
                val detailsViewModel: MedicineDetailsViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return MedicineDetailsViewModel(app, medId) as T
                        }
                    }
                )
                MedicineDetailsScreen(
                    viewModel = detailsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(Screen.EditMedicine.createRoute(id))
                    }
                )
            }
        }
    }

    // Active Full-Screen Ringing Dialog
    ringingInfo?.let { info ->
        AlarmRingingDialog(
            info = info,
            onTakeNow = {
                AlarmSoundManager.stopAlarm(context)
                context.stopService(Intent(context, AlarmRingingService::class.java))
                if (info.medicineId > 0) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    CoroutineScope(Dispatchers.IO).launch {
                        app.medicineRepository.recordDoseAction(
                            medicineId = info.medicineId,
                            medicineName = info.medicineName,
                            dosage = info.dosage,
                            dateString = sdf.format(Date()),
                            scheduledTime = info.scheduledTime,
                            status = DoseStatus.TAKEN
                        )
                    }
                }
            },
            onSnooze = { minutes ->
                AlarmSoundManager.stopAlarm(context)
                context.stopService(Intent(context, AlarmRingingService::class.java))
                if (info.medicineId > 0) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    CoroutineScope(Dispatchers.IO).launch {
                        app.medicineRepository.recordDoseAction(
                            medicineId = info.medicineId,
                            medicineName = info.medicineName,
                            dosage = info.dosage,
                            dateString = sdf.format(Date()),
                            scheduledTime = info.scheduledTime,
                            status = DoseStatus.SNOOZED,
                            snoozeUntil = System.currentTimeMillis() + (minutes * 60 * 1000L)
                        )
                    }
                    AlarmScheduler(context).scheduleSnooze(
                        medicineId = info.medicineId,
                        medicineName = info.medicineName,
                        dosage = info.dosage,
                        scheduledTime = info.scheduledTime,
                        instruction = info.instruction,
                        type = info.type,
                        snoozeMinutes = minutes
                    )
                }
            },
            onSkip = {
                AlarmSoundManager.stopAlarm(context)
                context.stopService(Intent(context, AlarmRingingService::class.java))
                if (info.medicineId > 0) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    CoroutineScope(Dispatchers.IO).launch {
                        app.medicineRepository.recordDoseAction(
                            medicineId = info.medicineId,
                            medicineName = info.medicineName,
                            dosage = info.dosage,
                            dateString = sdf.format(Date()),
                            scheduledTime = info.scheduledTime,
                            status = DoseStatus.SKIPPED
                        )
                    }
                }
            },
            onDismiss = {
                AlarmSoundManager.stopAlarm(context)
                context.stopService(Intent(context, AlarmRingingService::class.java))
            }
        )
    }
}
