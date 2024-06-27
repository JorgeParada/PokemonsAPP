package com.example.pokedex.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.work.WorkManager

class AppStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
            // Cancel all WorkManager jobs when the app is closed
            context?.let { ctx ->
                val prefs: SharedPreferences = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("is_app_closed", true).apply()
                WorkManager.getInstance(ctx).cancelAllWork()
            }
        }
    }
}

