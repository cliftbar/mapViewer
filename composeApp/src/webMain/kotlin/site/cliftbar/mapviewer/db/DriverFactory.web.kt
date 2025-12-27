package site.cliftbar.mapviewer.db

import app.cash.sqldelight.db.SqlDriver

actual fun createDriver(): SqlDriver {
    throw RuntimeException("Web requires specialized driver setup.")
}

actual fun createInMemoryDriver(): SqlDriver {
    throw RuntimeException("Web requires specialized driver setup.")
}
