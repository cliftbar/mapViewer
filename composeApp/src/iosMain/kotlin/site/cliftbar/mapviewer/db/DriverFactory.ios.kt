package site.cliftbar.mapviewer.db

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import site.cliftbar.mapviewer.MapViewerDB

private val emptySchema = object : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long get() = MapViewerDB.Schema.version
    override fun create(driver: SqlDriver): QueryResult.Value<Unit> = QueryResult.Value(Unit)
    override fun migrate(driver: SqlDriver, oldVersion: Long, newVersion: Long, vararg callbacks: AfterVersion): QueryResult.Value<Unit> = QueryResult.Value(Unit)
}

actual suspend fun createDriver(): SqlDriver {
    val driver = NativeSqliteDriver(
        schema = emptySchema,
        name = "mapviewer.db"
    )
    
    // Manually handle creation/migration because the real schema is async
    val currentVersion = getVersion(driver)
    val isBroken = currentVersion == MapViewerDB.Schema.version && !hasFoldersTable(driver)
    
    if (currentVersion == 0L) {
        MapViewerDB.Schema.create(driver).await()
        setVersion(driver, MapViewerDB.Schema.version)
    } else if (currentVersion < MapViewerDB.Schema.version || isBroken) {
        val startVersion = if (isBroken) MapViewerDB.Schema.version - 1 else currentVersion
        MapViewerDB.Schema.migrate(driver, startVersion, MapViewerDB.Schema.version).await()
        setVersion(driver, MapViewerDB.Schema.version)
    }
    
    return driver
}

private fun hasFoldersTable(driver: SqlDriver): Boolean {
    return try {
        driver.executeQuery(
            identifier = null,
            sql = "SELECT count(*) FROM folders LIMIT 0;",
            mapper = { QueryResult.Value(true) },
            parameters = 0
        ).value ?: false
    } catch (e: Exception) {
        false
    }
}

actual suspend fun createInMemoryDriver(): SqlDriver {
    val randomId = kotlin.random.Random.nextInt(1000000)
    val driver = NativeSqliteDriver(
        schema = emptySchema,
        name = "test_$randomId.db"
    )
    MapViewerDB.Schema.create(driver).await()
    return driver
}

private fun getVersion(driver: SqlDriver): Long {
    return driver.executeQuery(null, "PRAGMA user_version;", { cursor ->
        if (cursor.next().value) QueryResult.Value(cursor.getLong(0)) else QueryResult.Value(0L)
    }, 0).value ?: 0L
}

private fun setVersion(driver: SqlDriver, version: Long) {
    driver.execute(null, "PRAGMA user_version = $version;", 0)
}
