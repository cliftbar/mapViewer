### QA & Code Quality Improvement Plan - MapViewer

This document outlines the current state of quality assurance for the MapViewer project and provides a roadmap for improving test coverage, code quality, and cross-platform reliability.

---

### 1. Current Status Assessment

#### 1.1 Test Coverage Overview
- **Core Logic (commonTest)**: ~85-90%. High coverage for `TrackParser` (GPX/GeoJSON), `TrackStatsCalculator`, and `TrackRepository`.
- **UI Components (commonTest)**: ~30%. Basic rendering and zoom tests exist, but complex state interactions are largely untested.
- **Platform-Specific (androidUnitTest, etc.)**: ~10%. Limited to database factory and basic location provider tests.
- **Data Integrity**: Good coverage for database migrations and basic CRUD operations.

#### 1.2 Code Quality Observations
- **Architecture**: Clean separation of concerns using Voyager `ScreenModel`, Repository pattern, and SQLDelight.
- **Error Handling**: Parsers use broad `try-catch` blocks with `println` logging. Lack of structured error reporting to the UI.
- **Consistency**: Strong adherence to KMP patterns, though `expect`/`actual` for File/Color pickers lacks automated verification.

---

### 2. Improvement Roadmap

#### Phase 1: Deepen Common Logic Testing (Short Term)
1.  **Parser Edge Cases**:
    - Add tests for large GPX files (>10MB) to monitor performance.
    - Test malformed XML/JSON with specific recovery expectations (e.g., partial track recovery).
2.  **Stats Calculator Robustness**:
    - Add tests for tracks crossing the International Date Line.
    - Verify stats calculation with extremely high-frequency data (e.g., 10Hz GPS).

#### Phase 2: Enhanced UI & State Testing (Mid Term)
1.  **Component Testing**:
    - **TrackStatsPanel**: Create tests for various `TrackStatsPrefs` overrides to ensure UI correctly reflects preference changes (units, algorithms).
    - **TrackManagementScreenModel**: Verify folder hierarchy logic, moving tracks between folders, and multi-selection behavior.
2.  **Navigation Testing**:
    - Verify state persistence when navigating between `MapScreen` and `SettingsScreen`.

#### Phase 3: Platform & Integration Testing (Long Term)
1.  **Visual Regression**: Introduce screenshot testing for `MapView` on JVM to catch UI regressions in tile rendering and track drawing.
2.  **Platform Bridges**:
    - Implement mock-based verification for `FilePicker` and `ColorPicker` in platform-specific test sets.
    - Expand Android instrumentation tests for `LocationProvider` to simulate movement.

---

### 3. Proposed Code Quality Standards

1.  **Structured Error Handling**:
    - Replace `println` in parsers with a result-wrapped return type or a dedicated `Logger` interface.
    - Define a `UIError` state in `ScreenModels` to display meaningful error messages to users.
2.  **KDoc & Documentation**:
    - Require KDoc for all public functions in `Repository` and `ScreenModel` classes.
    - Maintain `docs/agents/qa.md` with updated Gradle commands and testing strategies.
3.  **Coroutine Safety**:
    - Ensure all `screenModelScope` launches handle `CancellationException` and unexpected errors via a custom `CoroutineExceptionHandler`.

---

### 4. Implementation Plan

| Task | Priority | Target |
| :--- | :--- | :--- |
| **Parser Error Handling Refactor** | High | commonMain |
| **TrackStatsPanel State Tests** | Medium | commonTest |
| **Folder Hierarchy Edge Case Tests** | Medium | commonTest |
| **JVM Screenshot Tests** | Low | jvmTest |
| **structured UI Error Reporting** | High | commonMain |

---
*Created by QA Agent - February 2026*
