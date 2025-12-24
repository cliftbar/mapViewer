# Agent Directions - MapViewer

This is a Kotlin Multiplatform (KMP) mapping application.

## Project Goals
- Show a map using OpenStreetMap (OSM) data.
- Track current position and save to GPX.
- Export GPX to various formats (GPX, GeoJSON).
- Import GPX and GeoJSON tracks.
- Import raster files for overlays.
- Main Screens: Map, Settings, Track Management.
- Support Online and Offline modes.

## Tech Stack
- **Kotlin Multiplatform (KMP)**
- **Compose Multiplatform** for UI.
- **SQLDelight** for local storage.
- **Kermit** for logging (or standard KMP logging).
- **Ktor** for networking.

## Development Guidelines
- Follow KMP best practices.
- Use `commonMain` as much as possible for logic.
- Platform-specific code should be in `androidMain`, `iosMain`, `jvmMain`, etc., using expect/actual.
- Keep `agent.md` updated with progress and important architectural decisions.

## Current Progress
- [x] Initial project structure created.
- [x] Basic screen structure and navigation (Map, Tracks, Settings).
- [x] Custom MapView using Compose Canvas.
- [x] Tile provider using Ktor for OSM fetching.
- [x] Platform-specific image decoding (expect/actual).
- [x] Location tracking architecture (LocationProvider interface).
- [x] Mock Location Provider (JVM).
- [x] Android Location Provider foundation (FusedLocationProviderClient).
- [x] Map Screen Enhancements (Rendering optimizations, Zoom/Pan, Lat/Lon preservation, Layer categories).
- [x] Persist map position across screen navigation (Voyager TabNavigator + ScreenModel).
- [x] Fix map unresponsiveness after state hoisting (pointerInput state capture fix).
- [x] Default map location set to Portland, Oregon.
- [x] Support for multiple OSM-based layers (OpenSnowMap, Waymarked Trails, etc.).
- [ ] Implement iOS Location Provider.
- [ ] Implement Offline Map Support (SQLDelight/FileSystem tile caching).
- [ ] Implement Track Recording and GPX storage.
- [ ] Implement GPX/GeoJSON Import/Export.
- [ ] Implement Raster Overlays.

## Future Roadmap

### Phase 1: Core Mapping & Location
- Complete MapView interactions (multi-touch zoom, inertia pan).
- Implement persistent tile cache for offline use.
- Finish iOS location implementation.

### Phase 2: Track Management
- Real-time track rendering on the map.
- Background location updates (Foreground Service on Android, Background Mode on iOS).
- Track list view with basic stats (distance, time).

### Phase 3: Data Exchange & Overlays
- GPX/GeoJSON parser/serializer implementation.
- File picker integration for importing tracks.
- Raster overlay rendering (MapTiles from local files).

## Implementation Plans

### Configuration Loader (SQLite Persistence & YAML Overrides)
#### Phase 1: Infrastructure & Dependencies
1.  **Add YAML Parsing Library**: Integrate a KMP-compatible YAML library (e.g., `net.mamoe.yamlkt`).
2.  **Define Configuration Schema**: Create a `@Serializable` `Config` data class in `commonMain`.
3.  **SQLDelight Schema Update**: Add a `config` table to `1.sq` for key-value persistence.

#### Phase 2: Configuration Loader Logic
1.  **Config Repository**: Create `ConfigRepository` in `commonMain` for serializing/deserializing config to SQLite.
2.  **YAML Override Implementation**: Implement `expect val platformConfigPath` to locate `config.yaml` on each platform.
3.  **Merge Logic**: Update `loadConfig()` to merge YAML values over SQLite values.

#### Phase 3: Integration & UI
1.  **App Startup**: Initialize `ConfigRepository` and load config in `App.kt`.
2.  **State Management**: Update `MapScreenModel` and others to react to config changes.
3.  **Settings Screen**: Display config values, allow saving to SQLite, and indicate YAML overrides.

#### Phase 4: Validation & Testing
1.  **Unit Tests**: Verify YAML > SQLite priority in `commonTest`.
2.  **Platform Testing**: Verify platform-specific YAML loading.
