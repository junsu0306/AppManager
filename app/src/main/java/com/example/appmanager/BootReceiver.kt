package com.example.appmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted. Starting AppManagerService.")

            // 재부팅 시 AppManagerService 시작
            val serviceIntent = Intent(context, AppManagerService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}

