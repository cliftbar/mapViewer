package site.cliftbar.mapviewer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import site.cliftbar.mapviewer.config.AppTheme
import site.cliftbar.mapviewer.config.ConfigRepository
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.db.createDriver

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    application {
        var database by remember { mutableStateOf<MapViewerDB?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        
        LaunchedEffect(Unit) {
            try {
                database = MapViewerDB(createDriver())
            } catch (e: Exception) {
                println("[DEBUG_LOG] Database initialization failed: ${e.message}")
                e.printStackTrace()
                error = e.message ?: "Unknown error"
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "mapviewer",
        ) {
            if (database != null) {
                val db = database!!
                val configRepository = remember { ConfigRepository(db) }
                val trackRepository = remember { TrackRepository(db) }
                val config by configRepository.activeConfig.collectAsState()

                val darkTheme = when (config.theme) {
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                    AppTheme.LIGHT -> false
                    AppTheme.DARK -> true
                }

                LaunchedEffect(darkTheme) {
                    val os = System.getProperty("os.name").lowercase()
                    if (os.contains("mac")) {
                        val appearance = if (darkTheme) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
                        window.rootPane.putClientProperty("apple.awt.appearance", appearance)
                    }
                }

                App(db, configRepository, trackRepository)
            } else if (error != null) {
                Text("Error: $error")
            } else {
                Text("Loading database...")
            }
        }
    }
}