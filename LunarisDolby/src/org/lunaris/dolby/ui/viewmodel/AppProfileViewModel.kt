/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.lunaris.dolby.R
import org.lunaris.dolby.data.AppProfileManager
import org.lunaris.dolby.domain.models.AppProfileUiState
import org.lunaris.dolby.utils.ToastHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val appProfileManager = AppProfileManager(application)
    private val context = application

    private val _uiState = MutableStateFlow<AppProfileUiState>(AppProfileUiState.Loading)
    val uiState: StateFlow<AppProfileUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            try {
                _uiState.value = AppProfileUiState.Loading
                val apps = appProfileManager.getInstalledApps()
                val appsWithProfiles = appProfileManager.getAppsWithProfiles()
                
                _uiState.value = AppProfileUiState.Success(
                    apps = apps,
                    appsWithProfiles = appsWithProfiles
                )
            } catch (e: Exception) {
                _uiState.value = AppProfileUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setAppProfile(packageName: String, profile: Int) {
        viewModelScope.launch {
            if (profile == -1) {
                appProfileManager.removeAppProfile(packageName)
                ToastHelper.showToast(context, "Profile reset to default")
            } else {
                appProfileManager.setAppProfile(packageName, profile)
                val profileName = getProfileName(profile)
                ToastHelper.showToast(context, "Profile set to: $profileName")
            }
            loadApps()
        }
    }

    fun removeAppProfile(packageName: String) {
        viewModelScope.launch {
            appProfileManager.removeAppProfile(packageName)
            ToastHelper.showToast(context, "Profile removed")
            loadApps()
        }
    }

    fun clearAllAppProfiles() {
        viewModelScope.launch {
            appProfileManager.clearAllAppProfiles()
            ToastHelper.showToast(context, "All app profiles cleared")
            loadApps()
        }
    }
    
    private fun getProfileName(profile: Int): String {
        val profiles = context.resources.getStringArray(R.array.dolby_profile_entries)
        val profileValues = context.resources.getStringArray(R.array.dolby_profile_values)
        
        return try {
            val index = profileValues.indexOfFirst { it.toInt() == profile }
            if (index >= 0) profiles[index] else "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
