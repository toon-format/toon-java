# Changelog

All notable changes to this project will be documented in this file.

This project adheres to Semantic Versioning and follows a Keep a Changelog-like format.

## [0.1.5] - 2025-11-23

### Added

- Key Folding Option with folding death
- Added unit tests for PrimitiveDecoder to validate handling of primitives and edge cases.
- Added unit tests for StringValidator, covering the updated pattern evaluation order. 

### Changed

- Refactored ObjectEncoder and Flatter to improve structure robustness after spec updates involving key-folding.
- Refactored ValueDecoder (two-step refactor) to better separate logic and improve maintainability.
- Updated test resources to conform to TOON Specification v2.0.1.
- Updated PrimitiveDecoder with improved regex logic for more accurate literal parsing.
- Updated StringValidator pattern ordering: octal is now evaluated before numeric.

## [0.1.4] - 2025-11-20

### Added

- Javadoc generation task (`generateJavadoc`) in build.gradle.
- Specs validation task (`specsValidation`) for conformance testing.
- CODE_OF_CONDUCT.md.
- CONTRIBUTING.md.
- GitHub templates: CODEOWNERS, ISSUE_TEMPLATE (bug_report.yml, feature_request.yml, spec_compliance.yml), PULL_REQUEST_TEMPLATE.md.
- Documentation reorganization: moved TOON-SPECIFICATION.md to docs/FORMAT.md, added docs/README.md.
- Javadoc HTML documentation in docs/javadoc/.

### Changed

- **BREAKING**: Package name migration from `com.felipestanzani.jtoon` to `dev.toonformat.jtoon`.
- **BREAKING**: Maven group ID changed from `com.felipestanzani` to `dev.toonformat`.
- Repository migrated from `felipestanzani/jtoon` to `toon-format/toon-java`.
- Minimum test coverage requirement increased from 85% to 90%.
- LICENSE.md renamed to LICENSE.
- Updated GitHub Actions workflows (build.yml, release.yml).
- Updated Gradle wrapper.
- Updated dependency: `actions/github-script` from 6 to 8.

## [0.1.3] - 2025-11-14

### Added

- Decoding support via `JToon.decode()` and `JToon.decodeToJson()` methods.
- `DecodeOptions` record with `strict` validation mode.
- `decoder` package with full TOON parser supporting all formats (primitives, objects, arrays, delimiters).
- String unescaping in `StringEscaper.unescape()` method.
- Comprehensive test suite with round-trip encode/decode verification.

### Changed

- Updated README with decode API documentation and examples.

## [0.1.2] - 2025-11-05

### Changed

- Java version requirement from 21 to 17 for broader compatibility.
- Refactored `JsonNormalizer` to use if-else statements instead of switch expressions for better readability.
- Updated dependency: `com.fasterxml.jackson.core:jackson-databind` from 2.18.2 to 2.20.1.
- Updated dependency: `org.junit:junit-bom` from 5.10.0 to 6.0.1.
- Updated GitHub Actions: `actions/setup-java` from 4 to 5, `actions/upload-artifact` from 4 to 5, `actions/checkout` from 4 to 5, `softprops/action-gh-release` from 1 to 2.

## [0.1.1] - 2025-10-30

### Added

- `JToon.encodeJson(String)` and `JToon.encodeJson(String, EncodeOptions)` to encode plain JSON strings directly to TOON.
- Centralized JSON parsing via `JsonNormalizer.parse(String)` to preserve separation of concerns.
- Unit tests for JSON string entry point (objects, primitive arrays, tabular arrays, custom options, error cases).
- README examples for JSON-string encoding, including a Java text block example.
- This changelog.

### Changed

- README: Expanded API docs to include `encodeJson` overloads.

## [0.1.0] - 2025-10-30

### Added

- Initial release.
- Core encoding of Java objects to TOON with automatic normalization of Java types (numbers, temporals, collections, maps, arrays, POJOs).
- Tabular array encoding for uniform arrays of objects.
- Delimiter options (comma, tab, pipe) and optional length marker.
- Comprehensive README with specification overview and examples.

[0.1.5]: https://github.com/toon-format/toon-java/releases/tag/v0.1.5
[0.1.4]: https://github.com/toon-format/toon-java/releases/tag/v0.1.4
[0.1.3]: https://github.com/toon-format/toon-java/releases/tag/v0.1.3
[0.1.2]: https://github.com/toon-format/toon-java/releases/tag/v0.1.2
[0.1.1]: https://github.com/toon-format/toon-java/releases/tag/v0.1.1
[0.1.0]: https://github.com/toon-format/toon-java/releases/tag/v0.1.0
