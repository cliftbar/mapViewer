package site.cliftbar.mapviewer.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class LocationProvider {
    actual fun getLocations(): Flow<Location> {
        // TODO: Implement iOS location tracking using CoreLocation
        return flowOf(Location(0.0, 0.0))
    }
}

@Composable
actual fun rememberLocationProvider(): LocationProvider {
    return remember { LocationProvider() }
}
