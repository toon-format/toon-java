# Changelog

All notable changes to this project will be documented in this file.

This project adheres to Semantic Versioning and follows a Keep a Changelog-like format.

## [Unreleased]

### Changed

-   Conformance raised from spec 4.1.2 to **4.2**. The 23 conformance fixture files are byte-identical to the spec repository at tag `v4.2.1`.
-   **Non-strict decoding no longer returns `null` on invalid input.** It applies the leniencies the spec names – such as count mismatches, indentation depth jumps and skipped over-indented lines – and throws `IllegalArgumentException` wherever the spec names none (§14).

### Fixed

-   **The encoder no longer drops nested arrays that mix primitives and objects** inside list items.
-   **A line without an unquoted colon is a scalar line, never a header**, and a scalar line outside root primitive position is an error in any mode, including among skipped over-indented lines and as a root scalar followed by other lines (§5, §5.2, §14.2).
-   **A `[`-led line without a well-formed header is a key-value line**: a lone bracket stays part of the key, and in non-strict mode a malformed bracket segment such as `[03]` or `[invalid]` does too. An unclosed quote in an unquoted key opens a span to the end of the line, so no header follows it (§5.2, §6).
-   **Non-strict mode adopts the depth of a jumped first line** in objects, lists, tabular rows and keyed entries; an indented first line of the document is over-indented, `null` and `[]` included (§8, §14.2).
-   **Strict mode rejects a blank line anywhere inside a list's span** and an over-indented line under a list item, while non-strict mode keeps a list item's fields after a blank line (§12, §14.2).
-   **A field list ends its header line**: strict mode rejects inline content after it (`items[1]{a}: 1`) and a malformed keyless header (`[1]{a}}:`) instead of dropping what follows, and non-strict mode reads such a line as a key-value pair. A field list spanning the colon (`[1]{x:y}`) leaves a key-value line in strict mode too, and a keyless header with a field list as a list item (`- [1]{a}:`) is an error in either mode (§6, §14.2).
-   **`maxArraySize` bounds the actual element count** of inline, list, tabular and keyed forms in either mode; only the comparison with the declared `[N]` stays strict-only.
-   **Header parsing:** empty field entries and malformed nested field groups are rejected, a length beyond the `long` range still forms a header, and a line below a bare `key[N]:` header carries no values (§6, §9).
-   **Key and value tokens:** `: 1` decodes as the empty key, `\uXXXX` escapes with a surrogate or a non-ASCII digit are rejected, a carriage return before CRLF is content, a backslash outside quotes is a literal character (`a\: b` has the key `a\`), a list ends at a hyphen without a following space, and tabular rows sit exactly one level below their header in strict mode (§7, §9, §12).

## [2.0.5] - 2026-10-03

### Fixed

-   **Token trimming is now exactly U+0020**, as §12 requires. `String.trim()`, `String.isBlank()` and `String.stripTrailing()` also removed tabs and control characters, so `key: value<tab>` decoded as `value`, and `key: "a"<tab>` was accepted even though §7.4 requires the quoted token to end with its closing quote. A trailing tab at the end of a line is now line content. A single U+FEFF at the very start of a document is still stripped as a byte-order mark (§12).
-   **Any character after the closing quote of a quoted token is now rejected in key position** as well as in value position (§7.4).
-   **Root-form discovery starts at the first non-blank line** instead of assuming line 0, so a document with leading blank lines is no longer misparsed (§5).
-   **An unquoted key token may contain spaces.** `foo bar[2]: 1,2` now decodes with the literal key `foo bar`, which §7.4 requires of decoders. Whitespace between a key and its bracket segment (`foo [2]:`) remains a header syntax error (§6, §14.2).
-   **A root string value starting with U+FEFF is now quoted**, as §7.2 requires; unquoted, a conforming decoder would strip it as a byte-order mark and silently lose the character (§12). This is the one normative behaviour change in spec 4.1.2.

### Changed

-   Conformance raised from spec 4.1.1 to **4.1.2**, which is a single normative change: a root primitive starting with U+FEFF must be quoted (§7.2). On top of that, decoder strictness was tightened in five places where the implementation accepted input the spec rejects — token trimming, quoted-token boundaries, root-form discovery, header key tokens and the `[N]` length marker. The full conformance suite from 4.1 (canonical number formatting, BOM stripping, comment pre-pass §5.1, strict header validation §5/§6/§7.3/§7.4, nested field groups in tabular arrays §9.3, keyed tabular form §9.5/§10, non-strict tab leniency §12) carries over unchanged and remains green. The 23 conformance fixture files are byte-identical to the spec repository at tag `v4.1.2`.
-   Upstream [PR #201](https://github.com/toon-format/toon-java/pull/201) integrated (squash merge). Conflicts in `KeyDecoder`, `ListItemDecoder` and `ValueDecoder` were resolved additively, keeping both the `validateQuotedTokenBoundary` check from #201 and the `validateKeyHasNoUnquotedBrackets` check from #200. `DecodeHelper.trimSpaces()` remains the canonical token trimmer.
-   The targeted specification version is now declared as `toon-spec: 4.1.2` in the README, as §13 recommends.

### Documentation

-   `README.md`: spec badge updated to v4.1.2; corrected a stale quick-start example that showed the non-conforming legacy empty-array form `preferences[0]:` instead of `preferences: []` (§9.1).
-   `docs/FORMAT.md` audited against the spec and corrected. The most serious defect was a flat contradiction: the document stated *"TOON does not support comments"*, while §5.1 defines full-line comments and the decoder has always implemented the comment pre-pass. Also corrected: empty arrays (§9.1, was the stale `items[0]:` form), number notation (§2, was stated as an absolute MUST where the spec only requires canonical decimal inside the canonical range and permits exponent notation outside it), nested-uniform tabular columns (§9.3), decoder key permissiveness (§7.4), and the quoting triggers for a leading `-` or `#` and for a root primitive starting with U+FEFF (§7.2).
-   `docs/FORMAT.md` gained the normative sections that were missing entirely: Keyed Tabular Arrays (§9.5), Header Syntax (§6), Quoted Token Boundaries (§7.4), line terminators and the BOM (§12), the two delimiter scopes (§11.1) and the full strict-mode error set (§14).
-   `util/package-info.java`: the documented unquoted-key pattern was `^[A-Z_][\w.]*$`, which is wrong — it excluded lowercase keys that the encoder in fact emits unquoted. Corrected to the spec's `^[A-Za-z_][A-Za-z0-9_.]*$` (§7.3), with a note that §7.3 constrains encoders only while decoders accept any token. The quoting trigger was documented as `- ` (dash-space) rather than a hyphen at position 0, and the `#` trigger, the root U+FEFF trigger and `Constants.BYTE_ORDER_MARK` were missing.
-   `docs/javadoc/` regenerated. 20 pages had never been generated at all — the checked-in output predated the decoder, encoder and validator packages, so `Headers`, `KeyFolding`, every `decoder/*` class and the whole `validator` package were absent. 88 → 109 pages.

### Build and Tooling

-   Gradle wrapper **9.7.1 → 9.8.0**.
-   NullAway **0.14.1 → 0.14.2**.
-   SpotBugs Gradle plugin **6.5.11 → 6.5.12**.
-   `gradle/verification-metadata.xml` **regenerated from scratch** instead of merged: **3786 → 2821 lines, 522 → 398 components**. The removed entries were artifacts of dependencies that have left the graph. Regenerated under Gradle 9.8.0 and spot-checked against Maven Central's published SHA1 checksums.
-   Removed `gradle/verification-metadata.dryrun.xml`, a leftover from an earlier dry run that Gradle never reads and that nothing in the repository referenced.
-   The `update-verification` workflow no longer lets stale entries accumulate: `--write-verification-metadata` *merges* into an existing file rather than replacing it, so the workflow now deletes the file first and then regenerates. A subsequent verification-enabled build was added as a self-check, since the write mode tolerates verification failures by design.
-   `README.md` and `CONTRIBUTING.md` document the delete-then-regenerate procedure, so the merge behaviour is not reintroduced locally.
-   Checkstyle is clean again: `PrimitiveEncoder` overloads reordered, and the U+FEFF literal in `Constants` expressed without an escaped unicode character.

### Tests

-   New `HeadersTest` coverage for §7.4 key tokens: keys containing a hyphen, a leading digit, an internal space, a tab, a non-breaking space and a quoted key; rejection of a space before the bracket segment and before the colon, and of content between the bracket segment and the colon.
-   New `PrimitiveEncoderTest` coverage for the §7.2 root byte-order-mark rule: a root string starting with U+FEFF is quoted, a bare U+FEFF is quoted, and U+FEFF stays unquoted both in non-root position and in the interior of a root string.
-   Full suite: **1870 tests, 0 failures**, with dependency verification enabled.

## [2.0.4] - 2026-09-07

### Fixed

-   Releases triggered from the GitHub UI failed, because the release workflow assumed the version came from a gradle property it now derives from the git tag.
-   The release workflow gained a `workflow_dispatch` trigger, so a patch can be cut without pushing a tag first.

## [2.0.3] - 2026-09-07

### Changed

-   Conformance raised to spec **4.1.1**: a decode fixture for the comment change was added and `ValueDecoder` adapted to it.
-   Jackson **3.2.1 → 3.2.2** (`jackson-databind`, `jackson-module-blackbird`).
-   NullAway **0.14.0 → 0.14.1**.
-   JSpecify **1.0.0 → 1.0.1**.
-   Gradle wrapper **9.6.1 → 9.7.0 → 9.7.1**.
-   SpotBugs plugin **6.5.9 → 6.5.10**, OWASP dependency-check **12.2.2 → 13.0.0**.
-   CI dependency bumps: `actions/checkout` 7.0.0 → 7.0.1, `actions/setup-java` 5.4.0 → 6.0.0, `softprops/action-gh-release` 3.0.1 → 3.0.3, `EndBug/add-and-commit` 10.0.0 → 11.1.1.

## [2.0.2] - 2026-07-31

### Changed

-   Conformance raised to spec **4.1**, the largest decoder change so far. New `FatalDecodeException` for strict-mode violations, new `KeyedObjectDecoder` and `KeyedObjectEncoder` for the keyed tabular form (§9.5), and a rewrite of `ArrayDecoder`, `DecodeHelper`, `KeyDecoder`, `ListItemDecoder`, `ObjectDecoder`, `PrimitiveDecoder`, `TabularArrayDecoder`, `ValueDecoder`, `ArrayEncoder` and `HeaderFormatter` to enforce strict header validation (§5, §6, §7.3, §7.4). Adds canonical number formatting, BOM stripping, the comment pre-pass (§5.1), nested field groups in tabular arrays (§9.3) and keyed headers on list-item hyphen lines (§9.5, §10).
-   Decode conformance fixtures refreshed for spec 3.3.2, including new cases for validation errors, objects and arrays; the keyed-array pattern was reordered.
-   NullAway **0.13.7 → 0.13.8**, SpotBugs plugin **6.5.8 → 6.5.9**, CycloneDX BOM **3.2.4 → 3.3.0**.

### Documentation

-   README and CONTRIBUTING now document the dependency-verification regeneration command.

## [2.0.1] - 2026-07-11

### Added

-   NullAway + Error Prone integration for compile-time null safety checks.
-   SECURITY.md with vulnerability reporting policy.
-   OWASP dependency check integration and CycloneDX SBOM generation.
-   Checkstyle enforcement for test code.
-   GitHub Actions workflow for dependency validation.

### Changed

-   Upgraded Jackson dependencies to 3.2.1.
-   Upgraded JUnit BOM to 6.1.1.
-   Upgraded SpotBugs to 6.5.8, spotbugs-annotations to 4.10.2.
-   Upgraded Gradle wrapper to 9.6.1.
-   Stricter PMD rules and SpotBugs configured to fail on warnings in main source set.
-   Null-safety improvements across the entire codebase.
-   Fixed catastrophic backtracking in `StringValidator` for random/fuzzy input.

### Security

-   Added dependency verification metadata for reproducible builds.
-   Enforced SHA-256 checksum verification for Gradle wrapper.

## [2.0.0] - 2026-05-21

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

## [0.1.1] - 2025-10-31

### Added

-   `JToon.encodeJson(String)` and `JToon.encodeJson(String, EncodeOptions)` to encode plain JSON strings directly to TOON.
-   Centralized JSON parsing via `JsonNormalizer.parse(String)` to preserve separation of concerns.
-   Unit tests for JSON string entry point (objects, primitive arrays, tabular arrays, custom options, error cases).
-   README examples for JSON-string encoding, including a Java text block example.
-   This changelog.

### Changed

-   README: Expanded API docs to include `encodeJson` overloads.

## [0.1.0] - 2025-10-28

### Added

-   Initial release.
-   Core encoding of Java objects to TOON with automatic normalization of Java types (numbers, temporals, collections, maps, arrays, POJOs).
-   Tabular array encoding for uniform arrays of objects.
-   Delimiter options (comma, tab, pipe) and optional length marker.
-   Comprehensive README with specification overview and examples.

[Unreleased]: https://github.com/toon-format/toon-java/compare/v2.0.1...HEAD
[2.0.1]: https://github.com/toon-format/toon-java/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/toon-format/toon-java/compare/v1.0.9...v2.0.0
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
