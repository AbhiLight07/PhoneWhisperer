package com.phonewhisperer

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phonewhisperer.presentation.navigation.MainScreen
import com.phonewhisperer.presentation.screens.dashboard.DashboardScreen
import com.phonewhisperer.presentation.theme.PhoneWhispererTheme
import com.phonewhisperer.presentation.theme.StatusActive
import com.phonewhisperer.presentation.theme.TextSecondary
import com.phonewhisperer.presentation.theme.WhispererPrimary
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions result handled — UI recomposes automatically
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PhoneWhispererTheme(darkTheme = true) {
                var showOnboarding by remember { mutableStateOf(true) }

                if (showOnboarding) {
                    OnboardingScreen(
                        onRequestLocationPermission = { requestLocationPermissions() },
                        onRequestUsageStatsPermission = { openUsageStatsSettings() },
                        onRequestCalendarPermission = { requestCalendarPermission() },
                        onRequestNotificationPermission = { requestNotificationPermission() },
                        onRequestNotificationListenerPermission = { openNotificationListenerSettings() },
                        isNotificationListenerEnabled = { isNotificationListenerEnabled() },
                        onComplete = { showOnboarding = false }
                    )
                } else {
                    MainScreen()
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        locationPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestCalendarPermission() {
        locationPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    private fun openUsageStatsSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    /**
     * Opens the Notification Listener settings where the user must manually
     * enable PhoneWhisperer to receive notification events.
     */
    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    /**
     * Checks if our NotificationListenerService is enabled.
     * Reads from the secure setting where Android stores enabled listener packages.
     */
    private fun isNotificationListenerEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }
}

@Composable
private fun OnboardingScreen(
    onRequestLocationPermission: () -> Unit,
    onRequestUsageStatsPermission: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestNotificationListenerPermission: () -> Unit,
    isNotificationListenerEnabled: () -> Boolean,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Psychology, null, modifier = Modifier.size(72.dp), tint = WhispererPrimary)
        Spacer(Modifier.height(16.dp))
        Text("PhoneWhisperer", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Grant permissions to start learning your routine", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        PermissionButton("Location Access", "Track places you visit", Icons.Rounded.LocationOn) { onRequestLocationPermission() }
        Spacer(Modifier.height(12.dp))
        PermissionButton("Usage Stats", "See which apps you use", Icons.Rounded.Apps) { onRequestUsageStatsPermission() }
        Spacer(Modifier.height(12.dp))
        PermissionButton("Calendar", "Read your schedule", Icons.Rounded.CalendarMonth) { onRequestCalendarPermission() }
        Spacer(Modifier.height(12.dp))
        PermissionButton("Notifications", "Show collection status", Icons.Rounded.Notifications) { onRequestNotificationPermission() }
        Spacer(Modifier.height(12.dp))

        // Phase 2: Notification Listener
        val listenerEnabled = isNotificationListenerEnabled()
        PermissionButton(
            title = "Notification Listener",
            subtitle = if (listenerEnabled) "✓ Enabled — tracking notifications" else "Track which apps notify you",
            icon = Icons.Rounded.NotificationsActive,
            isGranted = listenerEnabled
        ) { onRequestNotificationListenerPermission() }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WhispererPrimary)
        ) { Text("Start Observing", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun PermissionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean = false,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(14.dp),
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (isGranted) StatusActive else WhispererPrimary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = if (isGranted) StatusActive else TextSecondary)
            }
            Icon(
                if (isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.ChevronRight,
                null,
                tint = if (isGranted) StatusActive else TextSecondary
            )
        }
    }
}
