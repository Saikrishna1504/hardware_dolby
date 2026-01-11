/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.data.PresetExportManager
import org.lunaris.dolby.domain.models.EqualizerPreset
import org.lunaris.dolby.domain.models.EqualizerUiState
import org.lunaris.dolby.ui.viewmodel.EqualizerViewModel
import org.lunaris.dolby.utils.ToastHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetImportExportScreen(
    viewModel: EqualizerViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportManager = remember { PresetExportManager(context) }
    val uiState by viewModel.uiState.collectAsState()
    var selectedPreset by remember { mutableStateOf<EqualizerPreset?>(null) }
    var showExportOptions by remember { mutableStateOf(false) }
    var showBatchExport by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val backgroundColor = Color(context.getColor(R.color.screen_background))
    
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            selectedPreset?.let { preset ->
                scope.launch {
                    isLoading = true
                    exportManager.exportPresetToFile(preset, uri).fold(
                        onSuccess = {
                            ToastHelper.showToast(context, "Preset exported successfully!")
                        },
                        onFailure = { e ->
                            ToastHelper.showToast(context, "Export failed: ${e.message}")
                        }
                    )
                    isLoading = false
                    showExportOptions = false
                }
            }
        }
    }
    
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isLoading = true
                exportManager.importPresetFromFile(uri).fold(
                    onSuccess = { preset ->
                        val error = viewModel.saveImportedPreset(preset)
                        if (error != null) {
                            ToastHelper.showToast(context, error)
                        } else {
                            ToastHelper.showToast(
                                context, 
                                "Preset '${preset.name}' imported! (${preset.bandMode.displayName})"
                            )
                            viewModel.loadEqualizer()
                        }
                    },
                    onFailure = { e ->
                        ToastHelper.showToast(context, "Import failed: ${e.message}")
                    }
                )
                isLoading = false
            }
        }
    }
    
    val batchExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                isLoading = true
                val state = uiState as? EqualizerUiState.Success
                val presets = state?.presets?.filter { it.isUserDefined } ?: emptyList()
                
                exportManager.exportMultiplePresets(presets, uri).fold(
                    onSuccess = {
                        ToastHelper.showToast(context, "${presets.size} presets exported!")
                    },
                    onFailure = { e ->
                        ToastHelper.showToast(context, "Batch export failed: ${e.message}")
                    }
                )
                isLoading = false
                showBatchExport = false
            }
        }
    }
    
    val batchImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isLoading = true
                exportManager.importMultiplePresets(uri).fold(
                    onSuccess = { presets ->
                        var successCount = 0
                        presets.forEach { preset ->
                            if (viewModel.saveImportedPreset(preset) == null) {
                                successCount++
                            }
                        }
                        ToastHelper.showToast(
                            context, 
                            "Imported $successCount of ${presets.size} presets"
                        )
                        viewModel.loadEqualizer()
                    },
                    onFailure = { e ->
                        ToastHelper.showToast(context, "Batch import failed: ${e.message}")
                    }
                )
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Import/Export Presets",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showBatchExport = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Batch export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is EqualizerUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.FileDownload,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Import Presets",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        "Import presets from files or clipboard",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { importLauncher.launch("*/*") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Single File")
                                        }
                                        Button(
                                            onClick = { batchImportLauncher.launch("*/*") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.FolderCopy, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Batch")
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                exportManager.importPresetFromClipboard().fold(
                                                    onSuccess = { preset ->
                                                        val error = viewModel.saveImportedPreset(preset)
                                                        if (error != null) {
                                                            ToastHelper.showToast(context, error)
                                                        } else {
                                                            ToastHelper.showToast(
                                                                context, 
                                                                "Preset imported from clipboard! (${preset.bandMode.displayName})"
                                                            )
                                                            viewModel.loadEqualizer()
                                                        }
                                                    },
                                                    onFailure = { e ->
                                                        ToastHelper.showToast(
                                                            context, 
                                                            "Clipboard import failed: ${e.message}"
                                                        )
                                                    }
                                                )
                                                isLoading = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("From Clipboard")
                                    }
                                }
                            }
                        }
                        item {
                            Text(
                                "Your Custom Presets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(state.presets.filter { it.isUserDefined }) { preset ->
                            PresetExportCard(
                                preset = preset,
                                onExportFile = {
                                    selectedPreset = preset
                                    exportLauncher.launch("${preset.name.replace(" ", "_")}_${preset.bandMode.value}band.ldp")
                                },
                                onCopyClipboard = {
                                    scope.launch {
                                        isLoading = true
                                        exportManager.copyPresetToClipboard(preset).fold(
                                            onSuccess = {
                                                ToastHelper.showToast(
                                                    context, 
                                                    "Preset copied to clipboard!"
                                                )
                                            },
                                            onFailure = { e ->
                                                ToastHelper.showToast(
                                                    context, 
                                                    "Copy failed: ${e.message}"
                                                )
                                            }
                                        )
                                        isLoading = false
                                    }
                                },
                                onShare = {
                                    scope.launch {
                                        isLoading = true
                                        exportManager.createShareIntent(preset).fold(
                                            onSuccess = { intent ->
                                                context.startActivity(intent)
                                            },
                                            onFailure = { e ->
                                                ToastHelper.showToast(
                                                    context, 
                                                    "Share failed: ${e.message}"
                                                )
                                            }
                                        )
                                        isLoading = false
                                    }
                                }
                            )
                        }
                        item {
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Processing...")
                        }
                    }
                }
            }
        }
    }
    
    if (showBatchExport) {
        val state = uiState as? EqualizerUiState.Success
        val presetCount = state?.presets?.count { it.isUserDefined } ?: 0
        
        AlertDialog(
            onDismissRequest = { showBatchExport = false },
            icon = {
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Batch Export") },
            text = { 
                Text("Export all $presetCount custom presets to a single file?") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        batchExportLauncher.launch("dolby_presets_backup.ldp")
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Export All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatchExport = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PresetExportCard(
    preset: EqualizerPreset,
    onExportFile: () -> Unit,
    onCopyClipboard: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${preset.bandMode.displayName} • ${preset.bandGains.size} bands",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onExportFile) {
                        Icon(
                            Icons.Default.FileDownload, 
                            contentDescription = "Export to file",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onCopyClipboard) {
                        Icon(
                            Icons.Default.ContentCopy, 
                            contentDescription = "Copy to clipboard",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            Icons.Default.Share, 
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}
