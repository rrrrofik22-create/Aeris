package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.AerisWorkspace
import com.example.ui.theme.*

@Composable
fun AerisBottomBar(
    currentWorkspace: AerisWorkspace,
    onWorkspaceSelected: (AerisWorkspace) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("aeris_bottom_navigation"),
        containerColor = AerisSurface,
        tonalElevation = 6.dp
    ) {
        AerisWorkspace.entries.forEach { workspace ->
            val isSelected = currentWorkspace == workspace
            NavigationBarItem(
                selected = isSelected,
                onClick = { onWorkspaceSelected(workspace) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) workspace.selectedIcon else workspace.unselectedIcon,
                        contentDescription = workspace.title
                    )
                },
                label = {
                    Text(
                        text = workspace.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AerisCyan,
                    selectedTextColor = AerisCyan,
                    indicatorColor = AerisDarkPrimaryContainer.copy(alpha = 0.7f),
                    unselectedIconColor = AerisTextMuted,
                    unselectedTextColor = AerisTextMuted
                ),
                modifier = Modifier.testTag("nav_${workspace.route}")
            )
        }
    }
}
