# TODO

## Existing Project Todos
- [ ] [iOS] Implement iOS Location Provider.
- [x] [Android][iOS][Desktop][Web] Implement Offline Map Support (SQLDelight/FileSystem tile caching).
- [ ] [Android][iOS][Desktop][Web] Implement Track Recording and GPX storage.
- [ ] [Android][iOS][Desktop][Web] Implement Raster Overlays.

## Feature Todos (Client-First)
- [ ] [iOS] iOS `LocationProvider` actual.
  - [ ] [iOS] Wire CoreLocation service + permission prompts.
  - [ ] [iOS] Add background location capability toggle (if needed for recording).
  - [ ] [iOS] Add iOS-specific tests/mocks.

- [ ] [Android][iOS][Desktop][Web] Tile cache eviction policy.
  - [ ] [Android][iOS][Desktop][Web] Define cache size cap config (per platform).
  - [ ] [Android][iOS][Desktop][Web] Implement LRU index + eviction in SQLDelight/FileSystem.
  - [ ] [Android][iOS][Desktop][Web] Add tests for eviction order and size thresholds.

- [ ] [Android][iOS] Background track recording.
  - [ ] [Android] Foreground service + notification + lifecycle hooks.
  - [ ] [iOS] Background mode + task scheduling + permission flow.
  - [ ] [Android][iOS] Add pause/resume + recovery after app restart.

- [x] [Android][iOS][Desktop][Web] Track list stats.
  - [x] [Android][iOS][Desktop][Web] Compute distance/duration/elevation from track points.
  - [x] [Android][iOS][Desktop][Web] Store computed stats for quick display.
  - [x] [Android][iOS][Desktop][Web] Add unit tests for stats accuracy.

- [ ] [Android][iOS][Desktop][Web] Import validation + user-facing errors.
  - [ ] [Android][iOS][Desktop][Web] Validate GPX/GeoJSON schema + required fields.
  - [ ] [Android][iOS][Desktop][Web] Surface errors in UI with actionable messages.
  - [ ] [Android][iOS][Desktop][Web] Add tests for common malformed inputs.

- [ ] [Android][iOS][Desktop][Web] Raster overlay support.
  - [ ] [Android][iOS][Desktop][Web] Define raster metadata (bounds, projection, opacity).
  - [ ] [Android][iOS][Desktop][Web] Load local file + render overlay in map.
  - [ ] [Android][iOS][Desktop][Web] Add visibility/opacity controls + persistence.

- [ ] [Android][iOS][Desktop][Web] Layer presets + quick toggle UI.
  - [ ] [Android][iOS][Desktop][Web] Persist presets (name + layer list + styles).
  - [ ] [Android][iOS][Desktop][Web] Add UI for create/edit/delete presets.
  - [ ] [Android][iOS][Desktop][Web] Add quick toggle bar on map screen.

- [ ] [Android][iOS][Desktop] Offline download UI.
  - [ ] [Android][iOS][Desktop] Define area selection (box or polygon) + zoom range.
  - [ ] [Android][iOS][Desktop] Show progress + estimated size.
  - [ ] [Android][iOS][Desktop] Queue/cancel downloads + store offline packs.

- [ ] [Android][iOS][Desktop][Web] Track export options.
  - [ ] [Android][iOS][Desktop][Web] Add export format options + filename templating.
  - [ ] [Android][iOS][Desktop][Web] Support splitting by time/size.
  - [ ] [Android][iOS][Desktop][Web] Add tests for export output correctness.

- [ ] [Android][iOS][Desktop][Web] Map interactions (inertia/pinch smoothing).
  - [ ] [Android][iOS][Desktop][Web] Implement pan inertia + decay tuning.
  - [ ] [Android][iOS][Desktop][Web] Improve pinch zoom smoothing and velocity handling.
  - [ ] [Android][iOS][Desktop][Web] Add regression tests for gesture behavior (where feasible).

## GIS & Compliance (from gis-feature-plan.md)
- [x] Implement `User-Agent` in `HttpClient.kt`.
- [x] Integrate `TileRepository` cache into `TileProvider`.
- [ ] Add API Key support for Thunderforest layers. (Currently Disabled)
- [ ] Research Land Ownership and Satellite Hybrid sources.

## New Features (Inspired by Competitors)
- [ ] Waypoints & POIs
  - [ ] Implement database schema for waypoints (lat, lon, icon, name, notes).
  - [ ] Add UI for dropping pins on the map.
  - [ ] Support custom icons/colors for different POI types.
- [ ] Route Planning
  - [ ] Implement "Draw Route" tool.
  - [ ] Add snap-to-trail functionality (via external routing API like OSRM or GraphHopper).
- [ ] Advanced Map Layers
  - [ ] Support for Satellite + Trails hybrid overlay.
  - [ ] Research and implement Land Ownership (Public/Private) data source.
  - [ ] Slope Angle Shading for winter safety.
- [ ] Multimedia Support
  - [ ] Photo Waypoints: Attach images to coordinates/waypoints.
