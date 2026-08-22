package com.medtime.reminder.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddMedicine : Screen("add_medicine?medicineId={medicineId}") {
        fun create(medicineId: Long = -1L) = "add_medicine?medicineId=$medicineId"
    }
    data object Details : Screen("details/{medicineId}") {
        fun create(medicineId: Long) = "details/$medicineId"
    }
    data object History : Screen("history")
    data object Settings : Screen("settings")
}
