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
import com.phonewhisperer.presentation.theme.*
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    // Local toggle states (these would connect to SharedPreferences in production)
    var locationEnabled by remember { mutableStateOf(true) }
    var usageStatsEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var screenTrackingEnabled by remember { mutableStateOf(true) }
    var ringerTrackingEnabled by remember { mutableStateOf(true) }
    var geofencingEnabled by remember { mutableStateOf(true) }

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
                        SettingsToggle(Icons.Rounded.LocationOn, "Location Tracking", "GPS-based place detection", StatusActive, locationEnabled) { locationEnabled = it }
                        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(Icons.Rounded.Apps, "App Usage", "Track which apps you use", WhispererSecondary, usageStatsEnabled) { usageStatsEnabled = it }
                        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(Icons.Rounded.Notifications, "Notification Tracking", "Monitor notification patterns", WhispererAccent, notificationsEnabled) { notificationsEnabled = it }
                        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(Icons.Rounded.PhoneAndroid, "Screen State", "Track screen on/off events", WhispererPrimary, screenTrackingEnabled) { screenTrackingEnabled = it }
                        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(Icons.Rounded.VolumeOff, "Ringer Mode", "Detect silent/vibrate changes", WhispererTertiary, ringerTrackingEnabled) { ringerTrackingEnabled = it }
                        HorizontalDivider(color = DarkSurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggle(Icons.Rounded.FmdGood, "Geofencing", "Auto-detect frequent places", StatusActive, geofencingEnabled) { geofencingEnabled = it }
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
