# JToon – TOON Format for Java

[![Build](https://github.com/toon-format/toon-java/actions/workflows/build.yml/badge.svg)](https://github.com/toon-format/toon-java/actions/workflows/build.yml)
[![Release](https://github.com/toon-format/toon-java/actions/workflows/release.yml/badge.svg)](https://github.com/toon-format/toon-java/actions/workflows/release.yml)
[![Maven Central](https://img.shields.io/maven-central/v/dev.toonformat/jtoon.svg)](https://central.sonatype.com/artifact/dev.toonformat/jtoon)
![Coverage](.github/badges/jacoco.svg)
[![SPEC v4.4.0](https://img.shields.io/badge/spec-v4.4.0-fef3c0?labelColor=1b1b1f)](https://github.com/toon-format/spec)
[![License: MIT](https://img.shields.io/badge/license-MIT-fef3c0?labelColor=1b1b1f)](./LICENSE)

Compact, human-readable serialization format for LLM contexts with **30-60% token reduction** vs JSON. Combines YAML-like indentation with CSV-like tabular arrays. Working towards full compatibility with the [official TOON specification](https://github.com/toon-format/spec).

**Key Features:** Minimal syntax • TOON Encoding and Decoding • Tabular arrays for uniform data • Array length validation • Java 17 & 21 • full [Jackson Annotation](https://github.com/FasterXML/jackson-annotations) Support • Null-safe by default (NullAway + JSpecify) • Comprehensive test coverage.

## Java Version Support

| Branch | Java | Version | Status |
|--------|------|---------|--------|
| `main` | 21 | 3.0.0+ | Active development |
| `java17` | 17 | 2.0.x | Maintenance (until Q4 2027) |

### Java 21 (3.0.0+)

The `main` branch targets **Java 21** and uses the native `Double.toString` (shortest digits since JDK 19). Full spec compliance.

> **Note:** Version 3.0.0 requires **Java 21**. Java 17 users should use the `java17` branch (2.0.x).

### Java 17 (2.0.x)

The `java17` branch maintains the 2.0.x line for Java 17 users. It uses [Jackson's Schubfach-based `DoubleToDecimal`](https://fasterxml.github.io/jackson-core/javadoc/2.14/com/fasterxml/jackson/core/io/schubfach/DoubleToDecimal.html) for shortest round-trip digits (spec §4.2), since `Double.toString` only delivers shortest digits from JDK 19+ (JDK-4511638).

> **Note:** Java 17 (September 2021) does not natively support spec §4.2 for shortest round-trip digits. Jackson's Schubfach implementation provides spec-compliant output on Java 17.

## Git Branches

- Raise `main` to Java 21 (3.0.0)
- Establish `java17` as long-term maintenance branch

## Installation

### Maven Central

JToon is available on Maven Central. Add it to your project using your preferred build tool:

**Gradle (Groovy DSL):**

```gradle
dependencies {
    implementation 'dev.toonformat:jtoon:3.0.0'
}
```

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation("dev.toonformat:jtoon:3.0.0")
}
```

**Maven:**

```xml
<dependency>
    <groupId>dev.toonformat</groupId>
    <artifactId>jtoon</artifactId>
    <version>3.0.0</version>
</dependency>
```

> **Note:** See the [latest version](https://central.sonatype.com/artifact/dev.toonformat/jtoon) on Maven Central (also shown in the badge above).

### Alternative: Manual Installation

You can also download the JAR directly from the [GitHub Releases](https://github.com/toon-format/toon-java/releases) page and add it to your project's classpath.

## Quick Start

```java
import dev.toonformat.jtoon.JToon;
import java.util.*;

record User(int id, String name, List<String> tags, boolean active, List<?> preferences) {}
record Data(User user) {}

User user = new User(123, "Ada", List.of("reading", "gaming"), true, List.of());
Data data = new Data(user);

System.out.println(JToon.encode(data));
```

**Output:**

```yaml
user:
  id: 123
  name: Ada
  tags[2]: reading,gaming
  active: true
  preferences: []
```

## Type Conversions

Some Java-specific types are automatically normalized for LLM-safe output:

| Input Type                  | Output                                                     |
| --------------------------- | ---------------------------------------------------------- |
| Number (finite)             | Decimal form; `-0` → `0`; whole numbers as integers        |
| Number (`NaN`, `±Infinity`) | `null`                                                     |
| `BigInteger`                | Integer if within Long range, otherwise string (no quotes) |
| `BigDecimal`                | Decimal number                                             |
| `LocalDateTime`             | ISO date-time string in quotes                             |
| `LocalDate`                 | ISO date string in quotes                                  |
| `LocalTime`                 | ISO time string in quotes                                  |
| `ZonedDateTime`             | ISO offset date-time string in quotes                      |
| `OffsetDateTime`            | ISO offset date-time string in quotes                      |
| `Instant`                   | ISO instant string in quotes                               |
| `java.util.Date`            | ISO instant string in quotes                               |
| `Optional<T>`               | Unwrapped value or `null` if empty                         |
| `Stream<T>`                 | Materialized to array                                      |
| `Map`                       | Object with string keys                                    |
| `Collection`, arrays        | Arrays                                                     |

## API

### Method Overview

| Method | Returns | Description                                |
|---|---|--------------------------------------------|
| `encode(Object)` | `String` | Java Object → TOON                         |
| `encode(Object, EncodeOptions)` | `String` | as above, with options                     |
| `encodeJson(String)` | `String` | JSON String → TOON                         |
| `encodeJson(String, EncodeOptions)` | `String` | as above, with options                     |
| `decode(String)` | `Object` | TOON → Java Object                         |
| `decode(String, DecodeOptions)` | `Object` | as above, with options                     |
| `decodeToJson(String)` | `String` | TOON → JSON String                         |
| `decodeToJson(String, DecodeOptions)` | `String` | as above, with options                     |
| `configure(ObjectMapper)` | `void` | Set custom ObjectMapper (before first use) |
| `createMapper(JacksonModule...)` | `ObjectMapper` | Default config + modules                   |

### Encode

```java
String encode(Object value);
String encode(Object value, EncodeOptions options);
String encodeJson(String json);
String encodeJson(String json, EncodeOptions options);
```

`EncodeOptions` (Defaults: `indent=2`, `delimiter=COMMA`, `flatten=OFF`, `flattenDepth=Integer.MAX_VALUE`):

| Field | Type | Default | Description |
|---|---|---|---|
| `indent` | `int` | `2` | Spaces per level |
| `delimiter` | `Delimiter` | `COMMA` | Tabular/inline delimiter |
| `flatten` | `KeyFolding` | `OFF` | Key folding for nested objects |
| `flattenDepth` | `int` | `MAX` | Max. depth for folding |

Factory methods: `withIndent`, `withDelimiter`, `withFlatten`, `withFlattenDepth`.

```java
import dev.toonformat.jtoon.JToon;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;

record Item(String sku, int qty, double price, @JsonIgnore double internPrice) {}
record Data(List<Item> items) {}

Item item1 = new Item("A1", 2, 9.99, 8.50);
Item item2 = new Item("B2", 1, 14.5, 14.0);
Data data = new Data(List.of(item1, item2));

System.out.println(JToon.encode(data));
```

```yaml
items[2]{sku,qty,price}:
  A1,2,9.99
  B2,1,14.5
```

The [Jackson Annotation](https://github.com/FasterXML/jackson-annotations) `@JsonIgnore` keeps fields from being exposed.

#### Encode a plain JSON string

```java
String json = """
{
  "user": {
    "id": 123,
    "name": "Ada",
    "tags": ["reading", "gaming"]
  }
}
""";
System.out.println(JToon.encodeJson(json));
```

```yaml
user:
  id: 123
  name: Ada
  tags[2]: reading,gaming
```

### Decode

```java
Object decode(String toon);
Object decode(String toon, DecodeOptions options);
String decodeToJson(String toon);
String decodeToJson(String toon, DecodeOptions options);
```

`DecodeOptions` (Defaults: `indent=2`, `delimiter=COMMA`, `strict=true`, `expandPaths=OFF`, `maxDepth=512`, `maxArraySize=10_000_000`, `maxStringLength=10_000_000`):

| Field | Type | Default | Description |
|---|---|---|---|
| `indent` | `int` | `2` | Spaces per level |
| `delimiter` | `Delimiter` | `COMMA` | Expected tabular/inline delimiter |
| `strict` | `boolean` | `true` | Strict validation; when `false`, spec-specific leniencies apply |
| `expandPaths` | `PathExpansion` | `OFF` | Path expansion for dotted keys |
| `maxDepth` | `int` | `512` | Max. nesting depth |
| `maxArraySize` | `int` | `10_000_000` | Max. elements per array/object |
| `maxStringLength` | `int` | `10_000_000` | Max. string length |

Factory methods: `withIndent`, `withDelimiter`, `withStrict`.

```java
import dev.toonformat.jtoon.JToon;

String toon = """
    users[2]{id,name,role}:
      1,Alice,admin
      2,Bob,user
    """;

Object result = JToon.decode(toon);          // Java objects
String json  = JToon.decodeToJson(toon);     // JSON string
```

#### Round-Trip

```java
import dev.toonformat.jtoon.*;
import java.util.*;

Map<String, Object> data = new LinkedHashMap<>();
data.put("id", 123);
data.put("name", "Ada");
data.put("tags", Arrays.asList("dev", "admin"));

String toon = JToon.encode(data);
Object decoded = JToon.decode(toon);   // Integers decode as Long
```

#### Custom Decode Options

```java
import dev.toonformat.jtoon.*;

String toon = "tags[3|]: a|b|c";
DecodeOptions pipe = DecodeOptions.withDelimiter(Delimiter.PIPE);
Object result = JToon.decode(toon, pipe);

String mismatched = "tags[3]: a,b";
DecodeOptions lenient = DecodeOptions.withStrict(false);
Object result2 = JToon.decode(mismatched, lenient);
```

### Delimiter

| Delimiter | Symbol | Notes |
|---|---|---|
| `COMMA` | `,` | Default |
| `TAB` | `\t` | Less escaping, efficient tokenization |
| `PIPE` | `\|` | Compromise |

```java
import dev.toonformat.jtoon.*;
import java.util.*;

record Item(String sku, String name, int qty, double price) {}
record Data(List<Item> items) {}

Data data = new Data(List.of(
    new Item("A1", "Widget", 2, 9.99),
    new Item("B2", "Gadget", 1, 14.5)));

// Tab
System.out.println(JToon.encode(data, new EncodeOptions(2, Delimiter.TAB, KeyFolding.OFF, 3)));
// Pipe
System.out.println(JToon.encode(data, new EncodeOptions(2, Delimiter.PIPE, KeyFolding.OFF, 3)));
```

### Jackson ObjectMapper / Optimizer Modules

JToon uses a shared, thread-safe `ObjectMapper` internally. By default **no optimizer
module** (Blackbird/Afterburner) is registered, which keeps JToon usable on Java 17+
with any Jackson 3.x release — including Jackson 3.3.0, whose `BlackbirdModule`
requires Java 25.

Applications that want the performance boost of an optimizer module can plug in their
own `ObjectMapper` **before the first** encode/decode call:

```java
import dev.toonformat.jtoon.JToon;
import tools.jackson.databind.JacksonModule;
import tools.jackson.module.afterburner.AfterburnerModule;
import tools.jackson.module.blackbird.BlackbirdModule;

// Java 17+ (Afterburner works on Java 17 and later)
JToon.configure(JToon.createMapper(new AfterburnerModule()));

// Java 25+ (Blackbird is faster but requires Java 25)
JToon.configure(JToon.createMapper(new BlackbirdModule()));
```

`JToon.configure(ObjectMapper)` and `JToon.createMapper(JacksonModule...)` are the
entry points; `ObjectMapperSingleton` exposes the same helpers. Calling `configure`
after the first encode/decode throws `IllegalStateException`.

**CI/CD:** GitHub Actions • Java 21 • Coverage enforcement • PR coverage comments

## Development

```bash
# Build (includes tests, checks, coverage)
./gradlew build

# Run tests only
./gradlew test

# Update dependency verification metadata (required when adding/updating dependencies).
# Delete first: --write-verification-metadata merges, it does not replace.
rm -f gradle/verification-metadata.xml
./gradlew --write-verification-metadata sha256 build cyclonedxBom -x test
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for full development guidelines.

## Project Status

`toon-spec: 4.4.0`

This project is 100% compliant with TOON specification 4.4.0 Release conformance enforced on CI/CD.

## Documentation

- [Full Documentation](docs/) - Extended guides and references
- [API Reference](https://javadoc.io/doc/dev.toonformat/jtoon) - Detailed Javadoc
- [Format Specification](docs/FORMAT.md) - TOON syntax and rules
- [TOON Spec](https://github.com/toon-format/spec) - Official specification
- [Issues](https://github.com/toon-format/toon-java/issues) - Bug reports and features
- [Contributing](CONTRIBUTING.md) - Contribution guidelines

## License

MIT License – see [LICENSE](LICENSE) for details
