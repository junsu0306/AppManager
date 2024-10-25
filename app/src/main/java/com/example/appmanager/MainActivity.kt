package com.example.appmanager

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var appSpinner: Spinner
    private lateinit var timeTriggerButton: Button
    private lateinit var locationTriggerButton: Button
    private lateinit var adminButton: Button

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appSpinner = findViewById(R.id.appSpinner)
        timeTriggerButton = findViewById(R.id.timeTriggerButton)
        locationTriggerButton = findViewById(R.id.locationTriggerButton)
        adminButton = findViewById(R.id.adminButton)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        loadInstalledApps()

        timeTriggerButton.setOnClickListener { showTimePickerDialog() }
        locationTriggerButton.setOnClickListener { requestLocationPermission() }
        adminButton.setOnClickListener { requestDeviceAdminPermission() }

        if (checkLocationPermission()) {
            requestBatteryOptimizationException()
            startAppManagerService()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startAppManagerService() {
        val serviceIntent = Intent(this, AppManagerService::class.java)
        startForegroundService(serviceIntent)
    }

    private val deviceAdminLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "기기 관리자 권한이 활성화되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "기기 관리자 권한을 활성화하지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestDeviceAdminPermission() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "기기 관리자 권한을 부여하세요.")
        deviceAdminLauncher.launch(intent)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun loadInstalledApps() {
        val pm: PackageManager = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { it.packageName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, apps)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        appSpinner.adapter = adapter
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
            Toast.makeText(this, "시간 트리거 설정됨: $hourOfDay:$minute", Toast.LENGTH_SHORT).show()
            setAppLaunchTrigger(hourOfDay, minute)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        timePickerDialog.show()
    }

    private fun setAppLaunchTrigger(hour: Int, minute: Int) {
        val selectedAppPackage = appSpinner.selectedItem as String
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        val triggerTime = calendar.timeInMillis

        if (triggerTime <= currentTime) {
            Toast.makeText(this, "트리거 시간이 현재 시간보다 늦어야 합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                launchApp(selectedAppPackage)
            }
        }, triggerTime - currentTime)
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "앱을 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermission() {
        if (!checkLocationPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            setLocationTrigger()
            startAppManagerService()
        }
    }

    private fun requestBatteryOptimizationException() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "배터리 최적화 예외 설정을 해주세요.", Toast.LENGTH_SHORT).show()
    }

    private fun setLocationTrigger() {
        Toast.makeText(this, "위치 트리거가 설정되었습니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setLocationTrigger()
            requestBatteryOptimizationException()
            startAppManagerService()
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }
}


