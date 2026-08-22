package com.medtime.reminder

import android.app.Application
import com.medtime.reminder.alarm.AlarmScheduler
import com.medtime.reminder.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedTimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        // Safety net: re-verify all alarms are scheduled correctly whenever the app process starts.
        CoroutineScope(Dispatchers.IO).launch {
            AlarmScheduler.rescheduleAll(this@MedTimeApp)
        }
    }
}
