package com.bypass.ide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bypass.ide.model.ProjectInfo
import com.bypass.ide.model.WorkspaceNode
import com.bypass.ide.ui.theme.BadgeCss
import com.bypass.ide.ui.theme.BadgeHtml
import com.bypass.ide.ui.theme.BadgeJs
import com.bypass.ide.ui.theme.BadgeJson
import com.bypass.ide.ui.theme.BadgeKotlin
import com.bypass.ide.ui.theme.BadgeMarkdown
import com.bypass.ide.ui.theme.BadgePython
import com.bypass.ide.ui.theme.CyanElectric
import com.bypass.ide.ui.theme.CyanMuted
import com.bypass.ide.ui.theme.CyanSoft
import com.bypass.ide.ui.theme.EmeraldMatrix
import com.bypass.ide.ui.theme.ObsidianBg
import com.bypass.ide.ui.theme.ObsidianBorder
import com.bypass.ide.ui.theme.ObsidianSurface
import com.bypass.ide.ui.theme.ObsidianSurfaceElevated
import com.bypass.ide.ui.theme.ObsidianSurfaceHigh
import com.bypass.ide.ui.theme.RoseError
import com.bypass.ide.ui.theme.TextMuted
import com.bypass.ide.ui.theme.TextPrimary
import com.bypass.ide.ui.theme.TextSecondary

@Composable
fun FileExplorerView(
    activeProject: String,
    projects: List<ProjectInfo>,
    nodes: List<WorkspaceNode>,
    onSwitchProject: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onCreateFile: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onRenameNode: (oldPath: String, newName: String) -> Unit,
    onDeleteNode: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<WorkspaceNode?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Project Header & Actions
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box {
                        Surface(
                            onClick = { showProjectDropdown = true },
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianSurfaceElevated,
                            border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("project_selector_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Active Project",
                                    tint = CyanElectric,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = activeProject,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showProjectDropdown,
                            onDismissRequest = { showProjectDropdown = false }
                        ) {
                            projects.forEach { proj ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${proj.name} (${proj.fileCount} files)")
                                    },
                                    onClick = {
                                        onSwitchProject(proj.name)
                                        showProjectDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { showNewProjectDialog = true },
                            modifier = Modifier.testTag("new_project_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Create New Project",
                                tint = EmeraldMatrix
                            )
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("refresh_files_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh File Tree",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showNewFileDialog = true },
                        border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_new_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = CyanElectric,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New File", color = CyanSoft)
                    }
                    OutlinedButton(
                        onClick = { showNewFolderDialog = true },
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_new_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Folder", color = TextPrimary)
                    }
                }
            }
        }

        if (nodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No files in this project folder yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Create a file above or ask Bypass AI in Agent mode to build a project.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(nodes, key = { it.relativePath }) { node ->
                    WorkspaceNodeRow(
                        node = node,
                        onOpen = { if (!node.isDirectory) onOpenFile(node.relativePath) },
                        onRename = { renameTarget = node },
                        onDelete = { onDeleteNode(node.relativePath) }
                    )
                }
            }
        }
    }

    if (showNewProjectDialog) {
        SimpleNameInputDialog(
            title = "Create New Project",
            label = "Project folder name",
            initialValue = "",
            onDismiss = { showNewProjectDialog = false },
            onConfirm = { name ->
                onCreateProject(name)
                showNewProjectDialog = false
            }
        )
    }

    if (showNewFileDialog) {
        SimpleNameInputDialog(
            title = "Create New File",
            label = "Relative file path (e.g., utils.js)",
            initialValue = "",
            onDismiss = { showNewFileDialog = false },
            onConfirm = { path ->
                onCreateFile(path)
                showNewFileDialog = false
            }
        )
    }

    if (showNewFolderDialog) {
        SimpleNameInputDialog(
            title = "Create New Folder",
            label = "Relative folder path (e.g., components)",
            initialValue = "",
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { path ->
                onCreateFolder(path)
                showNewFolderDialog = false
            }
        )
    }

    renameTarget?.let { target ->
        SimpleNameInputDialog(
            title = "Rename ${target.name}",
            label = "New name",
            initialValue = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                onRenameNode(target.relativePath, newName)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun WorkspaceNodeRow(
    node: WorkspaceNode,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurfaceElevated,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (node.depth * 14).dp)
            .clickable(enabled = !node.isDirectory, onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (node.isDirectory) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Folder",
                    tint = CyanElectric,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                FileTypeBadge(extension = node.extension)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!node.isDirectory) {
                    Text(
                        text = "${node.sizeBytes} bytes • ${node.relativePath}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onRename,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DriveFileRenameOutline,
                    contentDescription = "Rename ${node.name}",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete ${node.name}",
                    tint = RoseError.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun FileTypeBadge(extension: String) {
    val (label, tint) = when (extension.lowercase()) {
        "html", "htm" -> "HTML" to BadgeHtml
        "css" -> "CSS" to BadgeCss
        "js", "mjs" -> "JS" to BadgeJs
        "kt", "kts" -> "KT" to BadgeKotlin
        "py" -> "PY" to BadgePython
        "json" -> "JSON" to BadgeJson
        "md" -> "MD" to BadgeMarkdown
        else -> extension.uppercase().take(4).ifBlank { "FILE" } to CyanSoft
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = tint.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SimpleNameInputDialog(
    title: String,
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = { Text(title, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (value.isNotBlank()) onConfirm(value.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanElectric,
                    contentColor = ObsidianBg
                )
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
