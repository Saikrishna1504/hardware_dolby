/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.service

import android.app.ActivityManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import org.lunaris.dolby.DolbyConstants
import org.lunaris.dolby.R
import org.lunaris.dolby.data.AppProfileManager
import org.lunaris.dolby.data.DolbyRepository
import org.lunaris.dolby.utils.ToastHelper

class AppProfileMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val switchHandler = Handler(Looper.getMainLooper())
    private lateinit var appProfileManager: AppProfileManager
    private lateinit var dolbyRepository: DolbyRepository
    private var lastPackageName: String? = null
    private var originalProfile: Int = -1
    private var isMonitoring = false
    private var pendingSwitchRunnable: Runnable? = null

    private val checkForegroundAppRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            if (isMonitoring) {
                handler.postDelayed(this, CHECK_INTERVAL)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appProfileManager = AppProfileManager(this)
        dolbyRepository = DolbyRepository(this)
        originalProfile = dolbyRepository.getCurrentProfile()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true
            DolbyConstants.dlog(TAG, "Started monitoring foreground app")
            handler.post(checkForegroundAppRunnable)
        }
    }

    private fun stopMonitoring() {
        if (isMonitoring) {
            isMonitoring = false
            handler.removeCallbacks(checkForegroundAppRunnable)
            
            pendingSwitchRunnable?.let { switchHandler.removeCallbacks(it) }
            pendingSwitchRunnable = null
            
            if (originalProfile >= 0) {
                dolbyRepository.setCurrentProfile(originalProfile)
            }
            
            DolbyConstants.dlog(TAG, "Stopped monitoring foreground app")
        }
    }

    private fun checkForegroundApp() {
        try {
            val packageName = getForegroundPackage() ?: return
            
            if (packageName == lastPackageName) {
                return
            }
            
            lastPackageName = packageName
            
            pendingSwitchRunnable?.let { switchHandler.removeCallbacks(it) }
            
            pendingSwitchRunnable = Runnable {
                try {
                    val assignedProfile = appProfileManager.getAppProfile(packageName)
                    val prefs = getSharedPreferences("dolby_prefs", Context.MODE_PRIVATE)
                    val showToasts = prefs.getBoolean("app_profile_show_toasts", true)
                    
                    if (assignedProfile >= 0) {
                        DolbyConstants.dlog(TAG, "Switching to profile $assignedProfile for $packageName")
                        dolbyRepository.setCurrentProfile(assignedProfile)
                        
                        if (showToasts) {
                            val profileName = getProfileName(assignedProfile)
                            val appName = getAppName(packageName)
                            ToastHelper.showToast(
                                this@AppProfileMonitorService,
                                "Dolby: $profileName ($appName)"
                            )
                        }
                    } else if (originalProfile >= 0) {
                        DolbyConstants.dlog(TAG, "Restoring original profile $originalProfile for $packageName")
                        dolbyRepository.setCurrentProfile(originalProfile)
                        
                        if (showToasts) {
                            val profileName = getProfileName(originalProfile)
                            ToastHelper.showToast(
                                this@AppProfileMonitorService,
                                "Dolby: $profileName (Default)"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error switching profile", e)
                }
                pendingSwitchRunnable = null
            }
            
            switchHandler.postDelayed(pendingSwitchRunnable!!, SWITCH_DELAY)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking foreground app", e)
        }
    }

    private fun getForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        
        val usageEvents = usageStatsManager.queryEvents(currentTime - 1000, currentTime)
        val event = UsageEvents.Event()
        
        var lastPackage: String? = null
        
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = event.packageName
            }
        }
        
        return lastPackage
    }
    
    private fun getProfileName(profile: Int): String {
        val profiles = resources.getStringArray(R.array.dolby_profile_entries)
        val profileValues = resources.getStringArray(R.array.dolby_profile_values)
        
        return try {
            val index = profileValues.indexOfFirst { it.toInt() == profile }
            if (index >= 0) profiles[index] else "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    companion object {
        private const val TAG = "AppProfileMonitor"
        private const val CHECK_INTERVAL = 2000L
        private const val SWITCH_DELAY = 300L
        
        const val ACTION_START_MONITORING = "org.lunaris.dolby.START_MONITORING"
        const val ACTION_STOP_MONITORING = "org.lunaris.dolby.STOP_MONITORING"

        fun startMonitoring(context: Context) {
            val intent = Intent(context, AppProfileMonitorService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            context.startService(intent)
        }

        fun stopMonitoring(context: Context) {
            val intent = Intent(context, AppProfileMonitorService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }
    }
}
