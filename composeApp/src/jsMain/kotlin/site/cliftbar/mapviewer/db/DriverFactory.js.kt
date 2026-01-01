package site.cliftbar.mapviewer.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker
import site.cliftbar.mapviewer.MapViewerDB

actual suspend fun createDriver(): SqlDriver {
    val driver = WebWorkerDriver(
        Worker(
            js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)"""),
            js("({ type: 'module' })")
        )
    )
    MapViewerDB.Schema.create(driver).await()
    return driver
}

actual suspend fun createInMemoryDriver(): SqlDriver {
    val driver = WebWorkerDriver(
        Worker(
            js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)"""),
            js("({ type: 'module' })")
        )
    )
    MapViewerDB.Schema.create(driver).await()
    return driver
}
