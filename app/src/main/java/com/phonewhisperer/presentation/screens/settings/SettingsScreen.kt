package com.phonewhisperer.presentation.screens.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phonewhisperer.presentation.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    // State connected to SharedPreferences
    val locationEnabled by viewModel.locationEnabled.collectAsState()
    val usageStatsEnabled by viewModel.usageStatsEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val screenTrackingEnabled by viewModel.screenTrackingEnabled.collectAsState()
    val ringerTrackingEnabled by viewModel.ringerTrackingEnabled.collectAsState()
    val geofencingEnabled by viewModel.geofencingEnabled.collectAsState()
    val aiAutoRunEnabled by viewModel.aiAutoRunEnabled.collectAsState()
    val isModelDownloaded by viewModel.isModelDownloaded.collectAsState()
    var downloadProgress by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 80.dp)
    ) {
        // Header
        AnimatedVisibility(visible, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Settings, null, modifier = Modifier.size(32.dp), tint = TextSecondary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text("Configure your AI assistant", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Data Sources Section
        AnimatedVisibility(visible, enter = fadeIn(tween(800, 200)) + slideInVertically(tween(600, 200)) { 40 }) {
            Column {
                SectionHeader("Data Sources", "Toggle what the AI observes")
                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(Modifier.padding(4.dp)) {
                        SettingsToggle(Icons.Rounded.LocationOn, "Location Tracking", "GPS-based place detection", StatusActive, locationEnabled) { viewModel.setLocationEnabled(it) }
                        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(Icons.Rounded.Apps, "App Usage", "Track which apps you use", WhispererSecondary, usageStatsEnabled) { viewModel.setUsageStatsEnabled(it) }
                        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(Icons.Rounded.Notifications, "Notification Tracking", "Monitor notification patterns", WhispererAccent, notificationsEnabled) { viewModel.setNotificationsEnabled(it) }
                        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(Icons.Rounded.PhoneAndroid, "Screen State", "Track screen on/off events", WhispererPrimary, screenTrackingEnabled) { viewModel.setScreenTrackingEnabled(it) }
                        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(Icons.Rounded.VolumeOff, "Ringer Mode", "Detect silent/vibrate changes", WhispererTertiary, ringerTrackingEnabled) { viewModel.setRingerTrackingEnabled(it) }
                        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(Icons.Rounded.FmdGood, "Geofencing", "Auto-detect frequent places", StatusActive, geofencingEnabled) { viewModel.setGeofencingEnabled(it) }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Privacy Section
        AnimatedVisibility(visible, enter = fadeIn(tween(800, 400)) + slideInVertically(tween(600, 400)) { 40 }) {
            Column {
                SectionHeader("Privacy & Data", "Your data never leaves this device")
                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        SettingsInfoRow(Icons.Rounded.Security, "On-Device Only", "All data is processed locally. Nothing is ever sent to any server.")
                        Spacer(Modifier.height(16.dp))
                        SettingsInfoRow(Icons.Rounded.AutoDelete, "14-Day Retention", "Raw event data is automatically deleted after 14 days.")
                        Spacer(Modifier.height(16.dp))
                        SettingsInfoRow(Icons.Rounded.VisibilityOff, "No Content Storage", "Notification text and message content are never stored — only metadata.")
                        
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val scope = rememberCoroutineScope()
                        var showWipeDialog by remember { mutableStateOf(false) }

                        if (showWipeDialog) {
                            AlertDialog(
                                onDismissRequest = { showWipeDialog = false },
                                title = { Text("Wipe AI Memory?", style = MaterialTheme.typography.titleMedium) },
                                text = { Text("This will permanently delete all collected routines, location history, and generated automation rules. This action cannot be undone.", style = MaterialTheme.typography.bodyMedium) },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showWipeDialog = false
                                            scope.launch {
                                                val repo = context.applicationContext.let { appCtx ->
                                                    dagger.hilt.android.EntryPointAccessors.fromApplication(
                                                        appCtx,
                                                        com.phonewhisperer.di.RepositoryEntryPoint::class.java
                                                    ).eventRepository()
                                                }
                                                repo.wipeAllData()
                                                android.widget.Toast.makeText(context, "AI Memory Wiped Successfully", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = StatusWarning)
                                    ) { Text("Yes, Wipe Data", fontWeight = FontWeight.Bold) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showWipeDialog = false }) { Text("Cancel", color = TextSecondary) }
                                },
                                containerColor = DarkCard,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                textContentColor = TextSecondary
                            )
                        }

                        Button(
                            onClick = { showWipeDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusWarning.copy(alpha = 0.15f), contentColor = StatusWarning),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteForever, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Wipe AI Memory", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // AI Engine Section
        AnimatedVisibility(visible, enter = fadeIn(tween(800, 400)) + slideInVertically(tween(600, 400)) { 40 }) {
            Column {
                SectionHeader("AI Engine", "How PhoneWhisperer thinks")
                Spacer(Modifier.height(12.dp))

                val geminiAvailable = com.phonewhisperer.ai_engine.llm.GeminiRuleEnhancer.isAvailable

                val onDeviceContext = androidx.compose.ui.platform.LocalContext.current
                val onDeviceAvailable = com.phonewhisperer.ai_engine.llm.OnDeviceLlmEngine.isModelAvailable(onDeviceContext)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        SettingsInfoRow(
                            Icons.Rounded.AutoAwesome,
                            "Clustering Algorithm",
                            "DBSCAN with cyclic sin/cos temporal encoding, Haversine spatial distance, and app-category semantic matching"
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        SettingsToggle(
                            icon = Icons.Rounded.Autorenew, 
                            title = "AI Auto-Run", 
                            subtitle = "Automatically run Pattern Analysis", 
                            iconColor = WhispererPrimary, 
                            isEnabled = aiAutoRunEnabled
                        ) { viewModel.setAiAutoRunEnabled(it) }
                        
                        Spacer(Modifier.height(16.dp))

                        val ruleGenMode = when {
                            onDeviceAvailable -> "On-device Gemma 2B (MediaPipe) — fully offline, no API key"
                            geminiAvailable -> "Cloud Gemini API — heuristic baseline + LLM enhancement"
                            else -> "Heuristic-only — on-device rule generation"
                        }
                        val ruleGenIcon = when {
                            onDeviceAvailable -> Icons.Rounded.Memory
                            geminiAvailable -> Icons.Rounded.Cloud
                            else -> Icons.Rounded.Settings
                        }
                        SettingsInfoRow(ruleGenIcon, "Rule Generator", ruleGenMode)

                        Spacer(Modifier.height(16.dp))
                        SettingsInfoRow(
                            Icons.Rounded.Speed,
                            "Inference Schedule",
                            "Auto-runs every 6 hours when ≥10 unprocessed events exist. Manual trigger via Dashboard title tap."
                        )

                        // Status badge
                        Spacer(Modifier.height(12.dp))
                        val (badgeText, badgeColor) = when {
                            onDeviceAvailable -> "🔒 On-Device Gemma 2B Active" to StatusActive
                            geminiAvailable -> "☁️ Gemini Cloud LLM Active" to WhispererSecondary
                            else -> "⚡ Heuristic Engine Active" to TextSecondary
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(badgeColor.copy(0.1f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = badgeColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(badgeText, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = badgeColor)
                            }
                        }

                        if (!onDeviceAvailable) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Text("Fully Offline Mode (Optional)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "For maximum privacy, you can run the AI entirely on your phone without internet. " +
                                "Download the Gemma 2B model (~1.3GB) from Kaggle and place 'gemma-2b-it-gpu-int4.bin' " +
                                "in your Downloads folder. The app will auto-detect it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            
                            if (isModelDownloaded) {
                                Button(
                                    onClick = { },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusActive.copy(alpha = 0.2f), contentColor = StatusActive),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = false
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Gemma 2B Model Downloaded")
                                }
                            } else if (downloadProgress != null) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress!! / 100f },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = WhispererPrimary,
                                        trackColor = DarkSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("Downloading... $downloadProgress%", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
                                }
                            } else {
                                val scope = rememberCoroutineScope()
                                Button(
                                    onClick = {
                                        val flow = viewModel.startModelDownload()
                                        if (flow != null) {
                                            scope.launch {
                                                flow.collect { progress ->
                                                    downloadProgress = progress
                                                    if (progress >= 100) {
                                                        downloadProgress = null
                                                        viewModel.refreshModelStatus()
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onBackground),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Download Gemma 2B Model (1.5GB)")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // About Section
        AnimatedVisibility(visible, enter = fadeIn(tween(800, 600)) + slideInVertically(tween(600, 600)) { 40 }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(WhispererPrimary.copy(0.15f), WhispererSecondary.copy(0.05f))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Psychology, null, modifier = Modifier.size(48.dp), tint = WhispererPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text("PhoneWhisperer", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                        Text("v0.1.0-alpha", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "A privacy-first, on-device AI agent that learns your phone usage routines and automates them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {}, label = { Text("Kotlin", style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(Icons.Rounded.Code, null, Modifier.size(14.dp)) })
                            AssistChip(onClick = {}, label = { Text("Room", style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(Icons.Rounded.Storage, null, Modifier.size(14.dp)) })
                            AssistChip(onClick = {}, label = { Text("DBSCAN", style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(14.dp)) })
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {}, label = { Text("Gemini", style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(Icons.Rounded.Psychology, null, Modifier.size(14.dp)) })
                            AssistChip(onClick = {}, label = { Text("Hilt", style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(Icons.Rounded.Hub, null, Modifier.size(14.dp)) })
                            AssistChip(onClick = {}, label = { Text("Compose", style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(Icons.Rounded.Brush, null, Modifier.size(14.dp)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: androidx.compose.ui.graphics.Color,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (isEnabled) iconColor else TextMuted)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (isEnabled) MaterialTheme.colorScheme.onBackground else TextMuted)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = iconColor,
                checkedTrackColor = iconColor.copy(0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = StatusActive)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
