/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val assignedProfile: Int = -1
)

class AppProfileManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("app_profiles", Context.MODE_PRIVATE)
    private val packageManager = context.packageManager
    
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                packageManager.getLaunchIntentForPackage(app.packageName) != null
            }
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    appName = app.loadLabel(packageManager).toString(),
                    icon = app.loadIcon(packageManager),
                    assignedProfile = getAppProfile(app.packageName)
                )
            }
            .sortedBy { it.appName }
        
        apps
    }
    
    fun getAppProfile(packageName: String): Int {
        return prefs.getInt(packageName, -1)
    }
    
    fun setAppProfile(packageName: String, profile: Int) {
        prefs.edit().putInt(packageName, profile).apply()
    }
    
    fun removeAppProfile(packageName: String) {
        prefs.edit().remove(packageName).apply()
    }
    
    fun getAppsWithProfiles(): Map<String, Int> {
        return prefs.all.mapNotNull { (key, value) ->
            if (value is Int) key to value else null
        }.toMap()
    }
    
    fun clearAllAppProfiles() {
        prefs.edit().clear().apply()
    }
}
