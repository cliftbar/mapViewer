package site.cliftbar.mapviewer.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import site.cliftbar.mapviewer.MapViewerDB

actual fun createDriver(): SqlDriver {
    throw RuntimeException("Android requires Context to create SqlDriver. Use AndroidDriverFactory.")
}

class AndroidDriverFactory(private val context: Context) {
    fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(MapViewerDB.Schema, context, "mapviewer.db")
    }
}

actual fun createInMemoryDriver(): SqlDriver {
    throw RuntimeException("Android requires Context for in-memory driver. Not implemented for commonTest.")
}
