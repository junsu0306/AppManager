package com.example.appmanager

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var appSpinner: Spinner
    private lateinit var timeTriggerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appSpinner = findViewById(R.id.appSpinner)
        timeTriggerButton = findViewById(R.id.timeTriggerButton)

        // 알림 권한 체크 및 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        // 앱 초기화
        loadInstalledApps()
        timeTriggerButton.setOnClickListener { showTimePickerDialog() }

        // 포그라운드 서비스 시작
        startAppManagerService()
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) { // 알림 권한 결과
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 앱 목록 로드
    private fun loadInstalledApps() {
        val pm: PackageManager = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { it.packageName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, apps)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        appSpinner.adapter = adapter
    }

    // 시간 선택 다이얼로그 표시 및 알람 설정
    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
            Toast.makeText(this, "시간 트리거 설정됨: $hourOfDay:$minute", Toast.LENGTH_SHORT).show()
            setAppLaunchTrigger(hourOfDay, minute)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        timePickerDialog.show()
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun setAppLaunchTrigger(hour: Int, minute: Int) {
        val selectedAppPackage = appSpinner.selectedItem as String
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1) // 트리거 시간이 이미 지났다면 다음 날로 설정
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("appPackage", selectedAppPackage)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        Log.d("MainActivity", "Alarm set for: ${calendar.time}")

        // 예약 내역 알림 표시
        showNotification(selectedAppPackage, calendar)
    }

    // 알림 표시
    private fun showNotification(appPackage: String, calendar: Calendar) {
        val channelId = "app_trigger_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 이상에서는 알림 채널이 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Trigger Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val formattedTime = "${calendar.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", calendar.get(Calendar.MINUTE))}"
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle("예약된 앱 트리거")
            .setContentText("앱: $appPackage, 예약 시간: $formattedTime")
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    // 포그라운드 서비스 시작
    private fun startAppManagerService() {
        val serviceIntent = Intent(this, AppManagerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}






