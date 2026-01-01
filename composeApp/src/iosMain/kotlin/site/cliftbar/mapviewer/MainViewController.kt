package site.cliftbar.mapviewer

import androidx.compose.ui.window.ComposeUIViewController

import androidx.compose.runtime.*
import site.cliftbar.mapviewer.config.ConfigRepository
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.db.createDriver

fun MainViewController() = ComposeUIViewController {
    var database by remember { mutableStateOf<MapViewerDB?>(null) }
    
    LaunchedEffect(Unit) {
        database = MapViewerDB(createDriver())
    }

    if (database != null) {
        val db = database!!
        val configRepository = remember { ConfigRepository(db) }
        val trackRepository = remember { TrackRepository(db) }
        App(db, configRepository, trackRepository)
    }
}