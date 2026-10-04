package com.example.ui.screens.files

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.core.AerisCore
import com.example.data.local.entity.ProjectEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun FilesScreen(
    aerisCore: AerisCore,
    onOpenInEditor: (String) -> Unit,
    onOpenInPreview: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val projectsFlow = remember { aerisCore.projectDao.getAllProjects() }
    val projects by projectsFlow.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects else projects.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AerisDarkBackground)
            .padding(16.dp)
            .testTag("files_screen_container")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Project & File Storage",
                    color = AerisTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${projects.size} persistent projects in Aeris workspace",
                    color = AerisTextSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AerisCyan),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("create_project_button"),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null, tint = AerisDarkOnPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Project", color = AerisDarkOnPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search projects, web apps, tools...", color = AerisTextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = AerisTextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("files_search_field"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AerisDarkSurfaceVariant,
                unfocusedContainerColor = AerisDarkSurfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = AerisTextPrimary,
                unfocusedTextColor = AerisTextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Projects List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredProjects, key = { it.id }) { project ->
                ProjectCard(
                    project = project,
                    onOpenEditor = {
                        aerisCore.setActiveProject(project.id)
                        onOpenInEditor(project.id)
                    },
                    onOpenPreview = {
                        aerisCore.setActiveProject(project.id)
                        onOpenInPreview(project.id)
                    },
                    onDelete = {
                        coroutineScope.launch {
                            aerisCore.projectDao.deleteProject(project.id)
                            aerisCore.projectDao.deleteFilesForProject(project.id)
                        }
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("WEB") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Project", color = AerisTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project Name") },
                        placeholder = { Text("e.g. Cyber Matrix Visualizer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            coroutineScope.launch {
                                val id = "proj_${UUID.randomUUID().toString().take(8)}"
                                val project = ProjectEntity(
                                    id = id,
                                    name = name.trim(),
                                    description = desc.trim(),
                                    projectType = type
                                )
                                aerisCore.projectDao.insertProject(project)
                                aerisCore.toolSystem.executeTool(
                                    "create_project_file",
                                    mapOf(
                                        "project_id" to id,
                                        "file_path" to "index.html",
                                        "content" to "<!-- $name -->\n<!DOCTYPE html><html><head><title>$name</title></head><body><h1>$name</h1></body></html>"
                                    )
                                )
                                aerisCore.setActiveProject(id)
                                showCreateDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AerisCyan)
                ) {
                    Text("Create", color = AerisDarkOnPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = AerisTextMuted)
                }
            },
            containerColor = AerisDarkSurfaceVariant
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectEntity,
    onOpenEditor: () -> Unit,
    onOpenPreview: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = AerisDarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AerisDarkOutline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (project.projectType) {
                            "THREE_JS" -> Icons.Default.ViewInAr
                            "WEB" -> Icons.Default.Html
                            else -> Icons.Default.Code
                        },
                        contentDescription = null,
                        tint = AerisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = project.name,
                        color = AerisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenEditor,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Code", tint = AerisCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onOpenPreview,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Preview", tint = AerisEmerald, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AerisRose, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = project.description,
                color = AerisTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AerisDarkPrimaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = project.projectType, color = AerisCyan, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }

                Text(
                    text = "Aeris Context Preserved",
                    color = AerisTextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
