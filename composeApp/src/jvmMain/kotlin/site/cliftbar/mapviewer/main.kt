package site.cliftbar.mapviewer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

import site.cliftbar.mapviewer.db.createDriver

fun main() = application {
    val database = MapViewerDB(createDriver())
    Window(
        onCloseRequest = ::exitApplication,
        title = "mapviewer",
    ) {
        App(database)
    }
}