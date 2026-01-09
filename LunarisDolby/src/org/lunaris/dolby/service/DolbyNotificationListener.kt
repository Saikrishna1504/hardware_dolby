/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.service

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.lunaris.dolby.DolbyConstants
import org.lunaris.dolby.data.AppProfileManager
import org.lunaris.dolby.data.DolbyRepository

class DolbyNotificationListener : NotificationListenerService() {

    private lateinit var appProfileManager: AppProfileManager
    private lateinit var dolbyRepository: DolbyRepository
    private var lastActivePackage: String? = null

    override fun onCreate() {
        super.onCreate()
        DolbyConstants.dlog(TAG, "NotificationListener created")
        appProfileManager = AppProfileManager(this)
        dolbyRepository = DolbyRepository(this)
        initializeDolbySettings()
        startAppProfileMonitoringIfEnabled()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        DolbyConstants.dlog(TAG, "NotificationListener connected")
        initializeDolbySettings()
        startAppProfileMonitoringIfEnabled()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        DolbyConstants.dlog(TAG, "NotificationListener disconnected")
        requestRebind(android.content.ComponentName(this, DolbyNotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.packageName?.let { packageName ->
            if (packageName != lastActivePackage && packageName != this.packageName) {
                lastActivePackage = packageName
                handlePackageChange(packageName)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    private fun initializeDolbySettings() {
        try {
            val enabled = dolbyRepository.getDolbyEnabled()
            val profile = dolbyRepository.getCurrentProfile()
            if (enabled) {
                dolbyRepository.setDolbyEnabled(true)
                dolbyRepository.setCurrentProfile(profile)
                DolbyConstants.dlog(TAG, "Dolby initialized: enabled=$enabled, profile=$profile")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Dolby settings", e)
        }
    }

    private fun startAppProfileMonitoringIfEnabled() {
        val prefs = getSharedPreferences("dolby_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("app_profile_monitoring_enabled", false)
        if (isEnabled) {
            DolbyConstants.dlog(TAG, "Starting app profile monitoring")
            AppProfileMonitorService.startMonitoring(this)
        }
    }

    private fun handlePackageChange(packageName: String) {
        val prefs = getSharedPreferences("dolby_prefs", MODE_PRIVATE)
        val isMonitoringEnabled = prefs.getBoolean("app_profile_monitoring_enabled", false)
        if (!isMonitoringEnabled) return
        try {
            val assignedProfile = appProfileManager.getAppProfile(packageName)
            if (assignedProfile >= 0) {
                DolbyConstants.dlog(TAG, "Package change detected: $packageName -> profile $assignedProfile")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling package change", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DolbyConstants.dlog(TAG, "NotificationListener destroyed")
    }

    companion object {
        private const val TAG = "DolbyNotificationListener"
    }
}
