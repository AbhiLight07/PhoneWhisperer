package com.phonewhisperer.presentation.screens.dashboard

import android.widget.Toast
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phonewhisperer.data.local.db.entity.BehaviorPatternEntity
import com.phonewhisperer.presentation.theme.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val isCollecting by viewModel.isCollecting.collectAsState()
    val behaviorCount by viewModel.behaviorEventCount.collectAsState()
    val locationCount by viewModel.locationEventCount.collectAsState()
    val appUsageCount by viewModel.appUsageEventCount.collectAsState()
    val distinctApps by viewModel.distinctAppCount.collectAsState()
    val totalScreenTimeMs by viewModel.totalScreenTimeMs.collectAsState()
    val totalCount by viewModel.totalEventCount.collectAsState()
    val lastTimestamp by viewModel.lastEventTimestamp.collectAsState()
    val notificationCount by viewModel.notificationEventCount.collectAsState()
    val notificationApps by viewModel.notificationDistinctAppCount.collectAsState()
    val detectedPatterns by viewModel.detectedPatterns.collectAsState()
    val activeRules by viewModel.activeRules.collectAsState()

    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────
        AnimatedVisibility(visible, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }) {
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(modifier = Modifier.clickable {
                        val repo = context.applicationContext.let { appCtx ->
                            EntryPointAccessors.fromApplication(
                                appCtx,
                                com.phonewhisperer.di.RepositoryEntryPoint::class.java
                            ).eventRepository()
                        }
                        com.phonewhisperer.utils.MockDataGenerator.injectMockDataAndRunAI(context, repo)
                        Toast.makeText(context, "🔬 AI Simulator Triggered!", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("PhoneWhisperer", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text("Silent Observer Mode", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    IconButton(onClick = { viewModel.toggleCollection() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isCollecting) StatusActive.copy(0.15f) else StatusPaused.copy(0.15f))
                    ) {
                        Icon(if (isCollecting) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null, tint = if (isCollecting) StatusActive else StatusPaused)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val pulse by rememberInfiniteTransition(label = "status_pulse").animateFloat(
                        0.6f, 1f,
                        infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
                        label = "pulse"
                    )
                    Box(Modifier.size(8.dp).scale(if (isCollecting) pulse else 1f).clip(CircleShape).background(if (isCollecting) StatusActive else StatusPaused))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isCollecting) "Actively observing your routine" else "Collection paused",
                        style = MaterialTheme.typography.labelLarge, color = if (isCollecting) StatusActive else StatusPaused)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Hero Card ───────────────────────────────────────────────
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
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$totalCount", style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.width(8.dp))
                            Text("events", style = MaterialTheme.typography.bodyLarge, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        if (lastTimestamp != null) {
                            Spacer(Modifier.height(4.dp))
                            Text("Last: ${SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(lastTimestamp!!))}",
                                style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        // Show AI status
                        if (detectedPatterns.isNotEmpty() || activeRules.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (detectedPatterns.isNotEmpty()) {
                                    StatusChip("${detectedPatterns.size} patterns", WhispererAccent)
                                }
                                if (activeRules.isNotEmpty()) {
                                    StatusChip("${activeRules.size} active rules", StatusActive)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── AI Insights Section ─────────────────────────────────────
        if (detectedPatterns.isNotEmpty()) {
            AnimatedVisibility(visible, enter = fadeIn(tween(800, 350)) + slideInVertically(tween(600, 350)) { 40 }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = WhispererAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("AI Insights", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        detectedPatterns.take(3).forEach { pattern ->
                            InsightCard(pattern)
                        }
                        if (detectedPatterns.size > 3) {
                            Text("+ ${detectedPatterns.size - 3} more patterns", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Metric Grid ─────────────────────────────────────────────
        AnimatedVisibility(visible, enter = fadeIn(tween(800, 500)) + slideInVertically(tween(600, 500)) { 40 }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        Modifier.weight(1f), Icons.Rounded.LocationOn, "Locations", "$locationCount",
                        listOf(StatusActive.copy(0.2f), StatusActive.copy(0.05f)),
                        info = "GPS samples recorded every 15 min. Used by DBSCAN to detect places like HOME and WORK."
                    )
                    MetricCard(
                        Modifier.weight(1f), Icons.Rounded.Apps, "App Sessions", "$appUsageCount",
                        listOf(WhispererSecondary.copy(0.2f), WhispererSecondary.copy(0.05f)),
                        info = "Each time you open and use an app for 3+ seconds, it counts as one session. Tracks which apps, when, and for how long."
                    )
                }
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        Modifier.weight(1f), Icons.Rounded.Timeline, "Behaviors", "$behaviorCount",
                        listOf(WhispererPrimary.copy(0.2f), WhispererPrimary.copy(0.05f)),
                        info = "Ringer changes, screen on/off, and other device state events. These feed into DBSCAN to find your daily patterns."
                    )
                    MetricCard(
                        Modifier.weight(1f), Icons.Rounded.Fingerprint, "Unique Apps", "$distinctApps",
                        listOf(StatusPaused.copy(0.2f), StatusPaused.copy(0.05f)),
                        info = "Number of different apps you've used. Categorized into SOCIAL, PRODUCTIVITY, etc. for smarter clustering."
                    )
                }
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        Modifier.weight(1f), Icons.Rounded.NotificationsActive, "Notifications", "$notificationCount",
                        listOf(WhispererAccent.copy(0.2f), WhispererAccent.copy(0.05f)),
                        info = "Notifications received from all apps. Used to detect which apps you dismiss often → auto-block suggestions."
                    )
                    
                    val hours = totalScreenTimeMs / (1000 * 60 * 60)
                    val minutes = (totalScreenTimeMs / (1000 * 60)) % 60
                    val screenTimeStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                    
                    MetricCard(
                        Modifier.weight(1f), Icons.Rounded.Timer, "Screen Time", screenTimeStr,
                        listOf(StatusWarning.copy(0.2f), StatusWarning.copy(0.05f)),
                        info = "Total accumulated foreground usage time across all apps since collection started."
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Phase Progress ──────────────────────────────────────────
        AnimatedVisibility(visible, enter = fadeIn(tween(800, 700)) + slideInVertically(tween(600, 700)) { 40 }) {
            PhaseIndicator(totalCount, detectedPatterns.size, activeRules.size)
        }

        Spacer(Modifier.height(80.dp)) // Bottom nav clearance
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}

@Composable
private fun InsightCard(pattern: BehaviorPatternEntity) {
    val confPct = (pattern.confidence * 100).roundToInt()
    val (icon, color) = getPatternVisuals(pattern.patternType)

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(color.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = color)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(pattern.description.split("\n").first(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConfidenceBadge(confPct)
                    Text("${pattern.eventCount} observations", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(confidencePct: Int) {
    val color = when {
        confidencePct >= 80 -> StatusActive
        confidencePct >= 50 -> StatusPaused
        else -> StatusWarning
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text("$confidencePct%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
private fun MetricCard(modifier: Modifier, icon: ImageVector, label: String, value: String, colors: List<Color>, info: String? = null) {
    var showInfo by remember { mutableStateOf(false) }

    if (showInfo && info != null) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(label, style = MaterialTheme.typography.titleMedium) },
            text = { Text(info, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("Got it") }
            },
            containerColor = DarkCard,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = TextSecondary
        )
    }

    Card(modifier, RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(colors)).padding(16.dp)) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, label, tint = colors[0].copy(alpha = 1f), modifier = Modifier.size(24.dp))
                    if (info != null) {
                        Icon(
                            Icons.Rounded.Info, 
                            contentDescription = "Info", 
                            tint = TextMuted, 
                            modifier = Modifier.size(18.dp).clickable { showInfo = true }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun PhaseIndicator(totalEvents: Int, patternCount: Int, ruleCount: Int) {
    val phase = when {
        ruleCount > 0 -> "Phase 4: Automating your routines"
        patternCount > 0 -> "Phase 3: AI detected ${patternCount} patterns"
        totalEvents >= 1000 -> "Phase 2: Building behavioral profile"
        totalEvents >= 100 -> "Phase 1: Learning your patterns"
        totalEvents > 0 -> "Phase 1: Collecting initial data"
        else -> "Waiting to start..."
    }
    val progress by animateFloatAsState(when {
        ruleCount > 0 -> 0.95f
        patternCount > 0 -> 0.75f
        totalEvents >= 1000 -> 0.55f
        totalEvents >= 100 -> 0.35f
        totalEvents > 0 -> 0.15f
        else -> 0f
    }, tween(1000), label = "progress")

    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Column(Modifier.padding(20.dp)) {
            Text(phase, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(WhispererPrimary, WhispererSecondary))))
            }
            Spacer(Modifier.height(8.dp))
            Text("Observe → Infer → Automate",
                style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

private fun getPatternVisuals(patternType: String): Pair<ImageVector, Color> {
    return when (patternType) {
        "TYPE_SILENT_MODE" -> Icons.Rounded.VolumeOff to WhispererTertiary
        "TYPE_SCREEN_OFF" -> Icons.Rounded.MobileOff to WhispererPrimary
        "TYPE_NOTIFICATION" -> Icons.Rounded.Notifications to WhispererAccent
        "TYPE_APP_USAGE" -> Icons.Rounded.Apps to WhispererSecondary
        "TYPE_GEOFENCE_TRANSITION" -> Icons.Rounded.FmdGood to StatusActive
        else -> Icons.Rounded.AutoAwesome to StatusProcessed
    }
}
