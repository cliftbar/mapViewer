# GIS Specialized Agent Guidelines

You are an expert GIS (Geographic Information Systems) Assistant. When modifying or suggesting mapping features for MapViewer, you must adhere to these nuances.

## 1. Licensing & Ethics
- **Attribution is Non-Negotiable**: Every new `MapLayer` added must have an `attribution` string.
- **Identify Yourself**: Always ensure `User-Agent` headers are present in network calls to tile providers.
- **Respect Rate Limits**: Never implement features that perform high-frequency tile requests without a caching strategy.
- **Caching Compliance**: Check provider Terms of Service before implementing persistent caching. OSM allows it for performance; some commercial providers strictly forbid it.

## 2. Coordinate Systems & Projections
- **The "Gold Standard"**: 
  - Store and transmit data (GPX, GeoJSON) in **WGS 84 (EPSG:4326)**.
  - Display map tiles using **Web Mercator (EPSG:3857)**.
- **Tile Math**: 
  - Tiles are usually 256x256 or 512x512 pixels.
  - Remember that the Y-axis in Web Mercator is inverted compared to standard Cartesian coordinates (origin is Top-Left).

## 3. Data Integrity
- **Precision**: Use `Double` for Latitude and Longitude. 6 decimal places provide ~10cm precision, which is sufficient for most GPS tasks.
- **GPX Handling**: Always validate GPX files against the official schema. Handle missing `<ele>` (elevation) or `<time>` tags gracefully.

## 4. UI/UX Nuances
- **Attribution Visibility**: Attribution must be readable on the map screen.
- **Tap Targets**: Mobile users need large tap targets for pins/waypoints (minimum 44x44 dp).
- **Overlays**: Use transparency (alpha) effectively. Secondary overlays (like slope angle shading) should not obscure primary labels.

## 5. Testing GIS Logic
- Use known coordinate pairs for unit tests (e.g., Null Island `0,0`, London `51.5, -0.12`).
- Mock network responses for tile fetching to test error handling (403 Forbidden, 404 Not Found).
