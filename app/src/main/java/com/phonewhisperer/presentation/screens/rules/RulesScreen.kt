package com.phonewhisperer.presentation.screens.rules

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phonewhisperer.data.local.db.entity.AutomationRuleEntity
import com.phonewhisperer.presentation.theme.*

@Composable
fun RulesScreen(viewModel: RulesViewModel = hiltViewModel()) {
    val pendingRules by viewModel.pendingRules.collectAsState()
    val approvedRules by viewModel.approvedRules.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp, start = 20.dp, end = 20.dp)
    ) {
        // Header
        Text("AI Suggestions", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Text("Routines learned from your behavior", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        
        Spacer(Modifier.height(24.dp))

        // Custom Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkCard),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton(
                title = "Pending (${pendingRules.size})",
                isSelected = selectedTab == 0,
                modifier = Modifier.weight(1f)
            ) { selectedTab = 0 }
            
            TabButton(
                title = "Active (${approvedRules.size})",
                isSelected = selectedTab == 1,
                modifier = Modifier.weight(1f)
            ) { selectedTab = 1 }
        }

        Spacer(Modifier.height(24.dp))

        // Content
        AnimatedContent(targetState = selectedTab, label = "tab_content") { tab ->
            if (tab == 0) {
                if (pendingRules.isEmpty()) {
                    EmptyState(Icons.Rounded.AutoAwesome, "No new patterns detected yet.", "Keep using your phone normally. The AI needs a few weeks of data to find routines.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    EmptyState(Icons.Rounded.CheckCircle, "No active rules.", "Approve suggestions from the Pending tab to activate them.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(approvedRules, key = { it.id }) { rule ->
                            ActiveRuleCard(rule = rule)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(title: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) WhispererPrimary else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else TextSecondary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        )
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
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(WhispererAccent.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getIconForAction(rule.actionType), contentDescription = null, tint = WhispererAccent)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(rule.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("Proposed automation", style = MaterialTheme.typography.labelSmall, color = WhispererAccent)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(rule.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(24.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Dismiss", color = TextSecondary)
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhispererPrimary)
                ) {
                    Text("Approve")
                }
            }
        }
    }
}

@Composable
private fun ActiveRuleCard(rule: AutomationRuleEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(StatusActive.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getIconForAction(rule.actionType), contentDescription = null, tint = StatusActive)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(rule.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("Active", style = MaterialTheme.typography.labelSmall, color = StatusActive)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(rule.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
