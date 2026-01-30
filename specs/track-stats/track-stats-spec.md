# Track Stats Spec

## Goal
Implement extensible track stats (speed, elevation, stopped time) with:
- Stats visible on map screen and track screen.
- Avg speed toggle inline in the stats panel, persisted per track.
- Per-track settings stored in SQLDelight with app-level defaults.
- Stopped time algorithms selectable and configurable in Settings.

## Requirements
### Stats to compute
- Speed: min / max / avg.
- Elevation: min / max.
- Elevation gain/loss: gross gain, gross loss, net elevation.
- Stopped time.

### UI placement
- Map screen: stats shown for the selected track. Provide a selector listing visible tracks.
- Track screen: stats shown for each track (inline panel or expandable section).

### Settings
- Add a "Stats" section in Settings:
  - Default avg speed basis (Total time / Moving time).
  - Stopped time algorithm dropdown.
  - Algorithm-specific controls (for v1: speed threshold + min stop seconds).

### Avg speed toggle
- Toggle is inline in the stats panel.
- Persists per track (overrides default).
- If track has no override, use app default.

### Stopped time algorithms
- Support multiple algorithms and selection in Settings.
- v1 algorithm: speed threshold + min stop seconds.
- Algorithm config is stored in app settings.

## Proposed Architecture
### New stats module
Create `tracks/stats` package:
- `AvgSpeedBasis` enum: `TOTAL_TIME`, `MOVING_TIME`.
- `StoppedTimeAlgorithmId` enum: `SPEED_THRESHOLD`.
- `TrackStatsPrefs` (per-track overrides).
- `TrackStatsContext` (resolved defaults + overrides).
- `StoppedTimeAlgorithm` interface + `SpeedThresholdStoppedTimeAlgorithm`.
- `StoppedTimeAlgorithmRegistry` with definitions + factory.
- `TrackStatId`, `TrackStatDefinition`, `TrackStatResult`.
- `TrackStatsCalculator` with stat computation and formatters.

### Persisted settings
- App config (defaults) in `Config`:
  - `defaultAvgSpeedBasis` (enum).
  - `stoppedTimeAlgorithmId` (enum).
  - `stoppedTimeSpeedThresholdMps` (Double).
  - `stoppedTimeMinStopSeconds` (Int).

### Per-track preferences (SQLDelight)
Add new table:
```
track_stats_prefs(
  track_id TEXT PRIMARY KEY,
  avg_speed_basis TEXT,
  stopped_algo_id TEXT,
  stopped_speed_threshold_mps REAL,
  stopped_min_stop_seconds INTEGER
)
```
Queries:
- `getTrackStatsPrefs(track_id)`
- `getAllTrackStatsPrefs()`
- `upsertTrackStatsPrefs(...)`

### UI components
- Reusable `TrackStatsPanel` composable:
  - Renders stats list from the registry.
  - Inline avg speed toggle (Total vs Moving).
  - Optional header (track name / selector).

### ViewModel/data flow
- MapScreenModel:
  - `selectedTrackId` state.
  - `trackStatsPrefs` map, loaded via repository.
  - `updateAvgSpeedBasis(trackId, basis)` saves override.
- TrackManagementScreenModel:
  - `trackStatsPrefs` map.
  - `updateAvgSpeedBasis(trackId, basis)`.

## Algorithm details
### Speed samples
- Compute per-segment speed samples using consecutive points with valid timestamps.
- Distance: haversine (meters).
- Duration: seconds between timestamps.

### Stopped time (v1)
- Sum of sample durations where `speed < threshold`.
- Only count blocks that meet `minStopSeconds`.

### Elevation
- Min/max elevation from points with values.
- Gross gain/loss from consecutive points with elevations.
- Net elevation = last - first (within ordered points with elevation).

## Implementation Steps
- [x] 1: **Schema + config updates**
   - Add `track_stats_prefs` table + queries in `1.sq`.
   - Add defaults in `Config` and update serialization.

- [x] 2: **Stats module**
   - Implement models (`TrackStatsPrefs`, `TrackStatsContext`, enums, definitions).
   - Implement stopped time algorithm interface + registry.
   - Implement `TrackStatsCalculator` with helpers (distance, speed samples, elevation).

- [x] 3: **Repository support**
   - Add methods in `TrackRepository`:
     - `getTrackStatsPrefs(trackId)`
     - `getTrackStatsPrefsMap()`
     - `saveTrackStatsPrefs(prefs)`

- [x] 4: **Settings UI**
   - Add "Stats" section to `SettingsScreen`:
     - Default avg speed basis toggle.
     - Stopped algorithm dropdown.
     - Algorithm config fields.
   - Wire to `ConfigRepository.saveConfig`.

- [x] 5: **Stats UI components**
   - Add `TrackStatsPanel` composable in `ui/components`.
   - Provide inline avg speed toggle that calls viewmodel update.

- [x] 6: **Map screen integration**
   - Add selector for visible tracks (dropdown).
   - Render stats for selected track using `TrackStatsCalculator`.

- [x] 7: **Track screen integration**
   - Add stats panel per track (inline or expandable section).
   - Use per-track prefs + defaults to compute context.

- [x] 8: **Tests**
   - Add unit tests for `TrackStatsCalculator`:
     - Single-point track.
     - Missing timestamps.
     - Missing elevations.
     - Stopped time threshold logic.
     - Avg speed basis total vs moving.

## Open UX Decisions (for implementation)
- Track screen stats layout: resolved as expandable details panel.
- Units now support per-app and per-track preferences (distance: meters/kilometers/feet/miles; speed: km/h, m/s, mph, knots).
