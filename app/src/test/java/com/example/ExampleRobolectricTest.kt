package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Medicine
import com.example.model.MedicineType
import com.example.model.ReminderTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MedTime", appName)
    }

    @Test
    fun `medicine is scheduled for today verification`() {
        val medicine = Medicine(
            name = "Paracetamol",
            dosageAmount = "1",
            dosageUnit = "tablet",
            medicineType = MedicineType.TABLET,
            reminderTimes = listOf(ReminderTime(8, 0, "Morning"))
        )
        val today = Calendar.getInstance()
        assertTrue(medicine.isScheduledForDay(today))
    }
}
