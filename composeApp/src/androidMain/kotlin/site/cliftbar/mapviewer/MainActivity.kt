package site.cliftbar.mapviewer

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import site.cliftbar.mapviewer.config.AppTheme
import site.cliftbar.mapviewer.config.ConfigRepository
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.db.AndroidDriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            var database by remember { mutableStateOf<MapViewerDB?>(null) }
            
            LaunchedEffect(Unit) {
                database = MapViewerDB(AndroidDriverFactory(this@MainActivity).createDriver())
            }

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
                App(db, configRepository, trackRepository)
                AndroidSystemBars(darkTheme)
            }
        }
    }
}

@Composable
private fun AndroidSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as Activity).window
        val statusBarColor = if (darkTheme) darkColorScheme().surface else lightColorScheme().surface
        window.statusBarColor = statusBarColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    // App() // Needs database now
}
