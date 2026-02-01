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

#### Phase 1: Deepen Common Logic Testing (Completed)
1.  **Parser Edge Cases**:
    - ✅ Added tests for large GPX files (~10,000 points) to monitor performance.
    - ✅ Tested malformed XML/JSON with specific recovery expectations via `ParserResult`.
2.  **Stats Calculator Robustness**:
    - ✅ Added tests for tracks crossing the International Date Line.
    - ✅ Verified stats calculation with high-frequency data (10Hz GPS).

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

### 3. Proposed Code Quality Standards (Implemented)

1.  **Structured Error Handling**:
    - ✅ Replaced `println` in parsers with `ParserResult` wrapped return type.
    - ✅ Defined `UIError` and `BaseScreenModel` to display meaningful error messages and handle coroutine exceptions.
2.  **KDoc & Documentation**:
    - ✅ KDoc added for public functions in `Repository` and `ScreenModel` classes.
    - ✅ Updated `docs/agents/qa.md` with lessons and standards.
3.  **Coroutine Safety**:
    - ✅ All `screenModelScope` launches in `ScreenModels` use `exceptionHandler` (via `BaseScreenModel`).

---

### 4. Implementation Plan (Updated)

| Task | Priority | Status | Target |
| :--- | :--- | :--- | :--- |
| **Parser Error Handling Refactor** | High | ✅ Done | commonMain |
| **Structured UI Error Reporting** | High | ✅ Done | commonMain |
| **KDoc & Documentation** | Medium | ✅ Done | commonMain |
| **Parser Edge Case Tests** | High | ✅ Done | commonTest |
| **TrackStatsPanel State Tests** | Medium | To Do | commonTest |
| **Folder Hierarchy Edge Case Tests** | Medium | To Do | commonTest |
| **JVM Screenshot Tests** | Low | To Do | jvmTest |

---
### 5. Lessons Learned

1. **JS/Browser Test Timeouts**: When testing large file parsing (e.g., 10,000 points) on JS/Browser targets, the default 2000ms timeout may be insufficient. Use `@Timeout` or increase the global timeout for such tests.
2. **KMP Serialization & Namespaces**: `xmlutil` serialization requires careful namespace handling. Explicitly adding the default GPX namespace to the input string can improve compatibility with various GPX creators.
3. **Structured Error Propagation**: Using a `sealed interface` for parser results (`ParserResult`) allows for much cleaner error handling in Repositories and ScreenModels compared to `try-catch` blocks and nullable returns.
4. **Coroutine Safety in KMP**: A centralized `BaseScreenModel` with a `CoroutineExceptionHandler` significantly reduces boilerplate and ensures that unexpected errors are always reported to the UI across all platforms.

---
*Created by QA Agent - February 2026*
