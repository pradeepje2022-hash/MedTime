package com.example.model

enum class MedicineType(
    val displayName: String,
    val iconName: String,
    val defaultUnit: String
) {
    TABLET("Tablet", "pill", "tablet"),
    CAPSULE("Capsule", "capsule", "capsule"),
    SYRUP("Syrup", "bottle", "ml"),
    INJECTION("Injection", "syringe", "injection"),
    DROPS("Drops", "drops", "drops"),
    INHALER("Inhaler", "inhaler", "puff"),
    TOPICAL("Cream/Gel", "cream", "application"),
    OTHER("Other", "other", "dose");

    companion object {
        fun fromString(value: String): MedicineType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: TABLET
        }
    }
}

enum class FrequencyType(val displayName: String) {
    EVERY_DAY("Every day"),
    SPECIFIC_DAYS("Specific days"),
    AS_NEEDED("As needed")
}

enum class MealInstruction(val displayName: String) {
    AFTER_MEAL("After meal"),
    BEFORE_MEAL("Before meal"),
    WITH_MEAL("With meal"),
    EMPTY_STOMACH("On empty stomach"),
    BEDTIME("Before bedtime"),
    NONE("No specific time")
}
