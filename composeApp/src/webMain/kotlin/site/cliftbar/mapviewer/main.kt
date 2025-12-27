package site.cliftbar.mapviewer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // In a real app, we'd setup the web worker driver here.
    // For now, let's at least make it compile.
    // val database = MapViewerDB(createDriver())
    
    ComposeViewport {
        // App(database)
    }
}