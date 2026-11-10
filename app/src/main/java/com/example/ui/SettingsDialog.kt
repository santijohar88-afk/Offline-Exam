package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferences
import com.example.ui.theme.SchoolGold
import com.example.ui.theme.SchoolGreenPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsDialog(
    currentPreferences: AppPreferences,
    onSaveTeacherPreferences: (haptic: Boolean, defaultClass: String, maxViolations: Int) -> Unit,
    onClearActiveExamState: () -> Unit,
    onDismiss: () -> Unit
) {
    var hapticEnabled by remember { mutableStateOf(currentPreferences.hapticFeedbackEnabled) }
    var selectedClassFilter by remember { mutableStateOf(currentPreferences.defaultClassFilter) }
    var maxViolations by remember { mutableIntStateOf(currentPreferences.maxViolationsAllowed) }
    var showConfirmClearExam by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("app_settings_dialog"),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = SchoolGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Preferences & DataStore",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Storage & Persistence Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = SchoolGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DataStore Local Persistence",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "• Active Exam Stored: ${if (currentPreferences.hasActiveExam) "Yes (${currentPreferences.activeExamId})" else "None (Idle)"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                        )

                        if (currentPreferences.lastStudentName.isNotBlank()) {
                            Text(
                                text = "• Student Profile: ${currentPreferences.lastStudentName} (${currentPreferences.lastStudentId}) - ${currentPreferences.lastStudentClass}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                            )
                        }

                        if (currentPreferences.lastBackupTimestamp > 0L) {
                            val formattedDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                .format(Date(currentPreferences.lastBackupTimestamp))
                            Text(
                                text = "• Last Backup Recorded: $formattedDate",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Haptic Feedback Preference
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Haptic Feedback",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Vibrate on scan, alert, or submission",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = { hapticEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SchoolGreenPrimary
                        ),
                        modifier = Modifier.testTag("switch_haptic_feedback")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Anti-Cheat Warning Threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Max Anti-Cheat Warnings",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Auto-submits exam when limit reached",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { if (maxViolations > 1) maxViolations-- },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("-")
                        }
                        Text(
                            text = "$maxViolations",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        OutlinedButton(
                            onClick = { if (maxViolations < 10) maxViolations++ },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Exam State Reset option
                if (currentPreferences.hasActiveExam) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Active Exam Session in Memory",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                            Text(
                                text = "Exam ID: ${currentPreferences.activeExamId} (${currentPreferences.activeExamTimeRemaining}s left)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showConfirmClearExam = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("clear_active_exam_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reset Active Exam State",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveTeacherPreferences(hapticEnabled, selectedClassFilter, maxViolations)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolGreenPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("save_settings_button")
            ) {
                Text(
                    text = "Save Preferences",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )

    if (showConfirmClearExam) {
        AlertDialog(
            onDismissRequest = { showConfirmClearExam = false },
            title = { Text("Reset Exam State?") },
            text = { Text("This will clear any unfinished exam session currently stored in local DataStore.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearActiveExamState()
                        showConfirmClearExam = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmClearExam = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
