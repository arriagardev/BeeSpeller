package com.example.beespeller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.beespeller.data.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearProgress: () -> Unit
) {
    var challengeLimit by remember { mutableStateOf(settingsManager.challengeLimit.toString()) }
    var timeWindow by remember { mutableStateOf(settingsManager.timeWindowHours.toString()) }
    var maxAttempts by remember { mutableStateOf(settingsManager.maxAttemptsPerWindow.toString()) }
    
    var showClearConfirm1 by remember { mutableStateOf(false) }
    var showClearConfirm2 by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Usage Limits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            LabeledTextField(
                value = challengeLimit,
                onValueChange = { if (it.all { char -> char.isDigit() }) challengeLimit = it },
                label = "AI Hints per Challenge Group",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                helperText = "Max 'Meaning' or 'Example' uses per session"
            )

            HorizontalDivider()

            Text("Global API Throttling", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            LabeledTextField(
                value = timeWindow,
                onValueChange = { if (it.all { char -> char.isDigit() }) timeWindow = it },
                label = "Reset Period (Hours)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            LabeledTextField(
                value = maxAttempts,
                onValueChange = { if (it.all { char -> char.isDigit() }) maxAttempts = it },
                label = "Max API Calls per Period",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                helperText = "Set to 0 to disable all AI features"
            )

            HorizontalDivider()

            Text("Data Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Export")
                }
                Button(
                    onClick = onImport,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Import")
                }
            }

            OutlinedButton(
                onClick = { showClearConfirm1 = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error))
            ) {
                Icon(Icons.Default.DeleteForever, null)
                Spacer(Modifier.width(8.dp))
                Text("Clear All Progress")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    settingsManager.challengeLimit = challengeLimit.toIntOrNull() ?: 3
                    settingsManager.timeWindowHours = timeWindow.toIntOrNull() ?: 1
                    settingsManager.maxAttemptsPerWindow = maxAttempts.toIntOrNull() ?: 0
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save Settings")
            }
        }
    }

    // Confirmation Dialog 1
    if (showClearConfirm1) {
        AlertDialog(
            onDismissRequest = { showClearConfirm1 = false },
            title = { Text("Clear All Progress?") },
            text = { Text("This will delete all mastery levels and custom words. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { 
                    showClearConfirm1 = false
                    showClearConfirm2 = true 
                }) {
                    Text("YES, CLEAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm1 = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Confirmation Dialog 2
    if (showClearConfirm2) {
        AlertDialog(
            onDismissRequest = { showClearConfirm2 = false },
            title = { Text("Final Warning") },
            text = { Text("Are you absolutely sure? Please make sure you have an Export file if you want to restore your data later.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showClearConfirm2 = false
                        onClearProgress()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("DELETE EVERYTHING")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm2 = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions,
    helperText: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = keyboardOptions,
            singleLine = true
        )
        if (helperText != null) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
