package site.cliftbar.mapviewer.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker
import site.cliftbar.mapviewer.MapViewerDB

// Calls to 'js(code)' must be a single expression inside a top-level function body or a property initializer in Kotlin/Wasm.
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun createWorker(): Worker = js("""new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url), { type: 'module' })""")

actual suspend fun createDriver(): SqlDriver {
    val driver = WebWorkerDriver(createWorker())
    MapViewerDB.Schema.create(driver).await()
    return driver
}

actual suspend fun createInMemoryDriver(): SqlDriver {
    val driver = WebWorkerDriver(createWorker())
    MapViewerDB.Schema.create(driver).await()
    return driver
}
