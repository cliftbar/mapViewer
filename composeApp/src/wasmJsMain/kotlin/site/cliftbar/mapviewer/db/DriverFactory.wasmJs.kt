package site.cliftbar.mapviewer.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker
import site.cliftbar.mapviewer.MapViewerDB

// Calls to 'js(code)' must be a single expression inside a top-level function body or a property initializer in Kotlin/Wasm.
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun getWorkerUrl(): String = js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url).href""")

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun getWorkerOptions(): org.w3c.dom.WorkerOptions = js("({ type: 'module' })")

actual suspend fun createDriver(): SqlDriver {
    val driver = WebWorkerDriver(
        Worker(getWorkerUrl(), getWorkerOptions())
    )
    MapViewerDB.Schema.create(driver).await()
    return driver
}

actual suspend fun createInMemoryDriver(): SqlDriver {
    val driver = WebWorkerDriver(
        Worker(getWorkerUrl(), getWorkerOptions())
    )
    MapViewerDB.Schema.create(driver).await()
    return driver
}
