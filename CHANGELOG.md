# Changelog

All notable changes to this project will be documented in this file.

This project adheres to Semantic Versioning and follows a Keep a Changelog-like format.

## [Unreleased]

### Added

-   Conformance test suite for TOON Specification 3.1.
-   Security validation tests for input sanitization.

### Changed

-   Implemented new TOON Specification features: empty array syntax (`[]`) support in both encoder and decoder.
-   Improved `StringEscaper` with proper Unicode escape handling and control character formatting.
-   Upgraded Jackson dependencies to 3.1.x line.
-   Upgraded Gradle wrapper to 9.5.1.
-   Upgraded SpotBugs to 6.5.4, PIT to 1.19.0.
-   General code cleanup and test coverage improvements.

### Security

-   Fixed 23 vulnerabilities from V12 dependency audit.

## [1.0.9] - 2026-02-24

### Added

-   Thread safety tests for race condition scenarios.
-   Performance benchmarks for core encoding/decoding paths.
-   Conformance tests for TOON Specification 3.0.3.

### Changed

-   Updated to TOON Specification 3.0.3 compliance (#100).
-   Replaced hardcoded delimiter characters with `Delimiter` enum values throughout codebase (#94).
-   Extracted magic values into named constants for better maintainability (#95).
-   Switched coverage badge generation to a dedicated GitHub Action (#86).
-   Code cleanup: streamlined test structure, reduced technical debt (#96).
-   Upgraded Jackson databind to 3.0.4.
-   Upgraded Gradle wrapper to 9.3.1.
-   Upgraded JUnit BOM to 6.0.3, SpotBugs to 6.4.8.

## [1.0.8] - 2026-01-09

### Added

-   SQL Date support: proper handling of `java.sql.Date` (which extends `java.util.Date` but does not support `toInstant`).

### Fixed

-   Timezone-related test failures in `JsonNormalizerTest` due to locale-dependent assertions (#83).

### Changed

-   README cleanup: removed outdated badges and clarified installation instructions (#78).
-   Upgraded `actions/upload-artifact` from 5.0.0 to 6.0.0.

## [1.0.7] - 2025-12-09

### Added

-   Singleton `ObjectMapper` for shared Jackson resource management (#71).
-   Comprehensive decoder tests: `ArrayDecoderTest`, `ObjectDecoderTest`, `KeyDecoderTest`, `TabularArrayDecoderTest`, `ListItemDecoderTest`, `DecodeHelperTest`, `DecodeParserTest`.
-   Unit tests for special numeric value encoding (NaN, Infinity).

### Changed

-   Refactored decoder package: split monolithic `ValueDecoder` into dedicated classes (`ArrayDecoder`, `ObjectDecoder`, `KeyDecoder`, `DecodeHelper`, `DecodeParser`, `ListItemDecoder`, `TabularArrayDecoder`) (#62).
-   Preserved Java Bean field ordering during encoding (#76).
-   Updated to TOON Specification 3.0.1 compliance (#70).
-   Improved whitespace counting in `DecodeHelper` — leading spaces only counted once (#68).
-   Removed community health files in favor of organization-wide defaults.
-   Standardized `CONTRIBUTING.md` structure.
-   Upgraded Jackson databind to 3.0.3, Jackson Afterburner to 3.0.2.
-   Upgraded `actions/checkout` to 6.0.0, `actions/setup-java` to 5.1.0.

## [1.0.6] - 2025-11-26

### Added

-   Unit tests for tabular arrays as first field in list items.
-   Unit tests for arrays of arrays within objects.
-   `.gitattributes` file for line ending handling (CRLF for Windows batch scripts).
-   `.editorconfig` file for consistent editor settings.

### Changed

-   Updated to TOON Specification 3.0 compliance (#59).
-   Improved `ValueDecoder` tabular array parsing with dynamic depth detection.
-   Fixed `ListItemEncoder` indentation depth (depth+1 → depth+2) for proper nested structure encoding.
-   Updated conformance test files to spec version 3.0.
-   Updated GitHub issue template to reference spec v3.0 instead of v1.3.
-   Minor code formatting improvements in `ObjectEncoder`.

## [1.0.5] - 2025-11-23

### Added

-   Key Folding Option with folding death
-   Added unit tests for PrimitiveDecoder to validate handling of primitives and edge cases.
-   Added unit tests for StringValidator, covering the updated pattern evaluation order.

### Changed

-   Refactored ObjectEncoder and Flatter to improve structure robustness after spec updates involving key-folding.
-   Refactored ValueDecoder (two-step refactor) to better separate logic and improve maintainability.
-   Updated test resources to conform to TOON Specification v2.0.1.
-   Updated PrimitiveDecoder with improved regex logic for more accurate literal parsing.
-   Updated StringValidator pattern ordering: octal is now evaluated before numeric.

## [0.1.4] - 2025-11-20

### Added

-   Javadoc generation task (`generateJavadoc`) in build.gradle.
-   Specs validation task (`specsValidation`) for conformance testing.
-   CODE_OF_CONDUCT.md.
-   CONTRIBUTING.md.
-   GitHub templates: CODEOWNERS, ISSUE_TEMPLATE (bug_report.yml, feature_request.yml, spec_compliance.yml), PULL_REQUEST_TEMPLATE.md.
-   Documentation reorganization: moved TOON-SPECIFICATION.md to docs/FORMAT.md, added docs/README.md.
-   Javadoc HTML documentation in docs/javadoc/.

### Changed

-   **BREAKING**: Package name migration from `com.felipestanzani.jtoon` to `dev.toonformat.jtoon`.
-   **BREAKING**: Maven group ID changed from `com.felipestanzani` to `dev.toonformat`.
-   Repository migrated from `felipestanzani/jtoon` to `toon-format/toon-java`.
-   Minimum test coverage requirement increased from 85% to 90%.
-   LICENSE.md renamed to LICENSE.
-   Updated GitHub Actions workflows (build.yml, release.yml).
-   Updated Gradle wrapper.
-   Updated dependency: `actions/github-script` from 6 to 8.

## [0.1.3] - 2025-11-14

### Added

-   Decoding support via `JToon.decode()` and `JToon.decodeToJson()` methods.
-   `DecodeOptions` record with `strict` validation mode.
-   `decoder` package with full TOON parser supporting all formats (primitives, objects, arrays, delimiters).
-   String unescaping in `StringEscaper.unescape()` method.
-   Comprehensive test suite with round-trip encode/decode verification.

### Changed

-   Updated README with decode API documentation and examples.

## [0.1.2] - 2025-11-05

### Changed

-   Java version requirement from 21 to 17 for broader compatibility.
-   Refactored `JsonNormalizer` to use if-else statements instead of switch expressions for better readability.
-   Updated dependency: `com.fasterxml.jackson.core:jackson-databind` from 2.18.2 to 2.20.1.
-   Updated dependency: `org.junit:junit-bom` from 5.10.0 to 6.0.1.
-   Updated GitHub Actions: `actions/setup-java` from 4 to 5, `actions/upload-artifact` from 4 to 5, `actions/checkout` from 4 to 5, `softprops/action-gh-release` from 1 to 2.

## [0.1.1] - 2025-10-30

### Added

-   `JToon.encodeJson(String)` and `JToon.encodeJson(String, EncodeOptions)` to encode plain JSON strings directly to TOON.
-   Centralized JSON parsing via `JsonNormalizer.parse(String)` to preserve separation of concerns.
-   Unit tests for JSON string entry point (objects, primitive arrays, tabular arrays, custom options, error cases).
-   README examples for JSON-string encoding, including a Java text block example.
-   This changelog.

### Changed

-   README: Expanded API docs to include `encodeJson` overloads.

## [0.1.0] - 2025-10-30

### Added

-   Initial release.
-   Core encoding of Java objects to TOON with automatic normalization of Java types (numbers, temporals, collections, maps, arrays, POJOs).
-   Tabular array encoding for uniform arrays of objects.
-   Delimiter options (comma, tab, pipe) and optional length marker.
-   Comprehensive README with specification overview and examples.

[Unreleased]: https://github.com/toon-format/toon-java/compare/v1.0.9...HEAD
[1.0.9]: https://github.com/toon-format/toon-java/releases/tag/v1.0.9
[1.0.8]: https://github.com/toon-format/toon-java/releases/tag/v1.0.8
[1.0.7]: https://github.com/toon-format/toon-java/releases/tag/v1.0.7
[1.0.6]: https://github.com/toon-format/toon-java/releases/tag/v1.0.6
[1.0.5]: https://github.com/toon-format/toon-java/releases/tag/v1.0.5
[0.1.4]: https://github.com/toon-format/toon-java/releases/tag/v0.1.4
[0.1.3]: https://github.com/toon-format/toon-java/releases/tag/v0.1.3
[0.1.2]: https://github.com/toon-format/toon-java/releases/tag/v0.1.2
[0.1.1]: https://github.com/toon-format/toon-java/releases/tag/v0.1.1
[0.1.0]: https://github.com/toon-format/toon-java/releases/tag/v0.1.0
