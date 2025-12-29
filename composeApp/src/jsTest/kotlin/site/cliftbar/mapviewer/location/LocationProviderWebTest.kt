package site.cliftbar.mapviewer.location

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class LocationProviderWebTest {
    @Test
    fun testGetLocations() = runTest {
        val provider = LocationProvider()
        val location = provider.getLocations().first()
        assertNotNull(location)
    }
}
