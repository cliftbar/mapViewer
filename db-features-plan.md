# Database Feature & Improvement Plan

This document outlines planned improvements for the MapViewer database layer, focusing on performance, new features (offline support), and structural robustness.

## 1. Performance Optimizations (Indexing)
Currently, several foreign keys and frequently queried columns lack indexes, which will lead to performance degradation as the track database grows.

- [x] **Add Index on `track_points.track_id`**: Critical for fast track rendering and stats calculation.
- [x] **Add Index on `track_folder_map.folder_id`**: Speeds up track listing within folders.
- [x] **Add Index on `track_folder_map.track_id`**: Speeds up folder lookups for specific tracks.
- [x] **Migration Plan**: Create `5.sqm` to add these indexes.

## 2. Offline Map Support (Tile Caching)
To support offline maps, a dedicated tile cache schema is required.

- [ ] **Create `tile_cache` table**:
    - `zoom`: INTEGER
    - `x`: INTEGER
    - `y`: INTEGER
    - `layer_id`: TEXT
    - `data`: BLOB (Compressed tile image)
    - `expiry`: INTEGER (Timestamp)
    - `last_accessed`: INTEGER (For LRU eviction)
    - PRIMARY KEY (`layer_id`, `zoom`, `x`, `y`)
- [ ] **Implement Cleanup Logic**: A background task to delete tiles based on `expiry` or `last_accessed` when the cache exceeds a size limit (e.g., 500MB).

## 3. Data Integrity & Relationships
- [ ] **Standardize Deletion Cascades**: Ensure all relationships (like `track_stats_prefs`) correctly cascade on delete. (Partially done, needs verification across all tables).
- [ ] **Orphaned Points Cleanup**: Add a trigger or maintenance query to ensure no `track_points` exist without a parent `track`.
- [ ] **Unique Constraints**: Ensure `folder.name` is unique within the same `parent_id`.

## 4. Statistics & Metadata Enhancements
- [ ] **Track Metadata Table**: Separate heavy metadata (description, source URL, tags) from the main `tracks` table to keep the track list query lean.
- [ ] **Cached Stats**: Store calculated stats (total distance, duration) in a `track_stats_cache` table to avoid re-calculating on every load.

## 5. Developer Experience (DX) & Testing
- [ ] **Automated Migration Tests**: Implement a test suite that runs through all `.sqm` files sequentially to verify schema consistency across versions.
- [ ] **Common Result Wrapper**: Standardize the way database results (Success/Error/Loading) are handled in the Repository layer using Kotlin `Result` or a custom `Resource` sealed class.

## 6. Maintenance & Scalability
- [ ] **VACUUM Policy**: Implement a periodic `VACUUM` strategy, especially after large track deletions, to reclaim disk space.
- [ ] **Database Encryption**: Research `SQLCipher` integration for sensitive user data if required in the future.
