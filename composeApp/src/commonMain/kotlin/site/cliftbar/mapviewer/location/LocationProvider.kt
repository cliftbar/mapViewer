package site.cliftbar.mapviewer.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import kotlinx.coroutines.flow.Flow

data class Location(val latitude: Double, val longitude: Double)

expect class LocationProvider {
    fun getLocations(): Flow<Location>
}

@Composable
expect fun rememberLocationProvider(): LocationProvider
