package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Today : Screen("today", "Today")
    data object Medicines : Screen("medicines", "Medicines")
    data object History : Screen("history", "History")
    data object Settings : Screen("settings", "Settings")
    data object AddMedicine : Screen("add_medicine", "Add Medicine")
    data object EditMedicine : Screen("edit_medicine/{medicineId}", "Edit Medicine") {
        fun createRoute(medicineId: Long) = "edit_medicine/$medicineId"
    }
    data object MedicineDetails : Screen("medicine_details/{medicineId}", "Medicine Details") {
        fun createRoute(medicineId: Long) = "medicine_details/$medicineId"
    }
}
