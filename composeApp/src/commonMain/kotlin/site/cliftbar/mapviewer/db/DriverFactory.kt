package site.cliftbar.mapviewer.db

import app.cash.sqldelight.db.SqlDriver

expect fun createDriver(): SqlDriver
expect fun createInMemoryDriver(): SqlDriver
