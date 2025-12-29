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
- **All future functionality MUST come with corresponding tests.**
- Keep `agent.md` updated with progress and important architectural decisions.
- **Ask questions whenever something is unclear.**

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
- [x] Build and dependency fixes (YAML decoupling for wasmJs, missing actuals).
- [x] Auto-save map configuration (zoom, center, layers) to SQLite database.
- [x] Support multiple configuration profiles and shared state synchronization.
- [ ] Implement iOS Location Provider.
- [ ] Implement Offline Map Support (SQLDelight/FileSystem tile caching).
- [ ] Implement Track Recording and GPX storage.
- [x] Implement GPX/GeoJSON Import/Export.
- [x] Fix Android serialization crashes using Voyager ScreenModel.
- [x] Implement comprehensive test suite (Unit, Integration, and logic-based UI tests).
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
1.  **Add YAML Parsing Library**: Integrated `net.mamoe.yamlkt`. [x]
2.  **Define Configuration Schema**: Created `@Serializable` `Config` data class. [x]
3.  **SQLDelight Schema Update**: Added `config` table for key-value persistence. [x]

#### Phase 2: Configuration Loader Logic
1.  **Config Repository**: Created `ConfigRepository` in `commonMain`. [x]
2.  **YAML Override Implementation**: Implemented `expect val platformConfigPath` and `readFile`. [x]
3.  **Merge Logic**: Basic merge logic implemented (YAML takes precedence). [x]
4.  **Decoupled YAML**: Moved YAML parsing to `expect/actual` to support `wasmJs`. [x]

#### Phase 3: Integration & UI
1.  **App Startup**: Initialized `ConfigRepository` in `App.kt`. [x]
2.  **State Management**: `MapScreenModel` now uses settings from `Config`. [x]
3.  **Settings Screen**: Added UI to view and save configuration. [x]
4.  **Dark Theme**: Added support for dark/light themes and macOS title bar synchronization. [x]

#### Phase 4: Validation & Testing
1.  **Unit Tests**: Implemented comprehensive suite for common logic and view models. [x]
2.  **Platform Testing**: Basic platform-specific integration tests implemented. [x]
