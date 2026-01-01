package site.cliftbar.mapviewer.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import site.cliftbar.mapviewer.MapViewerDB
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class AndroidDriverFactoryTest {
    @Test
    fun testCreateInMemoryDriver() = runTest {
        val driver = createInMemoryDriver()
        assertNotNull(driver)
        val database = MapViewerDB(driver)
        assertNotNull(database)
    }

    @Test
    fun testAndroidDriverFactory() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = AndroidDriverFactory(context)
        val driver = factory.createDriver()
        assertNotNull(driver)
        
        // Verify folders table exists
        val database = MapViewerDB(driver)
        val folders = database.`1Queries`.getAllFolders().executeAsList()
        assertNotNull(folders)
    }
}
