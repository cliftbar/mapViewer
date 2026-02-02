# GIS Feature & Compliance Plan

This document outlines the strategy for GIS features, data sourcing, and licensing compliance for MapViewer.

## 1. Licensing & Usage Compliance

Currently, the application uses several third-party tile providers. To maintain access and legal compliance, the following technical requirements must be met:

### 1.1 Header Requirements
- **User-Agent**: All outgoing requests to tile servers must include a descriptive `User-Agent` header to identify the application and provide contact information.
- **Referer**: Some providers may require a `Referer` header to verify the domain (especially for web/JS builds).

### 1.2 Provider Specifics
| Layer | Provider | Status | Requirements |
| :--- | :--- | :--- | :--- |
| **OpenStreetMap** | OSM Foundation | ⚠️ At Risk | Strictly requires `User-Agent`. Forbidden: Heavy/Bulk downloading. |
| **OpenCycleMap** | Thunderforest | ⚠️ Disabled | Requires an API Key (`?apikey=...`). Free tier limits apply. |
| **OpenSnowMap** | OpenSnowMap.org | ✅ Compliant | Attribution required (present). |
| **Waymarked Trails** | Waymarked Trails | ✅ Compliant | Attribution required (present). |

### 1.3 Caching Policy
- **Tile Persistence**: Caching is encouraged for performance but must not be used to create an "offline-only" product that bypasses provider monetization.
- **TTL**: A default 30-day Time-To-Live (TTL) is implemented in `TileRepository`.

## 2. Technical GIS Roadmap

### 2.1 Tile Provider Integration
- [x] Implement `DefaultRequest` in `HttpClient.kt` with a compliant `User-Agent`.
- [ ] Wire `TileRepository` (cache) into `TileProvider` to reduce external hits.
- [ ] Externalize API keys (e.g., via `buildConfig` or `local.properties`).

### 2.2 Advanced Overlays
- **Satellite Hybrid**: Merge Satellite imagery (needs a provider like Mapbox or Esri) with transparent OSM trail overlays.
- **Slope Angle Shading (SAS)**: Implement color-coded transparency overlays for terrain steepness (crucial for backcountry/ski safety).
- **Land Ownership**: Research Public vs. Private land data sources (e.g., USFS, BLM data).

### 2.3 Coordinate & Projection Systems
- **Web Mercator (EPSG:3857)**: Primary system for tile display.
- **WGS 84 (EPSG:4326)**: Primary system for GPS coordinates and GPX storage.
- **Implementation**: Ensure all "Draw" and "Measure" tools correctly handle the projection conversion between screen pixels and geographical coordinates.

## 3. Performance & Storage
- **Tile Compression**: Evaluate WebP for local cache to save device storage.
- **Batch Downloads**: Implement a structured "Offline Pack" system that allows users to download specific regions while respecting provider download limits.
