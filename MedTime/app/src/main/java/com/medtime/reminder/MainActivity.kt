package com.medtime.reminder

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.medtime.reminder.ui.addmedicine.AddMedicineScreen
import com.medtime.reminder.ui.details.MedicineDetailsScreen
import com.medtime.reminder.ui.history.HistoryScreen
import com.medtime.reminder.ui.home.HomeScreen
import com.medtime.reminder.ui.navigation.Screen
import com.medtime.reminder.ui.settings.SettingsScreen
import com.medtime.reminder.ui.theme.MedTimeTheme
import com.medtime.reminder.util.ThemeMode
import com.medtime.reminder.viewmodel.MedicineViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled via banner state */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val viewModel: MedicineViewModel = viewModel()
            val themeMode by viewModel.prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MedTimeTheme(darkTheme = darkTheme) {
                MedTimeApp(viewModel)
            }
        }
    }
}

@Composable
fun MedTimeApp(viewModel: MedicineViewModel) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showExactAlarmBanner by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            showExactAlarmBanner = !alarmManager.canScheduleExactAlarms()
        }
    }

    val bottomItems = listOf(
        Triple(Screen.Home.route, "Home", Icons.Filled.Home),
        Triple(Screen.History.route, "History", Icons.Filled.List),
        Triple(Screen.Settings.route, "Settings", Icons.Filled.Settings)
    )

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination
            NavigationBar {
                bottomItems.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (showExactAlarmBanner) {
                ExactAlarmBanner(onDismiss = { showExactAlarmBanner = false })
            }
            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) { HomeScreen(navController, viewModel) }
                composable(
                    route = Screen.AddMedicine.route,
                    arguments = listOf(navArgument("medicineId") { type = NavType.LongType; defaultValue = -1L })
                ) { backStackEntry ->
                    val medicineId = backStackEntry.arguments?.getLong("medicineId") ?: -1L
                    AddMedicineScreen(navController, viewModel, medicineId)
                }
                composable(
                    route = Screen.Details.route,
                    arguments = listOf(navArgument("medicineId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val medicineId = backStackEntry.arguments?.getLong("medicineId") ?: -1L
                    MedicineDetailsScreen(navController, viewModel, medicineId)
                }
                composable(Screen.History.route) { HistoryScreen(viewModel) }
                composable(Screen.Settings.route) { SettingsScreen(viewModel) }
            }
        }
    }
}

@Composable
private fun ExactAlarmBanner(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.padding(0.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                "Allow exact alarms so reminders trigger on time.",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
                onDismiss()
            }) { Text("Allow") }
        }
    }
}
