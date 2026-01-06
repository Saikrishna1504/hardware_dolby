/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.lunaris.dolby.R
import org.lunaris.dolby.data.DolbyRepository
import org.lunaris.dolby.domain.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DolbyRepository(application)
    private val context = application

    private val _uiState = MutableStateFlow<EqualizerUiState>(EqualizerUiState.Loading)
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    private var currentProfile = 0
    private var currentBandMode = BandMode.TEN_BAND

    init {
        loadEqualizer()
        viewModelScope.launch {
            repository.profileChanged.collect {
                loadEqualizer()
            }
        }
    }

    fun loadEqualizer() {
        viewModelScope.launch {
            try {
                currentProfile = repository.getCurrentProfile()
                currentBandMode = repository.getBandMode()
                val bandGains = repository.getEqualizerGains(currentProfile, currentBandMode)
                
                val builtInPresets = getBuiltInPresets(currentBandMode)
                val userPresets = repository.getUserPresets()
                val allPresets = userPresets + builtInPresets
                
                val currentPresetName = repository.getPresetName(currentProfile)
                val currentPreset = allPresets.find { it.name == currentPresetName }
                    ?: EqualizerPreset(
                        name = context.getString(R.string.dolby_preset_custom),
                        bandGains = bandGains,
                        isCustom = true,
                        bandMode = currentBandMode
                    )
                
                _uiState.value = EqualizerUiState.Success(
                    presets = allPresets,
                    currentPreset = currentPreset,
                    bandGains = bandGains,
                    bandMode = currentBandMode
                )
            } catch (e: Exception) {
                _uiState.value = EqualizerUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun getBuiltInPresets(bandMode: BandMode): List<EqualizerPreset> {
        val names = context.resources.getStringArray(R.array.dolby_preset_entries)
        val values = context.resources.getStringArray(R.array.dolby_preset_values)
        
        return names.mapIndexed { index, name ->
            val gains = values[index].split(",").map { it.toInt() }
            
            val frequencies = when (bandMode) {
                BandMode.TEN_BAND -> DolbyRepository.BAND_FREQUENCIES_10
                BandMode.FIFTEEN_BAND -> DolbyRepository.BAND_FREQUENCIES_15
                BandMode.TWENTY_BAND -> DolbyRepository.BAND_FREQUENCIES_20
            }
            
            val tenBandGains = gains.filterIndexed { i, _ -> i % 2 == 0 }
            
            val targetGains = when (bandMode) {
                BandMode.TEN_BAND -> tenBandGains
                BandMode.FIFTEEN_BAND -> {
                    val result = mutableListOf<Int>()
                    result.add(tenBandGains[0])
                    result.add(tenBandGains[0])
                    result.add(tenBandGains[0])
                    result.add(tenBandGains[1])
                    result.add(tenBandGains[2])
                    result.add(tenBandGains[3])
                    result.add(tenBandGains[4])
                    result.add((tenBandGains[4] + tenBandGains[5]) / 2)
                    result.add(tenBandGains[5])
                    result.add((tenBandGains[5] + tenBandGains[6]) / 2)
                    result.add(tenBandGains[6])
                    result.add((tenBandGains[6] + tenBandGains[7]) / 2)
                    result.add(tenBandGains[7])
                    result.add((tenBandGains[7] + tenBandGains[8]) / 2)
                    result.add(tenBandGains[9])
                    result
                }
                BandMode.TWENTY_BAND -> gains
            }
            
            EqualizerPreset(
                name = name,
                bandGains = frequencies.mapIndexed { i, freq ->
                    BandGain(frequency = freq, gain = targetGains.getOrElse(i) { 0 })
                },
                bandMode = bandMode
            )
        }
    }

    fun setBandMode(mode: BandMode) {
        viewModelScope.launch {
            repository.setBandMode(mode)
            currentBandMode = mode
            loadEqualizer()
        }
    }

    fun setPreset(preset: EqualizerPreset) {
        viewModelScope.launch {
            val targetGains = if (preset.bandMode != currentBandMode) {
                convertPresetToBandMode(preset, currentBandMode)
            } else {
                preset.bandGains
            }
            
            repository.setEqualizerGains(currentProfile, targetGains, currentBandMode)
            loadEqualizer()
        }
    }

    private fun convertPresetToBandMode(preset: EqualizerPreset, targetMode: BandMode): List<BandGain> {
        val sourceFreqs = when (preset.bandMode) {
            BandMode.TEN_BAND -> DolbyRepository.BAND_FREQUENCIES_10
            BandMode.FIFTEEN_BAND -> DolbyRepository.BAND_FREQUENCIES_15
            BandMode.TWENTY_BAND -> DolbyRepository.BAND_FREQUENCIES_20
        }
        
        val targetFreqs = when (targetMode) {
            BandMode.TEN_BAND -> DolbyRepository.BAND_FREQUENCIES_10
            BandMode.FIFTEEN_BAND -> DolbyRepository.BAND_FREQUENCIES_15
            BandMode.TWENTY_BAND -> DolbyRepository.BAND_FREQUENCIES_20
        }
        
        return targetFreqs.map { targetFreq ->
            val closestIdx = sourceFreqs.indexOfFirst { it >= targetFreq }
            val gain = if (closestIdx == -1) {
                preset.bandGains.lastOrNull()?.gain ?: 0
            } else if (closestIdx == 0) {
                preset.bandGains.firstOrNull()?.gain ?: 0
            } else {
                val prevFreq = sourceFreqs[closestIdx - 1]
                val nextFreq = sourceFreqs[closestIdx]
                val prevGain = preset.bandGains[closestIdx - 1].gain
                val nextGain = preset.bandGains[closestIdx].gain
                
                val ratio = (targetFreq - prevFreq).toFloat() / (nextFreq - prevFreq)
                (prevGain + ratio * (nextGain - prevGain)).toInt()
            }
            BandGain(frequency = targetFreq, gain = gain)
        }
    }

    fun setBandGain(index: Int, gain: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state is EqualizerUiState.Success) {
                val newBandGains = state.bandGains.toMutableList()
                newBandGains[index] = newBandGains[index].copy(gain = gain)
                repository.setEqualizerGains(currentProfile, newBandGains, currentBandMode)
                loadEqualizer()
            }
        }
    }

    fun savePreset(name: String): String? {
        val state = _uiState.value
        if (state !is EqualizerUiState.Success) return "Invalid state"
        
        if (state.presets.any { it.name.equals(name.trim(), ignoreCase = true) }) {
            return context.getString(R.string.dolby_geq_preset_name_exists)
        }
        
        if (name.length > 50) {
            return context.getString(R.string.dolby_geq_preset_name_too_long)
        }
        
        viewModelScope.launch {
            repository.addUserPreset(name.trim(), state.bandGains, currentBandMode)
            loadEqualizer()
        }
        
        return null
    }

    fun deletePreset(preset: EqualizerPreset) {
        if (!preset.isUserDefined) return
        
        viewModelScope.launch {
            repository.deleteUserPreset(preset.name)
            loadEqualizer()
        }
    }

    fun resetGains() {
        viewModelScope.launch {
            val flatPreset = getBuiltInPresets(currentBandMode).first()
            repository.setEqualizerGains(currentProfile, flatPreset.bandGains, currentBandMode)
            loadEqualizer()
        }
    }
}
