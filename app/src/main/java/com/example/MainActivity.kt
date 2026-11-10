package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppPreferences
import com.example.data.AppPreferencesRepository
import com.example.ui.ExamWebView
import com.example.ui.SchoolInfoDialog
import com.example.ui.SettingsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SchoolGold
import com.example.ui.theme.SchoolGreenDark
import com.example.ui.theme.SchoolGreenPrimary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

data class SavedFileInfo(
    val fileName: String,
    val fileUri: Uri,
    val mimeType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val preferencesRepository = remember { AppPreferencesRepository(context) }
    val appPrefs by preferencesRepository.preferencesFlow.collectAsStateWithLifecycle(initialValue = AppPreferences())

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showSchoolInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCameraRationaleDialog by remember { mutableStateOf(false) }
    var savedFileInfo by remember { mutableStateOf<SavedFileInfo?>(null) }
    var pendingCameraCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    // Activity runtime camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Camera permission granted")
            }
            pendingCameraCallback?.invoke(true)
        } else {
            showCameraRationaleDialog = true
            pendingCameraCallback?.invoke(false)
        }
        pendingCameraCallback = null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_app_scaffold"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showSchoolInfoDialog = true }
                    ) {
                        // School Seal Emblem in Header
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.5.dp, SchoolGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_seal_logo),
                                contentDescription = "EKIII Logo",
                                modifier = Modifier.size(34.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "EKIII Exam Hub",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            )
                            Text(
                                text = "Eastern Kutawato Islamic Inst. Inc.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SchoolGold,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            webViewRef?.reload()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Reloaded")
                            }
                        },
                        modifier = Modifier.testTag("action_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Page",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    "EKIII Offline Mobile Examination System"
                                )
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "EKIII - Offline Mobile Examination System for Eastern Kutawato Islamic Institute Inc. (Buluan, Maguindanao)."
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share App"))
                        },
                        modifier = Modifier.testTag("action_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share App",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("action_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Preferences & Storage",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { showSchoolInfoDialog = true },
                        modifier = Modifier.testTag("action_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About School & App",
                            tint = SchoolGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SchoolGreenDark,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        ExamWebView(
            modifier = Modifier.padding(innerPadding),
            onFileSaved = { fileName, fileUri, mimeType ->
                savedFileInfo = SavedFileInfo(fileName, fileUri, mimeType)
            },
            preferencesRepository = preferencesRepository,
            onRequestCameraPermission = { onResult ->
                val isGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (isGranted) {
                    onResult(true)
                } else {
                    pendingCameraCallback = onResult
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onWebViewCreated = { webView ->
                webViewRef = webView
            }
        )
    }

    // Camera Permission Rationale & Settings Dialog
    if (showCameraRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showCameraRationaleDialog = false },
            modifier = Modifier.testTag("camera_permission_dialog"),
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = SchoolGreenPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Camera Permission Needed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Camera access is needed to scan student examination QR codes offline. You can grant permission now or open App Settings if it was previously denied.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraRationaleDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolGreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("grant_camera_permission_button")
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            showCameraRationaleDialog = false
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Could not open Settings", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Text("Settings")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = { showCameraRationaleDialog = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("cancel_camera_dialog_button")
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // School Info Dialog
    if (showSchoolInfoDialog) {
        SchoolInfoDialog(
            onDismiss = { showSchoolInfoDialog = false }
        )
    }

    // Settings & DataStore Persistence Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentPreferences = appPrefs,
            onSaveTeacherPreferences = { haptic, defaultClass, maxViolations ->
                coroutineScope.launch {
                    preferencesRepository.updateTeacherPreferences(haptic, defaultClass, maxViolations)
                    snackbarHostState.showSnackbar("Preferences saved to DataStore")
                }
            },
            onClearActiveExamState = {
                coroutineScope.launch {
                    preferencesRepository.clearActiveExamProgress()
                    webViewRef?.evaluateJavascript("if (typeof clearProgress === 'function') clearProgress();", null)
                    snackbarHostState.showSnackbar("Active exam state cleared from DataStore")
                }
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // File Saved Success Dialog
    savedFileInfo?.let { fileInfo ->
        AlertDialog(
            onDismissRequest = { savedFileInfo = null },
            modifier = Modifier.testTag("file_saved_dialog"),
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SchoolGreenPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "File Saved Successfully",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Saved to Downloads:",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = fileInfo.fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "You can share this file with your teacher via Bluetooth, Nearby Share, or Messenger.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = fileInfo.mimeType
                            putExtra(Intent.EXTRA_STREAM, fileInfo.fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(shareIntent, "Share ${fileInfo.fileName}"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open share chooser: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        savedFileInfo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolGreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("share_saved_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share File")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(fileInfo.fileUri, fileInfo.mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(viewIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No app found to open this file format.", Toast.LENGTH_SHORT).show()
                            }
                            savedFileInfo = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_saved_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = { savedFileInfo = null },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dismiss_saved_file_button")
                    ) {
                        Text("Done")
                    }
                }
            }
        )
    }
}
