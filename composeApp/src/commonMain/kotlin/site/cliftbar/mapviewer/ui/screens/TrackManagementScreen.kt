package site.cliftbar.mapviewer.ui.screens

import androidx.compose.foundation.layout.Box
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
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.platform.rememberFilePicker
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
        val tracks = remember { mutableStateListOf<site.cliftbar.mapviewer.tracks.Track>() }
        val scope = rememberCoroutineScope()
        val filePicker = rememberFilePicker()

        LaunchedEffect(Unit) {
            tracks.addAll(trackRepository.getAllTracks())
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Tracks", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(track.name, modifier = Modifier.weight(1f))
                        
                        TextButton(onClick = {
                            val gpx = trackRepository.exportTrack(track, "gpx")
                            gpx?.let {
                                scope.launch {
                                    filePicker.saveFile("${track.name}.gpx", it)
                                }
                            }
                        }) {
                            Text("GPX")
                        }
                        
                        IconButton(onClick = {
                            trackRepository.deleteTrack(track.id)
                            tracks.remove(track)
                        }) {
                            Text("X")
                        }
                    }
                }
            }
        }
    }
}
