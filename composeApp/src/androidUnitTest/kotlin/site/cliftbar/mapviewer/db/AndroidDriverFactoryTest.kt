package site.cliftbar.mapviewer.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import site.cliftbar.mapviewer.MapViewerDB
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class AndroidDriverFactoryTest {
    @Test
    fun testCreateInMemoryDriver() {
        val driver = createInMemoryDriver()
        assertNotNull(driver)
        val database = MapViewerDB(driver)
        assertNotNull(database)
    }

    @Test
    fun testAndroidDriverFactory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = AndroidDriverFactory(context)
        val driver = factory.createDriver()
        assertNotNull(driver)
    }
}
