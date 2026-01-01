package site.cliftbar.mapviewer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.material3.Text
import site.cliftbar.mapviewer.db.createInMemoryDriver
import site.cliftbar.mapviewer.config.ConfigRepository
import site.cliftbar.mapviewer.tracks.TrackRepository
import androidx.compose.runtime.*

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("MapViewer") {
        var database by remember { mutableStateOf<MapViewerDB?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            try {
                database = MapViewerDB(createInMemoryDriver())
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            }
        }
        
        if (database != null) {
            val db = database!!
            val configRepository = remember { ConfigRepository(db) }
            val trackRepository = remember { TrackRepository(db) }
            App(db, configRepository, trackRepository)
        } else if (error != null) {
            Text("MapViewer Web - Error: $error")
        } else {
            Text("Loading database...")
        }
    }
}