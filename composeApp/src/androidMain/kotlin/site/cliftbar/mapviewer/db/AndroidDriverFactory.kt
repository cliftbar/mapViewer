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
        
        MapViewerDB.Schema.create(driver)
        driver
    } catch (e: Throwable) {
        throw RuntimeException("Android requires Context for in-memory driver. Not implemented for commonTest on real device. Original error: ${e.message}", e)
    }
}
