package site.cliftbar.mapviewer.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import site.cliftbar.mapviewer.MapViewerDB

actual fun createDriver(): SqlDriver {
    return NativeSqliteDriver(MapViewerDB.Schema, "mapviewer.db")
}

actual fun createInMemoryDriver(): SqlDriver {
    val randomId = kotlin.random.Random.nextInt(1000000)
    return NativeSqliteDriver(MapViewerDB.Schema, "test_$randomId.db")
}
