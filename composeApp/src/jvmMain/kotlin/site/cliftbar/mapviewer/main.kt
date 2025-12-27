package site.cliftbar.mapviewer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import site.cliftbar.mapviewer.config.AppTheme
import site.cliftbar.mapviewer.config.ConfigRepository
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.db.createDriver

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    application {
        val database = MapViewerDB(createDriver())
        Window(
            onCloseRequest = ::exitApplication,
            title = "mapviewer",
        ) {
            val configRepository = remember { ConfigRepository(database) }
            val trackRepository = remember { TrackRepository(database) }
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

            App(database, configRepository, trackRepository)
        }
    }
}