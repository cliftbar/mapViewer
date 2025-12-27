package site.cliftbar.mapviewer.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import site.cliftbar.mapviewer.MapViewerDB

actual fun createDriver(): SqlDriver {
    return NativeSqliteDriver(MapViewerDB.Schema, "mapviewer.db")
}

actual fun createInMemoryDriver(): SqlDriver {
    return NativeSqliteDriver(MapViewerDB.Schema, "test.db") // Just use a test file for now if null is not allowed
}
