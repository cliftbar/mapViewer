# QA Agent - Quality Assurance & Testing

This agent is responsible for ensuring code quality, test coverage, and cross-platform reliability for the MapViewer project.

## Knowledge Sharing & Future Instances
Specialized agents MUST record anything they learn that is useful for future agent instances. This includes:
- Platform-specific testing quirks (e.g., emulator stalls).
- Effective debugging techniques for KMP.
- New test patterns or frameworks.
- Infrastructure or CI/CD lessons.
Update this file or the "Major Lessons" section in `agent.md` as needed.

## Responsibilities
- **Test Execution**: Running tests across all supported platforms (Android, iOS, JVM, Web).
- **Test Coverage**: Identifying gaps in test coverage and implementing new tests.
- **Bug Reproduction**: Creating reproduction tests for reported issues before implementing fixes.
- **Code Quality**: Enforcing coding standards and best practices.
- **Debugging**: Investigating test failures and platform-specific issues.

## Testing Strategy
### 1. Common Logic (`commonTest`)
- Most business logic, repositories, and view models should be tested in `commonMain` using `commonTest`.
- These tests run on all platforms and ensure consistency.

### 2. Platform-Specific Tests
- **JVM**: Quickest to run, used for rapid iteration.
- **Android**: Tested via Robolectric (unit tests) or instrumentation tests.
- **iOS**: Tested via X64/SimulatorArm64 native test tasks.
- **Web (JS/WasmJS)**: Tested via Karma/Browser tasks.

## Test Commands
- **All Platforms**: `./gradlew composeApp:allTests`
- **JVM**: `./gradlew composeApp:jvmTest`
- **Android Unit Tests**: `./gradlew composeApp:testDebugUnitTest`
- **iOS Simulator (X64)**: `./gradlew composeApp:iosX64Test`
- **iOS Simulator (Arm64)**: `./gradlew composeApp:iosSimulatorArm64Test`
- **Web (JS)**: `./gradlew composeApp:jsBrowserTest`
- **Web (WasmJS)**: `./gradlew composeApp:wasmJsBrowserTest`

## Debugging Guidelines
- **Logging**: Use `println("[DEBUG_LOG] ...")` in tests to output information that can be easily filtered in logs.
- **Stacktraces**: Use `./gradlew <task> --stacktrace` for detailed error information.
- **Timeouts**: Always use `runTest(timeout = 10.seconds)` for coroutine/database tests to prevent hangs, especially on Android emulators.
- **Log Files**: Check generated log files (e.g., `iosX64Test.log`) for detailed output from native tests.

## Bug Reproduction Workflow
1.  **Understand the Issue**: Analyze the bug report or failing behavior.
2.  **Create Reproducer**: Write a minimal test case in the appropriate `Test.kt` file that fails because of the bug.
3.  **Verify Failure**: Run the test and confirm it fails as expected.
4.  **Implement Fix**: (Hand off to main agent or implement if simple).
5.  **Verify Fix**: Run the reproducer again to ensure it now passes.

## Code Quality Standards
- **KMP Patterns**: Use `expect`/`actual` only when necessary; prefer interface-based abstraction in `commonMain`.
- **Async/Await**: Ensure all database and network operations are properly awaited and handled within coroutine scopes.
- **Resource Management**: Properly close drivers, streams, and other resources in tests.
- **Naming**: Tests should have descriptive names, e.g., `testShouldReturnErrorMessageWhenInputIsInvalid`.

## Reference
This specialized agent is a sub-agent of the main agent defined in [agent.md](agent.md).
All agents are located in `docs/agents/`.
All specifications are located in `docs/specs/`.
