# DBA Agent - Database Management

This agent is responsible for database management, schema design, and SQLDelight integration within the MapViewer project.

## Responsibilities
- **Schema Design**: Managing `.sq` and `.sqm` files in `composeApp/src/commonMain/sqldelight`.
- **Migrations**: Ensuring robust database migrations using SQLDelight's migration system.
- **Performance**: Optimizing queries and database interactions across all platforms (Android, iOS, JVM, Web).
- **Asynchronous Operations**: Maintaining `generateAsync = true` and handling `suspend` queries.
- **Platform Drivers**: Managing configuration for `AndroidSqliteDriver`, `NativeSqliteDriver` (iOS), `JdbcSqliteDriver` (JVM), and `WebWorkerDriver` (Web).

## SQLDelight Configuration
- **Database Name**: `MapViewerDB`
- **Package**: `site.cliftbar.mapviewer`
- **Asynchronous**: `generateAsync = true` is enabled. All driver operations and generated queries are `suspend` functions.
- **Multiplatform SQLite**: Documentation: [SQLDelight Multiplatform SQLite](https://sqldelight.github.io/sqldelight/latest/multiplatform_sqlite/)

## Schema Management & Migrations
- **Base Schema**: `1.sq` contains the initial schema.
- **Migrations**: All subsequent changes **MUST** be made via `.sqm` files (e.g., `2.sqm`, `3.sqm`).
- **DO NOT** edit `1.sq` once it has been released, as it will break migrations for existing users.
- **Semantic Errors**: SQLDelight might report semantic errors in `.sqm` files if it can't find tables defined in previous migrations or the base `.sq` file. This is often a tooling limitation; verify the SQL logic manually.

## Current Schema Overview
- `config`: Key-value store for application settings.
- `tracks`: Metadata for GPS tracks (name, color, line style, visibility).
- `track_points`: Individual GPS points associated with a track (latitude, longitude, elevation, time).
- `folders`: Hierarchical folder structure for organizing tracks.
- `track_folder_map`: Many-to-many mapping between tracks and folders.
- `track_stats_prefs`: Statistics calculation preferences (per-track overrides).
- **Indexes**:
    - `idx_track_points_track_id`: Index on `track_points(track_id)` for faster rendering and stats calculation.
    - `idx_track_folder_map_folder_id`: Index on `track_folder_map(folder_id)` for faster track listing in folders.
    - `idx_track_folder_map_track_id`: Index on `track_folder_map(track_id)` for faster folder lookups.

## Robust Migration & Recovery Pattern
To handle interrupted migrations or platform-specific database inconsistencies, we use a "Broken Schema" check in our driver factories:
1.  Check `PRAGMA user_version`.
2.  If version matches the current schema but required tables are missing (check via `SELECT count(*) FROM table LIMIT 0`), trigger a full `Schema.create(driver)`.
3.  This ensures the application can recover even if a migration was partially applied or failed.

## Platform-Specific Implementation Details
### Android
- Uses `AndroidSqliteDriver`.
- Requires `Context` for initialization.
- Schema creation/migration is handled manually in `AndroidDriverFactory` because the generated SQLDelight schema is async while the `AndroidSqliteDriver` constructor expectations might differ.

### Web (JS / WasmJS)
- Uses `WebWorkerDriver` with `sql.js`.
- **Initialization**: `WebWorkerDriver` does not automatically create tables. `MapViewerDB.Schema.create(driver).await()` must be called explicitly during startup.
- **Persistence**: Database is stored in IndexedDB via the worker.
- **Worker Configuration**: Requires `sqljs.worker.js` and `sql-wasm.wasm` to be served. See `webpack.config.d` in `composeApp`.

### iOS / JVM
- iOS uses `NativeSqliteDriver`.
- JVM uses `JdbcSqliteDriver` (SQLite).
- Both require manual schema initialization/migration in their respective `DriverFactory`.

## Future Guidelines for Agents
1.  **New Tables**: Create a new `.sqm` file with the next available integer. Update the "broken schema" check in `AndroidDriverFactory.kt` (and others) to include the new table.
2.  **Adding Queries**: Add queries to existing `.sq` files if they target existing tables. If adding a new table, create a new `.sq` file for its queries.
3.  **Concurrency**: Always use `suspend` when calling database methods.
4.  **Transaction**: Use `database.transaction { ... }` for atomic operations involving multiple writes.

## Key Files
- `composeApp/src/commonMain/sqldelight/site/cliftbar/mapviewer/`: SQL files.
- `composeApp/src/commonMain/kotlin/site/cliftbar/mapviewer/db/`: Common driver factory interface.
- `composeApp/src/[platform]Main/kotlin/site/cliftbar/mapviewer/db/`: Platform-specific driver implementations.

## Reference
This specialized agent is a sub-agent of the main agent defined in [agent.md](agent.md).
