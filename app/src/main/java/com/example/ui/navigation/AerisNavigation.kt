package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class AerisWorkspace(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Hub, Icons.Outlined.Hub),
    AUTO("auto", "Auto", Icons.Filled.ElectricBolt, Icons.Outlined.ElectricBolt),
    EDITOR("editor", "Editor", Icons.Filled.Code, Icons.Outlined.Code),
    PREVIEW("preview", "Preview", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline),
    BROWSER("browser", "Browser", Icons.Filled.Language, Icons.Outlined.Language),
    FILES("files", "Files", Icons.Filled.Folder, Icons.Outlined.FolderOpen);

    companion object {
        const val SETTINGS_ROUTE = "settings"
    }
}
