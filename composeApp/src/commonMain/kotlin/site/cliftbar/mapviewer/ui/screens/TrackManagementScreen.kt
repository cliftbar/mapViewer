package site.cliftbar.mapviewer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.LazyListScope
import site.cliftbar.mapviewer.tracks.Folder
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import site.cliftbar.mapviewer.tracks.LineStyle
import site.cliftbar.mapviewer.tracks.Track
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.platform.rememberFilePicker
import site.cliftbar.mapviewer.platform.rememberColorPicker
import mapviewer.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlinx.coroutines.launch

import cafe.adriel.voyager.core.model.rememberScreenModel
import site.cliftbar.mapviewer.ui.viewmodels.TrackManagementScreenModel

class TrackManagementScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 1u,
                title = "Tracks"
            )
        }

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    override fun Content() {
        val trackRepository = site.cliftbar.mapviewer.LocalTrackRepository.current
        val screenModel = rememberScreenModel { 
            TrackManagementScreenModel(trackRepository)
        }
        
        LaunchedEffect(Unit) {
            screenModel.refreshTracks()
        }

        val scope = rememberCoroutineScope()
        val filePicker = rememberFilePicker()
        var editingTrack by remember { mutableStateOf<Track?>(null) }
        var bulkEditingStyle by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Tracks", style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (screenModel.selectedTrackIds.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${screenModel.selectedTrackIds.size} selected", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    
                    var showFolderAssignment by remember { mutableStateOf(false) }

                    IconButton(onClick = { showFolderAssignment = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Add to Folder")
                    }

                    if (showFolderAssignment) {
                        FolderAssignmentDialog(
                            folders = screenModel.folders,
                            onDismiss = { showFolderAssignment = false },
                            onAssign = { folderId ->
                                screenModel.addSelectedTracksToFolder(folderId)
                                showFolderAssignment = false
                            },
                            onCreateAndAssign = { name ->
                                val repository = trackRepository
                                scope.launch {
                                    val newId = repository.createFolder(name, null)
                                    screenModel.addSelectedTracksToFolder(newId)
                                    showFolderAssignment = false
                                }
                            }
                        )
                    }

                    IconButton(onClick = { screenModel.updateSelectedTracksVisibility(true) }) {
                        Icon(Icons.Default.Visibility, contentDescription = "Make Visible")
                    }
                    IconButton(onClick = { screenModel.updateSelectedTracksVisibility(false) }) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "Hide")
                    }
                    IconButton(onClick = { bulkEditingStyle = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Bulk Style")
                    }
                    IconButton(onClick = { screenModel.deleteSelectedTracks() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                    }
                    TextButton(onClick = { screenModel.clearSelection() }) {
                        Text("Clear")
                    }
                }
            }

            Row {
                Button(onClick = {
                    scope.launch {
                        println("[DEBUG_LOG] Pick GPX started")
                        val content = filePicker.pickFile(listOf("gpx"))
                        println("[DEBUG_LOG] Pick GPX finished, content length: ${content?.length ?: "null"}")
                        content?.let {
                            try {
                                val imported = screenModel.importTrack(it, "gpx")
                                println("[DEBUG_LOG] Import GPX result: ${imported.size} tracks")
                            } catch (e: Exception) {
                                println("[DEBUG_LOG] Crash during GPX import: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    }
                }) {
                    Text("Import GPX")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(onClick = {
                    scope.launch {
                        println("[DEBUG_LOG] Pick GeoJSON started")
                        val content = filePicker.pickFile(listOf("json", "geojson"))
                        println("[DEBUG_LOG] Pick GeoJSON finished, content length: ${content?.length ?: "null"}")
                        content?.let {
                            try {
                                val imported = screenModel.importTrack(it, "geojson")
                                println("[DEBUG_LOG] Import GeoJSON result: ${imported.size} tracks")
                            } catch (e: Exception) {
                                println("[DEBUG_LOG] Crash during GeoJSON import: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    }
                }) {
                    Text("Import GeoJSON")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                fun LazyListScope.renderFolder(folder: Folder, level: Int = 0) {
                    item(key = "folder-${folder.id}") {
                        FolderItem(
                            folder = folder,
                            onDelete = { screenModel.deleteFolder(folder.id) },
                            onRename = { /* TODO */ },
                            onMove = { /* TODO */ },
                            onAddSelected = { screenModel.addSelectedTracksToFolder(folder.id) },
                            onRemoveSelected = { screenModel.removeSelectedTracksFromFolder(folder.id) },
                            level = level
                        )
                    }
                    
                    // Tracks in this folder
                    items(screenModel.tracks.filter { it.id in folder.trackIds }, key = { "track-${folder.id}-${it.id}" }) { track ->
                        val isSelected = screenModel.selectedTrackIds[track.id] ?: false
                        Row(modifier = Modifier.padding(start = ((level + 1) * 16).dp)) {
                            TrackItem(
                                track = track,
                                isSelected = isSelected,
                                onToggleSelection = { screenModel.toggleSelection(track.id) },
                                onVisibilityChange = { visible -> screenModel.updateTrackVisibility(track.id, visible) },
                                onEdit = { editingTrack = track },
                                onExport = {
                                    screenModel.exportTrack(track, "gpx") { result ->
                                        result?.let { scope.launch { filePicker.saveFile("${track.name}.gpx", it) } }
                                    }
                                },
                                onDelete = { screenModel.deleteTrack(track.id) }
                            )
                        }
                    }

                    folder.subFolders.forEach { subFolder ->
                        renderFolder(subFolder, level + 1)
                    }
                }

                screenModel.folders.forEach { folder ->
                    renderFolder(folder)
                }

                item {
                    Text(
                        "All Tracks",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(screenModel.tracks, key = { "track-root-${it.id}" }) { track ->
                    val isSelected = screenModel.selectedTrackIds[track.id] ?: false
                    TrackItem(
                        track = track,
                        isSelected = isSelected,
                        onToggleSelection = { screenModel.toggleSelection(track.id) },
                        onVisibilityChange = { visible ->
                            screenModel.updateTrackVisibility(track.id, visible)
                        },
                        onEdit = { editingTrack = track },
                        onExport = {
                            screenModel.exportTrack(track, "gpx") { result ->
                                result?.let {
                                    scope.launch {
                                        filePicker.saveFile("${track.name}.gpx", it)
                                    }
                                }
                            }
                        },
                        onDelete = {
                            screenModel.deleteTrack(track.id)
                        }
                    )
                }
            }
        }

        editingTrack?.let { track ->
            TrackEditDialog(
                track = track,
                onDismiss = { editingTrack = null },
                onSave = { color, style ->
                    screenModel.updateTrackStyle(track.id, color, style)
                    editingTrack = null
                }
            )
        }

        if (bulkEditingStyle) {
            TrackEditDialog(
                track = Track("", "Bulk Edit", emptyList()), // Dummy track for initial values
                onDismiss = { bulkEditingStyle = false },
                onSave = { color, style ->
                    screenModel.updateSelectedTracksStyle(color, style)
                    bulkEditingStyle = false
                }
            )
        }
    }

    @Composable
    private fun FolderItem(
        folder: Folder,
        onDelete: () -> Unit,
        onRename: (String) -> Unit,
        onMove: (String?) -> Unit,
        onAddSelected: () -> Unit,
        onRemoveSelected: () -> Unit,
        level: Int = 0
    ) {
        var expanded by remember { mutableStateOf(false) }

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .padding(start = (level * 16).dp)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null
                )
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    folder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Folder Actions")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Add Selected Tracks") },
                            onClick = { 
                                // This is slightly tricky as we don't have screenModel here
                                // But we can pass an onAddSelected action
                                onAddSelected(); showMenu = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove Selected Tracks") },
                            onClick = { onRemoveSelected(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { /* TODO */ showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { onDelete(); showMenu = false }
                        )
                    }
                }
            }

            if (expanded) {
                folder.subFolders.forEach { subFolder ->
                    FolderItem(
                        folder = subFolder,
                        onDelete = { /* TODO */ },
                        onRename = { /* TODO */ },
                        onMove = { /* TODO */ },
                        onAddSelected = onAddSelected,
                        onRemoveSelected = onRemoveSelected,
                        level = level + 1
                    )
                }
                
                // Note: tracks in folders will be handled differently or we need to pass tracks here
            }
        }
    }

    @Composable
    private fun FolderAssignmentDialog(
        folders: List<Folder>,
        onDismiss: () -> Unit,
        onAssign: (String) -> Unit,
        onCreateAndAssign: (String) -> Unit
    ) {
        var newFolderName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add to Folder") },
            text = {
                Column {
                    Text("Select an existing folder:")
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(folders) { folder ->
                            ListItem(
                                headlineContent = { Text(folder.name) },
                                modifier = Modifier.clickable { onAssign(folder.id) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Or create a new one:")
                    TextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { if (newFolderName.isNotBlank()) onCreateAndAssign(newFolderName) },
                    enabled = newFolderName.isNotBlank()
                ) {
                    Text("Create & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    private fun TrackItem(
        track: Track,
        isSelected: Boolean,
        onToggleSelection: () -> Unit,
        onVisibilityChange: (Boolean) -> Unit,
        onEdit: () -> Unit,
        onExport: () -> Unit,
        onDelete: () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )

            Checkbox(
                checked = track.visible,
                onCheckedChange = onVisibilityChange
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(track.name, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(2.dp)
                            .background(parseColor(track.color))
                    )
                    Text(
                        " ${track.lineStyle.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Style")
            }
            
            IconButton(onClick = onExport) {
                Icon(Icons.Default.Download, contentDescription = "Export GPX")
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }

    @Composable
    private fun TrackEditDialog(
        track: Track,
        onDismiss: () -> Unit,
        onSave: (String, LineStyle) -> Unit
    ) {
        var selectedColor by remember { mutableStateOf(track.color) }
        var selectedStyle by remember { mutableStateOf(track.lineStyle) }
        val colorPicker = rememberColorPicker()

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Track Style") },
            text = {
                Column {
                    Text("Color", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colors = listOf("#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#000000")
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(4.dp)
                                    .background(parseColor(color))
                                    .let {
                                        if (selectedColor == color) {
                                            it.border(2.dp, MaterialTheme.colorScheme.primary)
                                        } else it
                                    }
                                    .clickable { selectedColor = color }
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    colorPicker.pickColor(parseColor(selectedColor)) { color ->
                                        selectedColor = colorToHex(color)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Custom Color",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Line Style", style = MaterialTheme.typography.labelLarge)
                    Column {
                        LineStyle.values().forEach { style ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedStyle = style }
                            ) {
                                RadioButton(selected = selectedStyle == style, onClick = { selectedStyle = style })
                                Text(style.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onSave(selectedColor, selectedStyle) }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    private fun parseColor(hex: String): androidx.compose.ui.graphics.Color {
        return try {
            val colorStr = if (hex.startsWith("#")) hex.substring(1) else hex
            val color = hexToLong(colorStr)
            androidx.compose.ui.graphics.Color(color or 0xFF000000)
        } catch (e: Exception) {
            androidx.compose.ui.graphics.Color.Blue
        }
    }

    private fun hexToLong(hex: String): Long {
        return hex.toLong(16)
    }

    private fun colorToHex(color: androidx.compose.ui.graphics.Color): String {
        val r = (color.red * 255).toInt().toString(16).padStart(2, '0')
        val g = (color.green * 255).toInt().toString(16).padStart(2, '0')
        val b = (color.blue * 255).toInt().toString(16).padStart(2, '0')
        return "#${r}${g}${b}".uppercase()
    }
}
