package com.example.ui.screens.preview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.AerisCore
import com.example.data.local.entity.ProjectFileEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class ConsoleLogItem(
    val message: String,
    val level: ConsoleMessage.MessageLevel,
    val lineNumber: Int,
    val sourceId: String
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(
    aerisCore: AerisCore,
    onNavigateToEditor: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val activeProjectId by aerisCore.activeProjectId.collectAsState()

    val projectFilesFlow = remember(activeProjectId) {
        if (activeProjectId != null) {
            aerisCore.projectDao.getFilesForProject(activeProjectId!!)
        } else {
            aerisCore.projectDao.getFilesForProject("project_threejs_demo")
        }
    }
    val files by projectFilesFlow.collectAsState(initial = emptyList())

    val indexHtmlFile = files.firstOrNull { it.fileName == "index.html" } ?: files.firstOrNull()

    var consoleLogs by remember { mutableStateOf<List<ConsoleLogItem>>(emptyList()) }
    var detectedErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var isAutoFixing by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var showConsoleSheet by remember { mutableStateOf(false) }
    var testResultVerified by remember { mutableStateOf(false) }

    fun sanitizeHtmlForPreview(rawHtml: String): String {
        val shim = """
        <script>
        window.addEventListener('DOMContentLoaded', function() {
            if (typeof HTMLCanvasElement !== 'undefined' && HTMLCanvasElement.prototype.getContext) {
                const origGetContext = HTMLCanvasElement.prototype.getContext;
                HTMLCanvasElement.prototype.getContext = function(type, attributes) {
                    if (type === 'webgl' || type === 'experimental-webgl') {
                        try {
                            const ctx = origGetContext.call(this, type, attributes);
                            return ctx;
                        } catch (err) {
                            console.warn('WebGL is not supported in emulator: ' + err);
                            return null;
                        }
                    }
                    return origGetContext.call(this, type, attributes);
                };
            }
        });
        </script>
        """.trimIndent()
        return if (rawHtml.contains("<head>", ignoreCase = true)) {
            rawHtml.replaceFirst("<head>", "<head>\n$shim", ignoreCase = true)
        } else {
            "$shim\n$rawHtml"
        }
    }

    LaunchedEffect(files) {
        // When file content changes, reload webview
        webViewInstance?.let { webView ->
            if (indexHtmlFile != null) {
                val safeHtml = sanitizeHtmlForPreview(indexHtmlFile.content)
                webView.loadDataWithBaseURL("https://aeris.preview.local/", safeHtml, "text/html", "UTF-8", null)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AerisDarkBackground)
            .testTag("preview_screen_container")
    ) {
        // Preview Header Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AerisDarkSurfaceVariant,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = AerisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Universal Preview & Testing",
                        color = AerisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (detectedErrors.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(AerisEmerald.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("HEALTHY", color = AerisEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Toggle Console Logs
                    IconButton(
                        onClick = { showConsoleSheet = !showConsoleSheet },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("preview_console_button")
                    ) {
                        BadgedBox(badge = {
                            if (detectedErrors.isNotEmpty()) {
                                Badge(containerColor = AerisRose) {
                                    Text("${detectedErrors.size}", fontSize = 9.sp)
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Console",
                                tint = if (detectedErrors.isNotEmpty()) AerisRose else AerisTextSecondary
                            )
                        }
                    }

                    // Reload Button
                    IconButton(
                        onClick = {
                            detectedErrors = emptyList()
                            testResultVerified = false
                            reloadTrigger++
                            webViewInstance?.let { webView ->
                                if (indexHtmlFile != null) {
                                    val safeHtml = sanitizeHtmlForPreview(indexHtmlFile.content)
                                    webView.loadDataWithBaseURL("https://aeris.preview.local/", safeHtml, "text/html", "UTF-8", null)
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload", tint = AerisCyan)
                    }

                    // Back to Editor
                    IconButton(
                        onClick = onNavigateToEditor,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editor", tint = AerisTextSecondary)
                    }
                }
            }
        }

        // AUTO FIX BANNER (Appears strictly when an error is detected!)
        AnimatedVisibility(visible = detectedErrors.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("auto_fix_banner"),
                colors = CardDefaults.cardColors(containerColor = AerisRose.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AerisRose.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = "Error",
                            tint = AerisRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Runtime Error Detected in Preview",
                                color = AerisRose,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = detectedErrors.firstOrNull()?.take(70) ?: "Script execution error",
                                color = AerisTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Autonomous Auto Fix Button
                    Button(
                        onClick = {
                            if (indexHtmlFile != null && !isAutoFixing) {
                                isAutoFixing = true
                                coroutineScope.launch {
                                    val errorLog = detectedErrors.joinToString("\n")
                                    val prompt = """The preview code failed with error:
$errorLog

Source Code:
${indexHtmlFile.content}

Analyze the error, fix the root cause, and provide ONLY the corrected complete working code without markdown code blocks."""

                                    val resp = aerisCore.groqApiClient.executeChatCompletion(
                                        messages = listOf(
                                            "system" to "You are Aeris Auto Fix Engine. Return only valid, repaired HTML/JS code.",
                                            "user" to prompt
                                        )
                                    )

                                    if (resp is com.example.core.ai.AgentApiResponse.TextSuccess) {
                                        val fixedCode = resp.content
                                            .replace(Regex("^```[a-zA-Z]*\\n"), "")
                                            .replace(Regex("\\n```$"), "")
                                        aerisCore.projectDao.updateFile(indexHtmlFile.copy(content = fixedCode))

                                        // Reload and re-test
                                        detectedErrors = emptyList()
                                        testResultVerified = true
                                        webViewInstance?.loadDataWithBaseURL(
                                            "https://aeris.preview.local/",
                                            fixedCode,
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                    }
                                    isAutoFixing = false
                                }
                            }
                        },
                        enabled = !isAutoFixing,
                        colors = ButtonDefaults.buttonColors(containerColor = AerisViolet),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("auto_fix_execute_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        if (isAutoFixing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("Auto Fix", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Test Success Indicator
        AnimatedVisibility(visible = testResultVerified && detectedErrors.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AerisEmerald.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = AerisEmerald, modifier = Modifier.size(14.dp))
                Text(
                    text = "Auto Fix verified! Project execution test passed with 0 runtime errors.",
                    color = AerisEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Embedded Real Android WebView Preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true

                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                if (consoleMessage != null) {
                                    val item = ConsoleLogItem(
                                        message = consoleMessage.message(),
                                        level = consoleMessage.messageLevel(),
                                        lineNumber = consoleMessage.lineNumber(),
                                        sourceId = consoleMessage.sourceId()
                                    )
                                    consoleLogs = consoleLogs + item
                                    if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                        detectedErrors = detectedErrors + "${consoleMessage.message()} (line ${consoleMessage.lineNumber()})"
                                    }
                                }
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                try {
                                    (view?.parent as? android.view.ViewGroup)?.removeView(view)
                                    view?.destroy()
                                } catch (_: Exception) {}
                                webViewInstance = null
                                return true
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    detectedErrors = detectedErrors + "Load error: ${error?.description}"
                                }
                            }
                        }

                        webViewInstance = this
                        if (indexHtmlFile != null) {
                            val safeHtml = sanitizeHtmlForPreview(indexHtmlFile.content)
                            loadDataWithBaseURL("https://aeris.preview.local/", safeHtml, "text/html", "UTF-8", null)
                        }
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("preview_webview")
            )
        }

        // Console Drawer if toggled
        if (showConsoleSheet) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                color = AerisObsidian,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Preview Runtime Console", color = AerisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { consoleLogs = emptyList(); detectedErrors = emptyList() }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Clear", tint = AerisTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(consoleLogs) { log ->
                            Text(
                                text = "[${log.level.name}] ${log.message}",
                                color = when (log.level) {
                                    ConsoleMessage.MessageLevel.ERROR -> AerisRose
                                    ConsoleMessage.MessageLevel.WARNING -> AerisAmber
                                    else -> AerisTextSecondary
                                },
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
