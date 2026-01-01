package site.cliftbar.mapviewer

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
                App(db, configRepository, trackRepository)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    // App() // Needs database now
}