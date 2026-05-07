package com.phonewhisperer.presentation.screens.rules

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.presentation.theme.*
import kotlinx.coroutines.delay

@Composable
fun RulesScreen(viewModel: RulesViewModel = hiltViewModel()) {
    val pendingRules by viewModel.pendingRules.collectAsState()
    val approvedRules by viewModel.approvedRules.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp, start = 20.dp, end = 20.dp)
    ) {
        // Header with gradient accent
        AnimatedVisibility(visible, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.AutoAwesome, null,
                        modifier = Modifier.size(32.dp),
                        tint = WhispererAccent
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("AI Suggestions", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text("Routines learned from your behavior", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Custom Tab Row with animation
        AnimatedVisibility(visible, enter = fadeIn(tween(800, 200)) + slideInVertically(tween(600, 200)) { 40 }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(DarkCard),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabButton(
                    title = "Pending",
                    count = pendingRules.size,
                    isSelected = selectedTab == 0,
                    accentColor = WhispererAccent,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = 0 }

                TabButton(
                    title = "Active",
                    count = approvedRules.size,
                    isSelected = selectedTab == 1,
                    accentColor = StatusActive,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = 1 }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Content
        AnimatedContent(
            targetState = selectedTab,
            label = "tab_content",
            transitionSpec = {
                fadeIn(tween(300)) + slideInHorizontally(tween(300)) { if (targetState > initialState) 100 else -100 } togetherWith
                fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { if (targetState > initialState) -100 else 100 }
            }
        ) { tab ->
            if (tab == 0) {
                if (pendingRules.isEmpty()) {
                    EmptyState(
                        Icons.Rounded.AutoAwesome,
                        "No new patterns detected yet",
                        "Keep using your phone normally. The AI needs a few weeks of data to find routines.\n\nTip: Tap the header on the Dashboard to simulate 3 weeks of data."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(pendingRules, key = { it.id }) { rule ->
                            PendingRuleCard(
                                rule = rule,
                                onApprove = { viewModel.approveRule(rule.id) },
                                onReject = { viewModel.rejectRule(rule.id) }
                            )
                        }
                    }
                }
            } else {
                if (approvedRules.isEmpty()) {
                    EmptyState(
                        Icons.Rounded.CheckCircle,
                        "No active rules",
                        "Approve suggestions from the Pending tab to activate them."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(approvedRules, key = { it.id }) { rule ->
                            ActiveRuleCard(
                                rule = rule,
                                onTestExecute = {
                                    val success = viewModel.testExecuteRule(rule)
                                    Toast.makeText(
                                        context,
                                        if (success) "✓ ${rule.name} executed!" else "✗ Execution failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onRevoke = { viewModel.revokeRule(rule.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "tab_scale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isSelected) Brush.horizontalGradient(
                    listOf(accentColor, accentColor.copy(alpha = 0.7f))
                ) else Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.Transparent)
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = if (isSelected) Color.White else TextSecondary,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            )
            if (count > 0) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(0.25f) else DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingRuleCard(rule: AutomationRuleEntity, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulsing icon for pending state
                val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                    initialValue = 0.9f, targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                    label = "pulse_scale"
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(WhispererAccent.copy(0.3f), WhispererAccent.copy(0.05f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getIconForAction(rule.actionType), null, tint = WhispererAccent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(rule.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(12.dp), tint = WhispererAccent)
                        Spacer(Modifier.width(4.dp))
                        Text("Trigger: ${rule.triggerValue}", style = MaterialTheme.typography.labelSmall, color = WhispererAccent)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(rule.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(StatusError.copy(0.3f), StatusError.copy(0.1f)))
                    )
                ) {
                    Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp), tint = StatusError.copy(0.7f))
                    Spacer(Modifier.width(6.dp))
                    Text("Dismiss", color = TextSecondary)
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhispererPrimary)
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Approve")
                }
            }
        }
    }
}

@Composable
private fun ActiveRuleCard(
    rule: AutomationRuleEntity,
    onTestExecute: () -> Unit,
    onRevoke: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(StatusActive.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getIconForAction(rule.actionType), null, tint = StatusActive, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(rule.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusActive)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Active • ${rule.triggerType}: ${rule.triggerValue}", style = MaterialTheme.typography.labelSmall, color = StatusActive)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(rule.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(20.dp))

            // Action buttons: Test Execute + Revoke
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRevoke,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.RemoveCircleOutline, null, modifier = Modifier.size(16.dp), tint = StatusError.copy(0.7f))
                    Spacer(Modifier.width(6.dp))
                    Text("Revoke", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onTestExecute,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusActive)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Test Now", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val pulse by rememberInfiniteTransition(label = "empty_pulse").animateFloat(
            initialValue = 0.8f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "empty_pulse_scale"
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(DarkCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(40.dp), tint = TextMuted)
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

private fun getIconForAction(actionType: String): ImageVector {
    return when (actionType) {
        "RINGER_MODE" -> Icons.Rounded.VolumeOff
        "DND" -> Icons.Rounded.DoNotDisturbOn
        "NOTIFICATION_BLOCK" -> Icons.Rounded.NotificationsOff
        else -> Icons.Rounded.AutoAwesome
    }
}
