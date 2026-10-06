# Documentation

For a complete API documentation see the library [Javadoc](https://javadoc.io/doc/dev.toonformat/jtoon)

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

Number normalization examples:

```
-0    → 0
1e6   → 1000000
1e-6  → 0.000001
```

## API

### `JToon.encode(Object value): String`

### `JToon.encode(Object value, EncodeOptions options): String`

### `JToon.encodeJson(String json): String`

### `JToon.encodeJson(String json, EncodeOptions options): String`

Converts any Java object or JSON-string to TOON format.

**Parameters:**

- `value` – Any Java object (Map, List, primitive, or nested structure). Non-serializable values are converted to `null`. Java temporal types are converted to ISO strings, Optional is unwrapped, and Stream is materialized.
- `options` – Optional encoding options (`EncodeOptions` record):
  - `indent` – Number of spaces per indentation level (default: `2`)
  - `delimiter` – Delimiter enum for array values and tabular rows: `Delimiter.COMMA` (default), `Delimiter.TAB`, or `Delimiter.PIPE`

For `encodeJson` overloads:

- `json` – A valid JSON string to be parsed and encoded. Invalid or blank JSON throws `IllegalArgumentException`.

**Returns:**

A TOON-formatted string with no trailing newline or spaces.

**Example:**

```java
import dev.toonformat.jtoon.JToon;
import java.util.*;

record Item(String sku, int qty, double price) {}
record Data(List<Item> items) {}

Item item1 = new Item("A1", 2, 9.99);
Item item2 = new Item("B2", 1, 14.5);
Data data = new Data(List.of(item1, item2));

System.out.println(JToon.encode(data));
```

**Output:**

```
items[2]{sku,qty,price}:
  A1,2,9.99
  B2,1,14.5
```

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

Output:

```
user:
  id: 123
  name: Ada
  tags[2]: reading,gaming
```

#### Delimiter Options

The `delimiter` option allows you to choose between comma (default), tab, or pipe delimiters for array values and tabular rows. Alternative delimiters can provide additional token savings in specific contexts.

##### Tab Delimiter (`\t`)

Using tab delimiters instead of commas can reduce token count further, especially for tabular data:

```java
import dev.toonformat.jtoon.*;
import java.util.*;

record Item(String sku, String name, int qty, double price) {}

record Data(List<Item> items) {}

Item item1 = new Item("A1", "Widget", 2, 9.99);
Item item2 = new Item("B2", "Gadget", 1, 14.5);
Data data = new Data(List.of(item1, item2));

EncodeOptions options = new EncodeOptions(2, Delimiter.TAB, KeyFolding.OFF, 3);
System.out.println(JToon.encode(data, options));
```

**Output:**

```
items[2 ]{sku name qty price}:
  A1 Widget 2 9.99
  B2 Gadget 1 14.5
```

**Benefits:**

- Tabs are single characters and often tokenize more efficiently than commas.
- Tabs rarely appear in natural text, reducing the need for quote-escaping.
- The delimiter is explicitly encoded in the array header, making it self-descriptive.

**Considerations:**

- Some terminals and editors may collapse or expand tabs visually.
- String values containing tabs will still require quoting.

##### Pipe Delimiter (`|`)

Pipe delimiters offer a middle ground between commas and tabs:

```java
// Using the same Item and Data records from above
EncodeOptions options = new EncodeOptions(2, Delimiter.PIPE, KeyFolding.OFF, 3);
System.out.println(JToon.encode(data, options));
```

**Output:**

```
items[2|]{sku|name|qty|price}:
  A1|Widget|2|9.99
  B2|Gadget|1|14.5
```

### `JToon.decode(String toon): Object`

### `JToon.decode(String toon, DecodeOptions options): Object`

### `JToon.decodeToJson(String toon): String`

### `JToon.decodeToJson(String toon, DecodeOptions options): String`

Converts TOON-formatted strings back to Java objects or JSON.

**Parameters:**

- `toon` – TOON-formatted input string
- `options` – Optional decoding options (`DecodeOptions` record):
  - `indent` – Number of spaces per indentation level (default: `2`)
  - `delimiter` – Expected delimiter: `Delimiter.COMMA` (default), `Delimiter.TAB`, or `Delimiter.PIPE`
  - `strict` – Boolean for validation mode. When `true` (default), throws `IllegalArgumentException` on invalid input. When `false`, applies the spec's non-strict leniencies, such as count mismatches, depth jumps and skipped over-indented lines, and throws wherever the spec names none.

**Returns:**

For `decode`: A Java object (`Map` for objects, `List` for arrays, primitives for scalars, or `null`)

For `decodeToJson`: A JSON string representation

**Example:**

```java
import dev.toonformat.jtoon.JToon;

String toon = """
    users[2]{id,name,role}:
      1,Alice,admin
      2,Bob,user
    """;

// Decode to Java objects
Object result = JToon.decode(toon);

// Decode directly to JSON string
String json = JToon.decodeToJson(toon);
```

#### Round-Trip Conversion

```java
import dev.toonformat.jtoon.*;
import java.util.*;

// Original data
Map<String, Object> data = new LinkedHashMap<>();
data.put("id", 123);
data.put("name", "Ada");
data.put("tags", Arrays.asList("dev", "admin"));

 // Encode to TOON
String toon = JToon.encode(data);

// Decode back to objects
 Object decoded = JToon.decode(toon);

// Values are preserved (note: integers decode as Long)
```

#### Custom Decode Options

```java
import dev.toonformat.jtoon.*;

String toon = "tags[3|]: a|b|c";

// Decode with pipe delimiter
DecodeOptions options = DecodeOptions.withDelimiter(Delimiter.PIPE);
Object result = JToon.decode(toon, options);

// Non-strict mode: a count mismatch decodes the values that are present
String mismatchedToon = "tags[3]: a,b";
DecodeOptions lenient = DecodeOptions.withStrict(false);
Object result2 = JToon.decode(mismatchedToon, lenient);
```
