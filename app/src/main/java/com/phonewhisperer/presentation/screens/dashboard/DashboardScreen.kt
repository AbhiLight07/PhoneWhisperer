package com.phonewhisperer.presentation.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.phonewhisperer.presentation.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val isCollecting by viewModel.isCollecting.collectAsState()
    val behaviorCount by viewModel.behaviorEventCount.collectAsState()
    val locationCount by viewModel.locationEventCount.collectAsState()
    val appUsageCount by viewModel.appUsageEventCount.collectAsState()
    val distinctApps by viewModel.distinctAppCount.collectAsState()
    val totalCount by viewModel.totalEventCount.collectAsState()
    val lastTimestamp by viewModel.lastEventTimestamp.collectAsState()
    val notificationCount by viewModel.notificationEventCount.collectAsState()
    val notificationApps by viewModel.notificationDistinctAppCount.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        AnimatedVisibility(visible, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }) {
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Column(modifier = Modifier.clickable {
                        // Secret Debug Trigger for Hackathon Demo
                        val repo = context.applicationContext.let { appCtx ->
                            dagger.hilt.android.EntryPointAccessors.fromApplication(
                                appCtx,
                                com.phonewhisperer.di.RepositoryEntryPoint::class.java
                            ).eventRepository()
                        }
                        com.phonewhisperer.utils.MockDataGenerator.injectMockDataAndRunAI(context, repo)
                        android.widget.Toast.makeText(context, "AI Simulator Triggered!", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Text("PhoneWhisperer", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text("Silent Observer Mode", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    IconButton(onClick = { viewModel.toggleCollection() },
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(if (isCollecting) StatusActive.copy(0.15f) else StatusPaused.copy(0.15f))
                    ) {
                        Icon(if (isCollecting) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null, tint = if (isCollecting) StatusActive else StatusPaused)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (isCollecting) StatusActive else StatusPaused))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isCollecting) "Actively observing your routine" else "Collection paused",
                        style = MaterialTheme.typography.labelLarge, color = if (isCollecting) StatusActive else StatusPaused)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Hero Card
        AnimatedVisibility(visible, enter = fadeIn(tween(800, 200)) + slideInVertically(tween(600, 200)) { 40 }) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(WhispererPrimary.copy(0.3f), WhispererSecondary.copy(0.1f)))).padding(24.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Psychology, null, tint = WhispererPrimary, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Behavior Memory", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("$totalCount", style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                        Text("total events recorded", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        if (lastTimestamp != null) {
                            Spacer(Modifier.height(8.dp))
                            Text("Last: ${SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(lastTimestamp!!))}",
                                style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Metric Grid — 3 rows with Phase 2 additions
        AnimatedVisibility(visible, enter = fadeIn(tween(800, 400)) + slideInVertically(tween(600, 400)) { 40 }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    MetricCard(Modifier.weight(1f), Icons.Rounded.LocationOn, "Locations", "$locationCount", listOf(StatusActive.copy(0.2f), StatusActive.copy(0.05f)))
                    MetricCard(Modifier.weight(1f), Icons.Rounded.Apps, "App Sessions", "$appUsageCount", listOf(WhispererSecondary.copy(0.2f), WhispererSecondary.copy(0.05f)))
                }
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    MetricCard(Modifier.weight(1f), Icons.Rounded.Timeline, "Behaviors", "$behaviorCount", listOf(WhispererPrimary.copy(0.2f), WhispererPrimary.copy(0.05f)))
                    MetricCard(Modifier.weight(1f), Icons.Rounded.CalendarMonth, "Unique Apps", "$distinctApps", listOf(StatusPaused.copy(0.2f), StatusPaused.copy(0.05f)))
                }
                // Phase 2: Notification metrics
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    MetricCard(Modifier.weight(1f), Icons.Rounded.NotificationsActive, "Notifications", "$notificationCount", listOf(WhispererAccent.copy(0.2f), WhispererAccent.copy(0.05f)))
                    MetricCard(Modifier.weight(1f), Icons.Rounded.PhoneAndroid, "Notifying Apps", "$notificationApps", listOf(StatusWarning.copy(0.2f), StatusWarning.copy(0.05f)))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Phase Indicator
        AnimatedVisibility(visible, enter = fadeIn(tween(800, 600)) + slideInVertically(tween(600, 600)) { 40 }) {
            PhaseIndicator(totalCount)
        }
    }
}

@Composable
private fun MetricCard(modifier: Modifier, icon: ImageVector, label: String, value: String, colors: List<androidx.compose.ui.graphics.Color>) {
    Card(modifier, RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(colors)).padding(16.dp)) {
            Column {
                Icon(icon, label, tint = colors[0].copy(alpha = 1f), modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(12.dp))
                Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun PhaseIndicator(totalEvents: Int) {
    val phase = when {
        totalEvents == 0 -> "Waiting to start..."
        totalEvents < 100 -> "Phase 1: Observing"
        totalEvents < 500 -> "Phase 1: Learning patterns"
        totalEvents < 1000 -> "Phase 2: Expanding sensors"
        totalEvents < 2000 -> "Phase 2: Detecting routines"
        else -> "Phase 3: Ready for inference"
    }
    val progress by animateFloatAsState(when {
        totalEvents == 0 -> 0f
        totalEvents < 100 -> totalEvents / 100f * 0.15f
        totalEvents < 500 -> 0.15f + (totalEvents - 100) / 400f * 0.20f
        totalEvents < 1000 -> 0.35f + (totalEvents - 500) / 500f * 0.20f
        totalEvents < 2000 -> 0.55f + (totalEvents - 1000) / 1000f * 0.20f
        else -> 0.75f + minOf((totalEvents - 2000) / 2000f * 0.25f, 0.25f)
    }, tween(1000), label = "progress")

    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Column(Modifier.padding(20.dp)) {
            Text(phase, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(WhispererPrimary, WhispererSecondary))))
            }
            Spacer(Modifier.height(8.dp))
            Text("Collecting data across 7 sensor types. AI engine activates after sufficient patterns emerge.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
