package com.phonewhisperer.presentation.screens.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phonewhisperer.presentation.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 6 })

    // Helper functions for checking permissions (should ideally be hoisted, but keeping it concise here)
    fun hasLocation() = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    fun hasCalendar() = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
    fun hasUsageStats(): Boolean {
        val appOps = context.getSystemService(android.app.AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
    fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(context.packageName) == true
    }
    fun hasDnd() = context.getSystemService(android.app.NotificationManager::class.java).isNotificationPolicyAccessGranted

    // Launchers
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasLocation()) scope.launch { pagerState.animateScrollToPage(2) }
    }
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (hasCalendar()) scope.launch { pagerState.animateScrollToPage(5) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (page) {
                    0 -> OnboardingPage(
                        icon = Icons.Rounded.Psychology,
                        title = "Welcome to PhoneWhisperer",
                        subtitle = "Your privacy-first, on-device AI agent. To learn your routines and automate your life, it needs a few permissions. Your data NEVER leaves your phone.",
                        iconColor = WhispererPrimary,
                        actionText = "Get Started",
                        onAction = { scope.launch { pagerState.animateScrollToPage(1) } }
                    )
                    1 -> OnboardingPage(
                        icon = Icons.Rounded.LocationOn,
                        title = "Location Access",
                        subtitle = "Allows the AI to learn places you visit (like Home or Work) to trigger location-based rules.",
                        iconColor = StatusActive,
                        actionText = "Grant Location",
                        isGranted = hasLocation(),
                        onAction = {
                            val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            locationLauncher.launch(perms.toTypedArray())
                        },
                        onNext = { scope.launch { pagerState.animateScrollToPage(2) } }
                    )
                    2 -> OnboardingPage(
                        icon = Icons.Rounded.Apps,
                        title = "Usage Stats",
                        subtitle = "Allows the AI to see which apps you use to understand your digital habits.",
                        iconColor = WhispererSecondary,
                        actionText = "Open Settings",
                        isGranted = hasUsageStats(),
                        onAction = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                        onNext = { scope.launch { pagerState.animateScrollToPage(3) } }
                    )
                    3 -> OnboardingPage(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Notification Listener",
                        subtitle = "Allows the AI to track notification volume and auto-dismiss annoying alerts.",
                        iconColor = WhispererAccent,
                        actionText = "Open Settings",
                        isGranted = isNotificationListenerEnabled(),
                        onAction = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        onNext = { scope.launch { pagerState.animateScrollToPage(4) } }
                    )
                    4 -> OnboardingPage(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "Calendar Access",
                        subtitle = "Allows the AI to sync with your schedule and mute your phone during meetings.",
                        iconColor = WhispererPrimary,
                        actionText = "Grant Calendar",
                        isGranted = hasCalendar(),
                        onAction = { calendarLauncher.launch(Manifest.permission.READ_CALENDAR) },
                        onNext = { scope.launch { pagerState.animateScrollToPage(5) } }
                    )
                    5 -> OnboardingPage(
                        icon = Icons.Rounded.DoNotDisturbOn,
                        title = "Do Not Disturb Access",
                        subtitle = "Allows the AI to automatically mute your phone when a rule is triggered.",
                        iconColor = StatusWarning,
                        actionText = "Open Settings",
                        isGranted = hasDnd(),
                        onAction = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) },
                        onNext = {
                            viewModel.completeOnboarding()
                            onComplete()
                        }
                    )
                }
            }
        }

        // Page Indicators
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(6) { iteration ->
                val color = if (pagerState.currentPage == iteration) WhispererPrimary else DarkSurfaceVariant
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: androidx.compose.ui.graphics.Color,
    actionText: String,
    isGranted: Boolean = false,
    onAction: () -> Unit,
    onNext: (() -> Unit)? = null
) {
    Icon(icon, null, modifier = Modifier.size(96.dp), tint = if (isGranted) StatusActive else iconColor)
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(48.dp))

    if (isGranted) {
        Button(
            onClick = { onNext?.invoke() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StatusActive),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Granted! Continue", style = MaterialTheme.typography.titleMedium)
        }
    } else {
        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = iconColor),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(actionText, style = MaterialTheme.typography.titleMedium)
        }
        if (onNext != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { onNext.invoke() }) {
                Text("Skip for Now", color = TextSecondary)
            }
        }
    }
}
