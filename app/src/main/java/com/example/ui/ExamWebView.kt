package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.bridge.WebAppInterface

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExamWebView(
    modifier: Modifier = Modifier,
    onFileSaved: (fileName: String, fileUri: Uri, mimeType: String) -> Unit,
    preferencesRepository: com.example.data.AppPreferencesRepository? = null,
    onRequestCameraPermission: ((onResult: (Boolean) -> Unit) -> Unit)? = null,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // File chooser callback holder
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    // Fallback in-composable permission launcher
    val localCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingPermissionRequest?.let { req ->
            if (isGranted) {
                req.grant(req.resources)
            } else {
                req.deny()
            }
            pendingPermissionRequest = null
        }
    }

    // File selection launcher for HTML <input type="file">
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results = WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
            filePathCallback?.onReceiveValue(results)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    // Handle Android system back gesture / button
    BackHandler(enabled = true) {
        val wv = webViewInstance
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            // Check if active exam is running to warn or allow normal exit
            wv?.evaluateJavascript(
                "Boolean(state && state.activeExam && !document.getElementById('view-exam-room').classList.contains('hidden'))"
            ) { inExam ->
                if (inExam == "true") {
                    wv.evaluateJavascript("openSubmitConfirmModal();", null)
                } else {
                    (context as? Activity)?.finish()
                }
            } ?: (context as? Activity)?.finish()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("exam_webview_container")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("exam_webview"),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Configure WebSettings for optimal offline performance
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    // Register JavaScript Interface for native capabilities
                    val bridge = WebAppInterface(
                        context = ctx,
                        onFileSaved = onFileSaved,
                        preferencesRepository = preferencesRepository,
                        onRequestCameraPermission = {
                            if (onRequestCameraPermission != null) {
                                onRequestCameraPermission { granted ->
                                    if (granted) {
                                        post {
                                            evaluateJavascript("if (typeof openQRScanner === 'function') openQRScanner();", null)
                                        }
                                    }
                                }
                            } else {
                                localCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                    addJavascriptInterface(bridge, "AndroidBridge")

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            if (newProgress >= 90) {
                                isLoading = false
                            }
                        }

                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallbackParam: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = filePathCallbackParam

                            try {
                                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "*/*"
                                }
                                filePickerLauncher.launch(intent)
                                return true
                            } catch (e: Exception) {
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = null
                                return false
                            }
                        }

                        override fun onPermissionRequest(request: PermissionRequest?) {
                            if (request == null) return
                            val hasVideo = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                            if (hasVideo) {
                                val isCameraGranted = ContextCompat.checkSelfPermission(
                                    ctx,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (isCameraGranted) {
                                    request.grant(request.resources)
                                } else {
                                    pendingPermissionRequest = request
                                    if (onRequestCameraPermission != null) {
                                        onRequestCameraPermission { granted ->
                                            if (granted) {
                                                pendingPermissionRequest?.grant(pendingPermissionRequest?.resources)
                                            } else {
                                                pendingPermissionRequest?.deny()
                                            }
                                            pendingPermissionRequest = null
                                        }
                                    } else {
                                        localCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            } else {
                                request.grant(request.resources)
                            }
                        }

                        override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                            super.onPermissionRequestCanceled(request)
                            pendingPermissionRequest = null
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            // Keep internal asset urls within WebView
                            return if (url.startsWith("file:///android_asset/")) {
                                false
                            } else {
                                // Launch external links in default browser
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    ctx.startActivity(intent)
                                    true
                                } catch (_: Exception) {
                                    false
                                }
                            }
                        }
                    }

                    // Load the bundled offline HTML app
                    loadUrl("file:///android_asset/index.html")

                    webViewInstance = this
                    onWebViewCreated(this)
                }
            },
            update = { webView ->
                webViewInstance = webView
            }
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("webview_loading_indicator")
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
        }
    }
}
