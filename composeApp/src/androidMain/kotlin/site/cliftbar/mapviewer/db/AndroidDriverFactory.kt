package site.cliftbar.mapviewer.db

import android.content.Context
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import site.cliftbar.mapviewer.MapViewerDB

private val emptySchema = object : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long get() = MapViewerDB.Schema.version
    override fun create(driver: SqlDriver): QueryResult.Value<Unit> = QueryResult.Value(Unit)
    override fun migrate(driver: SqlDriver, oldVersion: Long, newVersion: Long, vararg callbacks: AfterVersion): QueryResult.Value<Unit> = QueryResult.Value(Unit)
}

actual suspend fun createDriver(): SqlDriver {
    throw RuntimeException("Android requires Context to create SqlDriver. Use AndroidDriverFactory.")
}

class AndroidDriverFactory(private val context: Context) {
    suspend fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(
            schema = emptySchema,
            context = context,
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
        println("[DEBUG_LOG] Folders table check failed: ${e.message}")
        false
    }
}

actual suspend fun createInMemoryDriver(): SqlDriver {
    return try {
        // Try JDBC first (works in Android unit tests if sqldelight-desktop-driver is on classpath)
        // Note: We need to use "jdbc:sqlite::memory:" for JDBC driver
        val jdbcClass = Class.forName("app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver")
        
        // Ensure the SQLite JDBC driver is registered (Robolectric sometimes needs this)
        try {
            val driverClass = Class.forName("org.sqlite.JDBC")
            val driverInstance = driverClass.getDeclaredConstructor().newInstance() as java.sql.Driver
            java.sql.DriverManager.registerDriver(driverInstance)
        } catch (e: Exception) {
            println("[DEBUG_LOG] Failed to register org.sqlite.JDBC: ${e.message}")
        }
        
        val constructors = jdbcClass.getDeclaredConstructors()
        val constructor = constructors.firstOrNull { it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }
            ?: constructors.firstOrNull { it.parameterTypes.size == 2 && it.parameterTypes[0] == String::class.java }

        val driver = if (constructor?.parameterTypes?.size == 1) {
            constructor.newInstance("jdbc:sqlite::memory:")
        } else {
            constructor?.newInstance("jdbc:sqlite::memory:", java.util.Properties())
        } as SqlDriver
        
        MapViewerDB.Schema.create(driver).await()
        driver
    } catch (e: Throwable) {
        throw RuntimeException("Android requires Context for in-memory driver. Not implemented for commonTest on real device. Original error: ${e.message}", e)
    }
}

private fun getVersion(driver: SqlDriver): Long {
    return driver.executeQuery(null, "PRAGMA user_version;", { cursor ->
        if (cursor.next().value) QueryResult.Value(cursor.getLong(0)) else QueryResult.Value(0L)
    }, 0).value ?: 0L
}

private fun setVersion(driver: SqlDriver, version: Long) {
    driver.execute(null, "PRAGMA user_version = $version;", 0)
}
