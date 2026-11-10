package com.example.bridge

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class WebAppInterface(
    private val context: Context,
    private val onFileSaved: (fileName: String, fileUri: Uri, mimeType: String) -> Unit,
    private val preferencesRepository: AppPreferencesRepository? = null,
    var onRequestCameraPermission: (() -> Unit)? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    @JavascriptInterface
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    @JavascriptInterface
    fun requestCameraPermission() {
        (context as? Activity)?.runOnUiThread {
            onRequestCameraPermission?.invoke()
        }
    }

    // --- DataStore Persistence Methods ---

    @JavascriptInterface
    fun persistExamProgress(
        examId: String,
        studentName: String,
        studentId: String,
        studentClass: String,
        answersJson: String,
        timeRemaining: Long,
        endTime: Long,
        violations: Int,
        bookmarksJson: String,
        seed: Long,
        startedAt: String
    ) {
        preferencesRepository?.saveExamProgressAsync(
            examId = examId,
            studentName = studentName,
            studentId = studentId,
            studentClass = studentClass,
            answersJson = answersJson,
            timeRemaining = timeRemaining,
            endTime = endTime,
            violations = violations,
            bookmarksJson = bookmarksJson,
            seed = seed,
            startedAt = startedAt
        )
    }

    @JavascriptInterface
    fun clearExamProgress() {
        preferencesRepository?.clearExamProgressAsync()
    }

    @JavascriptInterface
    fun getPersistedExamProgress(): String {
        return preferencesRepository?.getActiveExamProgressJson() ?: "{}"
    }

    @JavascriptInterface
    fun persistStudentProfile(name: String, id: String, className: String) {
        scope.launch {
            preferencesRepository?.updateStudentProfile(name, id, className)
        }
    }

    @JavascriptInterface
    fun getPersistedStudentProfile(): String {
        return preferencesRepository?.getStudentProfileJson() ?: "{}"
    }

    @JavascriptInterface
    fun recordBackup() {
        scope.launch {
            preferencesRepository?.recordBackupTimestamp()
        }
    }

    // --- Native Actions ---

    @JavascriptInterface
    fun share(title: String?, text: String?, url: String?) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            if (!title.isNullOrEmpty()) {
                putExtra(Intent.EXTRA_SUBJECT, title)
            }
            val body = buildString {
                if (!text.isNullOrEmpty()) append(text)
                if (!url.isNullOrEmpty()) {
                    if (isNotEmpty()) append("\n\n")
                    append(url)
                }
            }
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val chooser = Intent.createChooser(shareIntent, title ?: "Share Result")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    @JavascriptInterface
    fun vibrate(durationMs: Long) {
        try {
            val safeDuration = durationMs.coerceIn(20L, 1000L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(safeDuration, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(safeDuration, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(safeDuration)
                }
            }
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun showToast(message: String?) {
        if (!message.isNullOrBlank()) {
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun saveFile(fileName: String, base64Data: String, mimeType: String) {
        (context as? Activity)?.runOnUiThread {
            try {
                val cleanFileName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                val dataBytes = Base64.decode(base64Data, Base64.DEFAULT)

                var savedUri: Uri? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, cleanFileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(dataBytes)
                        }
                        savedUri = uri
                    }
                }

                if (savedUri == null) {
                    // Fallback to app-specific external files dir
                    val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.filesDir
                    val file = File(downloadsDir, cleanFileName)
                    FileOutputStream(file).use { it.write(dataBytes) }
                    savedUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }

                savedUri?.let { uri ->
                    onFileSaved(cleanFileName, uri, mimeType)
                } ?: run {
                    Toast.makeText(context, "Saved $cleanFileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
