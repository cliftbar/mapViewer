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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import site.cliftbar.mapviewer.tracks.LineStyle
import site.cliftbar.mapviewer.tracks.Track
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.platform.rememberFilePicker
import site.cliftbar.mapviewer.platform.rememberColorPicker
import kotlinx.coroutines.launch

class TrackManagementScreen(
    private val trackRepository: TrackRepository
) : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 1u,
                title = "Tracks"
            )
        }

    @Composable
    override fun Content() {
        val tracks = remember { mutableStateListOf<Track>() }
        val scope = rememberCoroutineScope()
        val filePicker = rememberFilePicker()
        var editingTrack by remember { mutableStateOf<Track?>(null) }

        LaunchedEffect(Unit) {
            tracks.clear()
            tracks.addAll(trackRepository.getAllTracks())
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Tracks", style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row {
                Button(onClick = {
                    scope.launch {
                        val content = filePicker.pickFile(listOf("gpx"))
                        content?.let {
                            trackRepository.importTrack(it, "gpx")?.let { track ->
                                tracks.add(track)
                            }
                        }
                    }
                }) {
                    Text("Import GPX")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(onClick = {
                    scope.launch {
                        val content = filePicker.pickFile(listOf("json", "geojson"))
                        content?.let {
                            trackRepository.importTrack(it, "geojson")?.let { track ->
                                tracks.add(track)
                            }
                        }
                    }
                }) {
                    Text("Import GeoJSON")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tracks) { track ->
                    TrackItem(
                        track = track,
                        onVisibilityChange = { visible ->
                            trackRepository.updateTrackVisibility(track.id, visible)
                            val index = tracks.indexOfFirst { it.id == track.id }
                            if (index != -1) {
                                tracks[index] = track.copy(visible = visible)
                            }
                        },
                        onEdit = { editingTrack = track },
                        onExport = {
                            val gpx = trackRepository.exportTrack(track, "gpx")
                            gpx?.let {
                                scope.launch {
                                    filePicker.saveFile("${track.name}.gpx", it)
                                }
                            }
                        },
                        onDelete = {
                            trackRepository.deleteTrack(track.id)
                            tracks.remove(track)
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
                    trackRepository.updateTrackStyle(track.id, color, style)
                    val index = tracks.indexOfFirst { it.id == track.id }
                    if (index != -1) {
                        tracks[index] = track.copy(color = color, lineStyle = style)
                    }
                    editingTrack = null
                }
            )
        }
    }

    @Composable
    private fun TrackItem(
        track: Track,
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
