package com.laksman.injectorconnect

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val service = AutoClickService.instance
        if (service == null) {
            Toast.makeText(
                this,
                "Accessibility eke \"Injector Connect\" ON karanna",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            val ok = service.startConnect()
            if (!ok) {
                Toast.makeText(this, "HTTP Injector install karala na", Toast.LENGTH_LONG).show()
            }
        }
        finish()
    }
}
