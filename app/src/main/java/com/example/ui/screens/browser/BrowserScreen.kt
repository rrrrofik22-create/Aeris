package com.example.ui.screens.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.AerisCore
import com.example.data.local.entity.BrowserProfileEntity
import com.example.ui.theme.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    aerisCore: AerisCore
) {
    val coroutineScope = rememberCoroutineScope()
    val profilesFlow: Flow<List<BrowserProfileEntity>> = remember { aerisCore.browserProfileManager.allProfiles }
    val profiles: List<BrowserProfileEntity> by profilesFlow.collectAsState(initial = emptyList())
    val activeProfileId: String by aerisCore.browserProfileManager.activeProfileId.collectAsState()

    val currentProfile = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull()

    var urlInput by remember { mutableStateOf(currentProfile?.homeUrl ?: "https://www.google.com") }
    var currentLoadedUrl by remember { mutableStateOf(urlInput) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    var showProfileMenu by remember { mutableStateOf(false) }
    var showNewProfileDialog by remember { mutableStateOf(false) }
    var aiExtractedInfo by remember { mutableStateOf<String?>(null) }

    fun getPortalHtml(profileName: String): String = """
    <!DOCTYPE html>
    <html>
    <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
    body { background: #090D14; color: #F1F5F9; font-family: -apple-system, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 80vh; margin: 0; padding: 24px; text-align: center; }
    h1 { color: #00E5FF; font-size: 22px; margin-bottom: 6px; }
    .sub { color: #94A3B8; font-size: 12px; margin-bottom: 24px; }
    .card { background: #1E293B; border: 1px solid #334155; border-radius: 12px; padding: 16px; width: 100%; max-width: 380px; }
    .badge { background: rgba(168, 85, 247, 0.2); color: #C084FC; padding: 4px 8px; border-radius: 6px; font-size: 11px; font-weight: bold; }
    .links { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-top: 12px; }
    a { background: rgba(0, 229, 255, 0.12); color: #00E5FF; padding: 6px 12px; border-radius: 8px; font-size: 12px; text-decoration: none; border: 1px solid rgba(0, 229, 255, 0.3); }
    </style>
    </head>
    <body>
    <h1>AERIS AI BROWSER</h1>
    <div class="sub">Active Session: <span class="badge">$profileName</span></div>
    <div class="card">
    <div style="font-size: 11px; color: #64748B; font-weight: bold;">FEATURED AI SITES & SEARCH</div>
    <div class="links">
    <a href="https://en.m.wikipedia.org">Wikipedia</a>
    <a href="https://news.ycombinator.com">Hacker News</a>
    <a href="https://github.com">GitHub</a>
    <a href="https://duckduckgo.com">DuckDuckGo</a>
    </div>
    </div>
    </body>
    </html>
    """.trimIndent()

    fun loadTarget(webView: WebView, targetUrl: String, profileName: String) {
        if (targetUrl == "about:blank" || targetUrl.isBlank() || targetUrl == "aeris://home") {
            webView.loadDataWithBaseURL("https://aeris.browser.local/", getPortalHtml(profileName), "text/html", "UTF-8", null)
        } else {
            val clean = if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                "https://$targetUrl"
            } else targetUrl
            webView.loadUrl(clean)
        }
    }

    LaunchedEffect(currentProfile?.id) {
        if (currentProfile != null) {
            urlInput = currentProfile.homeUrl
            webViewInstance?.let { webView ->
                loadTarget(webView, currentProfile.homeUrl, currentProfile.name)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AerisDarkBackground)
            .testTag("browser_screen_container")
    ) {
        // Browser Controls Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AerisDarkSurfaceVariant,
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                // Top row: Profile Selector & AI Automation Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Switcher Button
                    Box {
                        Surface(
                            onClick = { showProfileMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            color = AerisObsidian,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AerisCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("browser_profile_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = AerisCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = currentProfile?.name ?: "Profile A",
                                    color = AerisTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AerisTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false },
                            modifier = Modifier.background(AerisDarkSurfaceVariant)
                        ) {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = profile.name,
                                            color = if (profile.id == activeProfileId) AerisCyan else AerisTextPrimary,
                                            fontWeight = if (profile.id == activeProfileId) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        aerisCore.browserProfileManager.selectActiveProfile(profile.id)
                                        showProfileMenu = false
                                    }
                                )
                            }
                            HorizontalDivider(color = AerisDarkOutline)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = AerisViolet, modifier = Modifier.size(16.dp))
                                        Text("New Browser Profile", color = AerisViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    showProfileMenu = false
                                    showNewProfileDialog = true
                                }
                            )
                        }
                    }

                    // AI Screen / Page Understand Action
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                webViewInstance?.evaluateJavascript("document.title + ' | ' + document.body.innerText.substring(0, 500)") { result ->
                                    val clean = result?.replace("\"", "") ?: "No page text extracted."
                                    aiExtractedInfo = clean
                                    aerisCore.processUserInput("I am looking at this web page: $currentLoadedUrl\nExtracted summary: $clean\nPlease analyze.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AerisViolet),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("browser_ai_extract_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Extract & Reason", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom row: Navigation Buttons and URL bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack) AerisCyan else AerisTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) AerisCyan else AerisTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AerisTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // URL TextField
                    TextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("browser_url_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AerisObsidian,
                            unfocusedContainerColor = AerisObsidian,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = AerisTextPrimary,
                            unfocusedTextColor = AerisTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    webViewInstance?.let { webView ->
                                        loadTarget(webView, urlInput, currentProfile?.name ?: "Profile A")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Navigate",
                                    tint = AerisCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = AerisCyan,
                trackColor = AerisObsidian
            )
        }

        // Extracted Info Notification
        AnimatedVisibility(visible = aiExtractedInfo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AerisViolet.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Page context forwarded to Aeris Core memory.",
                    color = AerisViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { aiExtractedInfo = null }, modifier = Modifier.size(20.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = AerisViolet, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Real Multi-Profile Android WebView
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.userAgentString = currentProfile?.userAgent ?: settings.userAgentString

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let {
                                    currentLoadedUrl = it
                                    urlInput = it
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                try {
                                    (view?.parent as? android.view.ViewGroup)?.removeView(view)
                                    view?.destroy()
                                } catch (_: Exception) {}
                                webViewInstance = null
                                return true
                            }
                        }

                        webViewInstance = this
                        loadTarget(this, urlInput, currentProfile?.name ?: "Profile A")
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("browser_webview")
            )
        }
    }

    if (showNewProfileDialog) {
        var newProfileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewProfileDialog = false },
            title = { Text("Create Browser Profile", color = AerisTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. Profile D (Social)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            coroutineScope.launch {
                                val id = aerisCore.browserProfileManager.createProfile(newProfileName.trim())
                                aerisCore.browserProfileManager.selectActiveProfile(id)
                                showNewProfileDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AerisCyan)
                ) {
                    Text("Create Profile", color = AerisDarkOnPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProfileDialog = false }) {
                    Text("Cancel", color = AerisTextMuted)
                }
            },
            containerColor = AerisDarkSurfaceVariant
        )
    }
}
