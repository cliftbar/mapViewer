package site.cliftbar.mapviewer.db

import kotlin.test.Test
import kotlin.test.assertNotNull
import site.cliftbar.mapviewer.MapViewerDB

class DriverFactoryJvmTest {
    @Test
    fun testCreateInMemoryDriver() {
        val driver = createInMemoryDriver()
        assertNotNull(driver)
        val database = MapViewerDB(driver)
        assertNotNull(database)
    }
}
