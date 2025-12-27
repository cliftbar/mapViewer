package site.cliftbar.mapviewer

import androidx.compose.ui.window.ComposeUIViewController

import androidx.compose.runtime.remember
import site.cliftbar.mapviewer.db.createDriver

fun MainViewController() = ComposeUIViewController {
    val database = remember { MapViewerDB(createDriver()) }
    App(database)
}