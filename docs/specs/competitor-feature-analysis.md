# Competitor Feature Analysis: Gaia GPS & onX Backcountry

This document analyzes features from Gaia GPS and onX Backcountry to identify potential enhancements for MapViewer.

## 1. Map Layers & Data
| Feature | Gaia GPS | onX Backcountry | Priority for MapViewer |
| :--- | :---: | :---: | :--- |
| **Private/Public Land Boundaries** | Yes | Yes (Stronger) | High |
| **Topo Maps (USGS, etc.)** | Yes | Yes | Medium (Already started) |
| **Satellite with Topo Overlay** | Yes | Yes | High |
| **Slope Angle Shading** | Yes | Yes | Medium |
| **Wildfire Activity/Smoke** | Yes | No | Low |
| **Snow Depth/Weather Overlays** | Yes | Yes | Low |
| **Motorized vs Non-Motorized Trails** | Yes | Yes | Medium |
| **3D Map View** | Yes | Yes | Low |

## 2. Planning & Navigation Tools
| Feature | Gaia GPS | onX Backcountry | Priority for MapViewer |
| :--- | :---: | :---: | :--- |
| **Route Planning (Snap-to-trail)** | Yes | Yes | High |
| **Offline Map Downloads** | Yes | Yes | High (In progress) |
| **Waypoint/POI Creation** | Yes | Yes | High |
| **Track Recording** | Yes | Yes | High (Planned) |
| **Folder Organization** | Yes | Yes | Medium (Already exists) |
| **Photo Waypoints** | Yes | Yes | Medium |
| **Range Finder / Distance Tool** | Yes | Yes | Low |

## 3. Social & Safety
| Feature | Gaia GPS | onX Backcountry | Priority for MapViewer |
| :--- | :---: | :---: | :--- |
| **Location Sharing (LiveTrack)** | Yes | No | Low |
| **Community Shared Tracks** | Yes | Yes | Low |
| **Safety Alerts (SOS/Beacon)** | No | No | Low |

## Proposed Feature Roadmap for MapViewer

### Phase 1: Core Navigation (High Priority)
- **Waypoints & POIs**: Allow users to drop pins with custom icons and notes.
- **Route Planning**: Simple line-drawing tool initially, followed by snap-to-trail logic.
- **Improved Overlays**: Support for "Satellite + Trails" hybrid view.
- **Land Ownership**: Basic public/private land boundary layer (where data is available).

### Phase 2: Enhanced Visualization (Medium Priority)
- **Slope Angle Shading**: Crucial for winter backcountry safety.
- **Photo Waypoints**: Attach photos to specific coordinates.
- **Advanced Track Stats**: Speed, pace, and split times (building on existing stats).

### Phase 3: Advanced Overlays (Low Priority)
- **Weather & Environmental Data**: Current wildfires, snow depth.
- **Social Integration**: Ability to share tracks or folders with other users via links.
