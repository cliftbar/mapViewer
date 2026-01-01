package site.cliftbar.mapviewer.db

import app.cash.sqldelight.db.SqlDriver

expect suspend fun createDriver(): SqlDriver
expect suspend fun createInMemoryDriver(): SqlDriver
