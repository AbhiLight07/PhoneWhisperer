package com.phonewhisperer.presentation.screens.timeline

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phonewhisperer.data.local.db.entity.BehaviorEvent
import com.phonewhisperer.presentation.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimelineScreen(viewModel: TimelineViewModel = hiltViewModel()) {
    val events by viewModel.recentEvents.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp, start = 20.dp, end = 20.dp)
    ) {
        // Header
        AnimatedVisibility(visible, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timeline, null, modifier = Modifier.size(32.dp), tint = WhispererSecondary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Activity Timeline", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text("Everything the AI has observed", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (events.isEmpty()) {
            AnimatedVisibility(visible, enter = fadeIn(tween(800, 300))) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val pulse by rememberInfiniteTransition(label = "empty").animateFloat(
                        0.85f, 1f,
                        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
                        label = "scale"
                    )
                    Box(
                        Modifier.size(80.dp).clip(CircleShape).background(DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Visibility, null, Modifier.size(40.dp), tint = TextMuted)
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("No activity yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(8.dp))
                    Text("Events will appear here as the AI observes your phone usage patterns.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
                }
            }
        } else {
            // Date grouping header
            AnimatedVisibility(visible, enter = fadeIn(tween(800, 200)) + slideInVertically(tween(600, 200)) { 40 }) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    var lastDateStr = ""

                    itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
                        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(event.timestamp))

                        // Date separator
                        if (dateStr != lastDateStr) {
                            lastDateStr = dateStr
                            if (index > 0) Spacer(Modifier.height(16.dp))
                            Text(
                                text = if (isToday(event.timestamp)) "Today" else if (isYesterday(event.timestamp)) "Yesterday" else dateStr,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        TimelineItem(event = event, isLast = index == events.lastIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(event: BehaviorEvent, isLast: Boolean) {
    val (icon, color, label) = getEventVisuals(event.eventType)
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(event.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline rail
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = color)
            }
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(
                            Brush.verticalGradient(listOf(color.copy(0.3f), Color.Transparent))
                        )
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // Content
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.titleSmall, color = color)
                    Text(timeStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                if (event.payload.isNotBlank() && event.payload != "{}") {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatPayload(event.eventType, event.payload),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class EventVisuals(val icon: ImageVector, val color: Color, val label: String)

private fun getEventVisuals(eventType: String): EventVisuals {
    return when (eventType) {
        BehaviorEvent.TYPE_LOCATION_CHANGE -> EventVisuals(Icons.Rounded.LocationOn, StatusActive, "Location")
        BehaviorEvent.TYPE_APP_OPEN -> EventVisuals(Icons.Rounded.Apps, WhispererSecondary, "App Usage")
        BehaviorEvent.TYPE_CALENDAR -> EventVisuals(Icons.Rounded.CalendarMonth, StatusPaused, "Calendar")
        BehaviorEvent.TYPE_SILENT_MODE -> EventVisuals(Icons.Rounded.VolumeOff, WhispererTertiary, "Ringer Changed")
        BehaviorEvent.TYPE_SCREEN_ON -> EventVisuals(Icons.Rounded.PhoneAndroid, WhispererPrimary, "Screen On")
        BehaviorEvent.TYPE_SCREEN_OFF -> EventVisuals(Icons.Rounded.MobileOff, TextMuted, "Screen Off")
        BehaviorEvent.TYPE_NOTIFICATION -> EventVisuals(Icons.Rounded.Notifications, WhispererAccent, "Notification")
        BehaviorEvent.TYPE_GEOFENCE_TRANSITION -> EventVisuals(Icons.Rounded.FmdGood, StatusActive, "Geofence")
        "RULE_EXECUTED" -> EventVisuals(Icons.Rounded.AutoAwesome, StatusProcessed, "AI Executed Rule")
        else -> EventVisuals(Icons.Rounded.DataObject, TextSecondary, eventType)
    }
}

private fun formatPayload(eventType: String, payload: String): String {
    return try {
        when (eventType) {
            BehaviorEvent.TYPE_SILENT_MODE -> {
                val mode = "\"ringerMode\":\"(\\w+)\"".toRegex().find(payload)?.groupValues?.get(1) ?: ""
                "Changed to $mode"
            }
            BehaviorEvent.TYPE_NOTIFICATION -> {
                val app = "\"appName\":\"([^\"]+)\"".toRegex().find(payload)?.groupValues?.get(1) ?: ""
                val action = "\"action\":\"(\\w+)\"".toRegex().find(payload)?.groupValues?.get(1) ?: ""
                "$app • $action"
            }
            BehaviorEvent.TYPE_GEOFENCE_TRANSITION -> {
                val label = "\"label\":\"([^\"]+)\"".toRegex().find(payload)?.groupValues?.get(1) ?: ""
                val transition = "\"transition\":\"(\\w+)\"".toRegex().find(payload)?.groupValues?.get(1) ?: ""
                "$transition at $label"
            }
            "RULE_EXECUTED" -> {
                val name = "\"ruleName\":\"([^\"]+)\"".toRegex().find(payload)?.groupValues?.get(1) ?: ""
                "Executed: $name"
            }
            else -> {
                val batteryMatch = "\"batteryLevel\":(\\d+)".toRegex().find(payload)
                if (batteryMatch != null) "Battery: ${batteryMatch.groupValues[1]}%" else payload.take(80)
            }
        }
    } catch (e: Exception) {
        payload.take(80)
    }
}

private fun isToday(timestamp: Long): Boolean {
    val today = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(timestamp: Long): Boolean {
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return yesterday.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}
