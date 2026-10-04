package com.example

import android.app.Application
import com.example.core.AerisCore
import com.example.core.ai.ApiKeyManager
import com.example.core.ai.GroqApiClient
import com.example.core.ai.ModelRouter
import com.example.core.automation.ActionRouter
import com.example.core.automation.ActionVerifier
import com.example.core.automation.IntentEngine
import com.example.core.automation.ShizukuEngine
import com.example.core.browser.BrowserProfileManager
import com.example.core.memory.ContextManager
import com.example.core.planner.TaskPlanner
import com.example.core.tools.AgentToolSystem
import com.example.core.voice.VoiceManager
import com.example.data.local.AerisDatabase
import com.example.data.local.entity.ApiKeyEntity
import com.example.data.local.entity.BrowserProfileEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectFileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class AerisApplication : Application() {

    companion object {
        init {
            try {
                android.system.Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                android.system.Os.setenv("GALLIUM_DRIVER", "llvmpipe", true)
                android.system.Os.setenv("MESA_LOADER_DRIVER_OVERRIDE", "swrast", true)
                android.system.Os.setenv("EGL_LOG_LEVEL", "fatal", true)
            } catch (_: Throwable) {}
        }
    }

    lateinit var database: AerisDatabase
        private set
    lateinit var modelRouter: ModelRouter
        private set
    lateinit var apiKeyManager: ApiKeyManager
        private set
    lateinit var groqApiClient: GroqApiClient
        private set
    lateinit var contextManager: ContextManager
        private set
    lateinit var intentEngine: IntentEngine
        private set
    lateinit var shizukuEngine: ShizukuEngine
        private set
    lateinit var actionVerifier: ActionVerifier
        private set
    lateinit var actionRouter: ActionRouter
        private set
    lateinit var toolSystem: AgentToolSystem
        private set
    lateinit var taskPlanner: TaskPlanner
        private set
    lateinit var voiceManager: VoiceManager
        private set
    lateinit var browserProfileManager: BrowserProfileManager
        private set
    lateinit var aerisCore: AerisCore
        private set

    override fun onCreate() {
        super.onCreate()

        database = AerisDatabase.getInstance(this)
        modelRouter = ModelRouter()
        apiKeyManager = ApiKeyManager(database.apiKeyDao())
        groqApiClient = GroqApiClient(apiKeyManager, modelRouter)
        contextManager = ContextManager(database.memoryDao(), database.chatDao(), database.projectDao())
        intentEngine = IntentEngine(this)
        shizukuEngine = ShizukuEngine(this)
        actionVerifier = ActionVerifier()
        actionRouter = ActionRouter(this, intentEngine, shizukuEngine, actionVerifier)
        toolSystem = AgentToolSystem(actionRouter, contextManager, database.projectDao())
        taskPlanner = TaskPlanner(toolSystem, database.taskDao())
        voiceManager = VoiceManager(this, groqApiClient)
        browserProfileManager = BrowserProfileManager(database.browserProfileDao())

        aerisCore = AerisCore(
            modelRouter = modelRouter,
            apiKeyManager = apiKeyManager,
            groqApiClient = groqApiClient,
            contextManager = contextManager,
            toolSystem = toolSystem,
            taskPlanner = taskPlanner,
            actionRouter = actionRouter,
            intentEngine = intentEngine,
            shizukuEngine = shizukuEngine,
            voiceManager = voiceManager,
            browserProfileManager = browserProfileManager,
            database = database,
            chatDao = database.chatDao(),
            projectDao = database.projectDao()
        )

        // Seed default initial data asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            seedDefaultData()
        }
    }

    private suspend fun seedDefaultData() {
        // 1. Seed default chat session if empty
        val sessions = database.chatDao().getAllSessions().firstOrNull()
        if (sessions.isNullOrEmpty()) {
            database.chatDao().insertSession(
                ChatSessionEntity(
                    id = "session_default",
                    title = "Primary Workspace Session",
                    summary = "Initial Aeris AI Operating Session"
                )
            )
        }

        // 2. Seed default browser profiles if empty
        val profiles = database.browserProfileDao().getAllProfiles().firstOrNull()
        if (profiles.isNullOrEmpty()) {
            database.browserProfileDao().insertProfile(
                BrowserProfileEntity(
                    id = "profile_work",
                    name = "Profile A (Work & Research)",
                    isDefault = true,
                    homeUrl = "https://www.google.com"
                )
            )
            database.browserProfileDao().insertProfile(
                BrowserProfileEntity(
                    id = "profile_dev",
                    name = "Profile B (Development & Sandbox)",
                    isDefault = false,
                    homeUrl = "https://threejs.org"
                )
            )
            database.browserProfileDao().insertProfile(
                BrowserProfileEntity(
                    id = "profile_personal",
                    name = "Profile C (Automation & Social)",
                    isDefault = false,
                    homeUrl = "https://news.ycombinator.com"
                )
            )
        }

        // 3. Seed starter project in Editor
        val starterProjId = "project_threejs_demo"
        val starterHtml = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Aeris Neural Nexus</title>
    <style>
        body { margin: 0; overflow: hidden; background: #090D14; font-family: sans-serif; color: #00E5FF; }
        #canvas-container { width: 100vw; height: 100vh; position: absolute; }
        #hud { position: absolute; top: 16px; left: 16px; pointer-events: none; z-index: 10; }
        .badge { background: rgba(0, 229, 255, 0.1); border: 1px solid #00E5FF; padding: 6px 12px; border-radius: 8px; font-size: 12px; }
    </style>
</head>
<body>
    <div id="hud"><span class="badge">AERIS ENGINE • NEURAL NEXUS</span></div>
    <div id="canvas-container"></div>
    <script>
        const container = document.getElementById('canvas-container');
        function initCanvas() {
            const canvas = document.createElement('canvas');
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;
            container.appendChild(canvas);
            const ctx = canvas.getContext('2d');
            let angle = 0;
            const particles = [];
            for (let i = 0; i < 40; i++) {
                particles.push({
                    x: Math.random() * canvas.width,
                    y: Math.random() * canvas.height,
                    vx: (Math.random() - 0.5) * 1.5,
                    vy: (Math.random() - 0.5) * 1.5,
                    radius: Math.random() * 2 + 1
                });
            }

            function render() {
                ctx.fillStyle = '#090D14';
                ctx.fillRect(0, 0, canvas.width, canvas.height);
                const cx = canvas.width / 2;
                const cy = canvas.height / 2;

                // Central Glowing Core
                const gradient = ctx.createRadialGradient(cx, cy, 5, cx, cy, 70);
                gradient.addColorStop(0, 'rgba(168, 85, 247, 0.9)');
                gradient.addColorStop(0.5, 'rgba(0, 229, 255, 0.4)');
                gradient.addColorStop(1, 'rgba(9, 13, 20, 0)');
                ctx.fillStyle = gradient;
                ctx.beginPath();
                ctx.arc(cx, cy, 70, 0, Math.PI * 2);
                ctx.fill();

                // Holographic orbital rings
                ctx.strokeStyle = '#00E5FF';
                ctx.lineWidth = 1.5;
                for (let r = 0; r < 3; r++) {
                    ctx.beginPath();
                    ctx.ellipse(cx, cy, 90 + r * 35, 35 + r * 15, angle + r * 0.8, 0, Math.PI * 2);
                    ctx.stroke();
                }

                // Neural particles
                ctx.fillStyle = '#00E5FF';
                particles.forEach(p => {
                    p.x += p.vx;
                    p.y += p.vy;
                    if (p.x < 0 || p.x > canvas.width) p.vx *= -1;
                    if (p.y < 0 || p.y > canvas.height) p.vy *= -1;
                    ctx.beginPath();
                    ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
                    ctx.fill();
                });

                angle += 0.015;
                requestAnimationFrame(render);
            }
            render();

            window.addEventListener('resize', () => {
                canvas.width = window.innerWidth;
                canvas.height = window.innerHeight;
            });
        }
        initCanvas();
    </script>
</body>
</html>"""

        val projects = database.projectDao().getAllProjects().firstOrNull()
        if (projects.isNullOrEmpty()) {
            database.projectDao().insertProject(
                ProjectEntity(
                    id = starterProjId,
                    name = "Three.js Neural Nexus",
                    description = "Interactive 3D particle sphere and holographic nodes built with Three.js",
                    projectType = "THREE_JS"
                )
            )
            database.projectDao().insertFile(
                ProjectFileEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = starterProjId,
                    filePath = "index.html",
                    fileName = "index.html",
                    fileExtension = "html",
                    content = starterHtml,
                    language = "html"
                )
            )
        } else {
            // Migrate any existing files from previous runs that contain unsafe WebGLRenderer
            val existingFiles = database.projectDao().getFilesForProject("project_threejs_demo").firstOrNull() ?: emptyList()
            for (file in existingFiles) {
                if (file.content.contains("WebGLRenderer")) {
                    database.projectDao().updateFile(file.copy(content = starterHtml))
                }
            }
        }
    }
}
