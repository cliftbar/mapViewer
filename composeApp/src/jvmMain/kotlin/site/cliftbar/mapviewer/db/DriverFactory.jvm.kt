package site.cliftbar.mapviewer.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import site.cliftbar.mapviewer.MapViewerDB
import java.io.File

actual fun createDriver(): SqlDriver {
    val databaseFile = File("mapviewer.db")
    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
    val currentVersion = getVersion(driver)
    val isBrokenV3 = currentVersion == 3L && !hasColorColumn(driver)

    if (currentVersion == 0L) {
        MapViewerDB.Schema.create(driver)
        setVersion(driver, MapViewerDB.Schema.version)
    } else if (currentVersion < MapViewerDB.Schema.version || isBrokenV3) {
        val startVersion = if (isBrokenV3) 2L else currentVersion
        MapViewerDB.Schema.migrate(driver, startVersion, MapViewerDB.Schema.version)
        setVersion(driver, MapViewerDB.Schema.version)
    }
    return driver
}

private fun hasColorColumn(driver: SqlDriver): Boolean {
    return try {
        driver.executeQuery(
            identifier = null,
            sql = "SELECT color FROM tracks LIMIT 0;",
            mapper = { QueryResult.Value(true) },
            parameters = 0
        ).value ?: false
    } catch (e: Exception) {
        false
    }
}

private fun getVersion(driver: SqlDriver): Long {
    return driver.executeQuery(
        identifier = null,
        sql = "PRAGMA user_version;",
        mapper = { cursor ->
            val next = cursor.next()
            if (next.value) {
                QueryResult.Value(cursor.getLong(0))
            } else {
                QueryResult.Value(0L)
            }
        },
        parameters = 0
    ).value ?: 0L
}

private fun setVersion(driver: SqlDriver, version: Long) {
    driver.execute(null, "PRAGMA user_version = $version;", 0)
}

actual fun createInMemoryDriver(): SqlDriver {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    MapViewerDB.Schema.create(driver)
    setVersion(driver, MapViewerDB.Schema.version)
    return driver
}
