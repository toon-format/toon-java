# TOON Format Specification

Detailed format rules, syntax, and examples for TOON (Token-Oriented Object Notation).

## Overview

TOON uses indentation-based structure like YAML for nested objects and tabular format like CSV for uniform arrays. This document explains the complete syntax and formatting rules.

---

## Objects

Objects use `key: value` pairs with indentation for nesting.

### Simple Objects

```python
{"name": "Alice", "age": 30, "active": True}
```

```toon
name: Alice
age: 30
active: true
```

### Nested Objects

```python
{
    "user": {
        "name": "Alice",
        "settings": {
            "theme": "dark"
        }
    }
}
```

```toon
user:
  name: Alice
  settings:
    theme: dark
```

### Object Keys

Encoders emit a key unquoted only when it matches `^[A-Za-z_][A-Za-z0-9_.]*$` (§7.3); anything else is quoted:

**Decoders are deliberately more permissive** (§7.4): they must accept *any* unquoted token before the `:` as a literal key, even when it does not match that pattern. `foo-bar: 1`, `foo-bar[2]: 1,2` and `items[1]{2key}:` are all valid input, and a space *inside* a key is part of the key.

```python
{
    "simple_key": 1,
    "with-dash": 2,
    "123": 3,           # Numeric key
    "with space": 4,    # Spaces require quotes
    "": 5               # Empty key requires quotes
}
```

```toon
simple_key: 1
with-dash: 2
"123": 3
"with space": 4
"": 5
```

---

## Arrays

All arrays include a length indicator `[N]` for validation, except empty arrays, which are emitted as `key: []` in field position or `[]` in root position (§9.1).

### Primitive Arrays

Arrays of primitives use inline format with comma separation:

```python
[1, 2, 3, 4, 5]
```

```toon
[5]: 1,2,3,4,5
```

```python
["alpha", "beta", "gamma"]
```

```toon
[3]: alpha,beta,gamma
```

**Note:** Comma delimiter is hidden in primitive arrays: `[5]:` not `[5,]:`

### Tabular Arrays

Uniform objects with primitive-only fields use CSV-like format:

```python
[
    {"id": 1, "name": "Alice", "age": 30},
    {"id": 2, "name": "Bob", "age": 25},
    {"id": 3, "name": "Charlie", "age": 35}
]
```

```toon
[3,]{id,name,age}:
  1,Alice,30
  2,Bob,25
  3,Charlie,35
```

**Tabular Format Rules:**

- All objects must have identical keys
- Every column must be *uniform-primitive* or *nested-uniform*: nested objects are allowed when every row shares the same key set and every sub-column is itself uniform, at unbounded depth (§9.3)
- Field order in header determines column order
- Delimiter appears in header: `[N,]` or `[N|]` or `[N\t]`

### Keyed Tabular Arrays

An object whose values are *all uniform objects* encodes as a table whose rows carry their own keys (§9.5). The bracket segment gains a `:` keyed marker:

```python
{
  "users": {
    "alice": {"id": 1, "name": "Ada"},
    "bob":   {"id": 2, "name": "Bob"}
  }
}
```

```toon
users[2:]{id,name}:
  alice: 1,Ada
  bob: 2,Bob
```

In root position the key and its bracket segment are omitted, leaving a keyless keyed header:

```toon
[2:]{id,name}:
  alice: 1,Ada
  bob: 2,Bob
```

Each entry row is `entrykey: c1,c2,…`. This form is also what a list item's `- ` line uses when the item is such an object (§10).

### List Arrays

Non-uniform or nested arrays use list format with `-` markers:

```python
[
    {"name": "Alice"},
    42,
    "hello"
]
```

```toon
[3]:
  - name: Alice
  - 42
  - hello
```

### Nested Arrays

```python
{
    "matrix": [
        [1, 2, 3],
        [4, 5, 6]
    ]
}
```

```toon
matrix[2]:
  - [3]: 1,2,3
  - [3]: 4,5,6
```

### Empty Arrays

```python
{"items": []}
```

```toon
items: []
```

An empty array in root position is a bare `[]` on its own line. The legacy `items[0]:` / `[0]:` header form MUST NOT be emitted (§9.1).

---

## Header Syntax

A header line is `key[N<delim?>]{fields}:` — optionally with the key and/or the field spec absent. Two structural rules are enforced in strict mode (§6):

- **No whitespace between a key and its bracket segment.** `foo [2]:` is an error, even though `foo bar[2]:` is valid input with the literal key `foo bar` (§7.4).
- **No content between `]` and the following `{` or `:`.** `[2] :` and `[2]extra:` are errors.

| Input | Valid | Reason |
|---|---|---|
| `items[2]:` | yes | the canonical form |
| `foo bar[2]:` | yes | the space belongs to the key, so the key is `foo bar` (§7.4) |
| `foo [2]:` | no | whitespace between key and bracket segment (§6) |
| `items[2] :` | no | whitespace between `]` and `:` (§6) |
| `items[2]extra:` | no | content between `]` and `:` (§6) |

---

## Delimiters

Two delimiter scopes exist (§11.1), and confusing them is a common source of wrong output:

- The **document delimiter** governs delimiter-aware quoting for object field values (`key: value`) and for root primitives.
- The **active delimiter** — the symbol declared in a header's bracket segment — governs quoting *inside that header's scope* only: inline array values, tabular row cells, and keyed entry-row cells (§9.5).

A header without a delimiter symbol always means comma, regardless of the document delimiter.

Three delimiter options for array values:

### Comma (Default)

```python
encode([1, 2, 3])  # Default delimiter
```

```toon
[3]: 1,2,3
```

For tabular arrays, delimiter shown in header:

```toon
users[2,]{id,name}:
  1,Alice
  2,Bob
```

### Tab

```python
encode([1, 2, 3], {"delimiter": "\t"})
```

```toon
[3 ]: 1 2 3
```

Tabular with tab:

```toon
users[2 ]{id,name}:
  1 Alice
  2 Bob
```

### Pipe

```python
encode([1, 2, 3], {"delimiter": "|"})
```

```toon
[3|]: 1|2|3
```

Tabular with pipe:

```toon
users[2|]{id,name}:
  1|Alice
  2|Bob
```

---

## String Quoting Rules

Strings are quoted **only when necessary** to avoid ambiguity.

### Unquoted Strings (Safe)

```python
"hello"          # Simple identifier
"hello world"    # Internal spaces OK
"user_name"      # Underscores OK
"hello-world"    # Hyphens OK
```

```toon
hello
hello world
user_name
hello-world
```

### Quoted Strings (Required)

**Empty strings:**

```python
""
```

```toon
""
```

**Reserved keywords:**

```python
"null"
"true"
"false"
```

```toon
"null"
"true"
"false"
```

**Numeric-looking strings:**

```python
"42"
"-3.14"
"1e5"
"0123"  # Leading zero
```

```toon
"42"
"-3.14"
"1e5"
"0123"
```

**Leading/trailing whitespace:**

```python
" hello"
"hello "
" hello "
```

```toon
" hello"
"hello "
" hello "
```

**Structural characters:**

```python
"key: value"     # Colon
"[array]"        # Brackets
"{object}"       # Braces
```

```toon
"key: value"
"[array]"
"{object}"
```

**Leading `-` or `#`:**

A string that equals `-` or starts with `-`, or equals `#` or starts with `#`, must be quoted — a hyphen at *any* position 0 counts, not just `- `:

```python
"-"
"- item"
"#"
"#tag"
```

```toon
"-"
"- item"
"#"
"#tag"
```

**Root primitive starting with U+FEFF:**

A string in root position that starts with the byte-order mark `U+FEFF` must be quoted. Unquoted, a decoder strips that character as a BOM (§12) before any other processing and reads a *different value*: the string below comes back as the number `8`.

```python
"﻿8"            # the string U+FEFF followed by 8
```

```toon
"﻿8"
```

The character is invisible in the blocks above — it is written `\ufeff` here so that you can see it. TOON does not escape it; only the two characters `\` and `"` and the control characters are escaped, so the encoded form really does contain the raw U+FEFF.

**Delimiter characters:**

```python
# When using comma delimiter
"a,b"
```

```toon
"a,b"
```

**Control characters:**

```python
"line1\nline2"
"tab\there"
```

```toon
"line1\nline2"
"tab\there"
```

### Quoted Token Boundaries

A token whose first character, after the §12 trimming, is `"` must be a *complete* quoted token: its closing `"` has to be the token's last character. Any character after that closing quote is an error (§7.4) — in key position as well as in value position.

| Input | Valid | Reason |
|---|---|---|
| `"valid": 1` | yes | the closing quote ends the token |
| `"a"b: 1` | no | `b` follows the closing quote, so the token is not a complete quoted token (§7.4) |

### Escape Sequences

Inside quoted strings:

| Sequence | Meaning |
|----------|---------|
| `\"` | Double quote |
| `\\` | Backslash |
| `\n` | Newline |
| `\r` | Carriage return |
| `\t` | Tab |
| `\uXXXX` | Unicode character (4 hex digits) |

**Example:**

```python
{
    "text": "Hello \"world\"\nNew line",
    "path": "C:\\Users\\Alice"
}
```

```toon
text: "Hello \"world\"\nNew line"
path: "C:\\Users\\Alice"
```

---

## Primitives

### Numbers

**Integers:**

```python
42
-17
0
```

```toon
42
-17
0
```

**Floats:**

```python
3.14
-0.5
0.0
```

```toon
3.14
-0.5
0
```

**Special Numbers:**

- **Scientific notation accepted in decoding:** `1e5`, `-3.14E-2`
- **Encoders emit canonical decimal form** for `0` or `1e-6 <= |n| < 1e21`; outside that range exponent notation is permitted (§2), and this implementation keeps plain decimal form
- **Negative zero normalized:** `-0.0` → `0`
- **Non-finite values → null:** `Infinity`, `-Infinity`, `NaN` → `null`

**Large integers (>2^53-1):**

```python
9007199254740993  # Exceeds JS safe integer
```

```toon
"9007199254740993"
```

It is quoted so that a JavaScript consumer does not read it as a lossy `number` (§7.2).

### Booleans

```python
True   # true in TOON (lowercase)
False  # false in TOON (lowercase)
```

```toon
true
false
```

### Null

```python
None  # null in TOON (lowercase)
```

```toon
null
```

---

## Indentation

Default: 2 spaces per level (configurable)

```python
{
    "level1": {
        "level2": {
            "level3": "value"
        }
    }
}
```

```toon
level1:
  level2:
    level3: value
```

**With 4-space indent:**

```python
encode(data, {"indent": 4})
```

```toon
level1:
    level2:
        level3: value
```

**Strict mode rules:**

- Indentation must be consistent multiples of `indent` value
- Tabs not allowed in indentation
- Mixing spaces and tabs causes errors

Strict mode additionally enforces the whole §14 error set, not just indentation: malformed headers and field lists (§6), array count and tabular width mismatches, indentation jumps and over-indented lines, any character after a quoted token's closing quote (§7.4), and — once a root array, an empty root `[]`, or a keyed tabular root object is complete — any further non-comment, non-blank line (§5).

**Non-strict mode (§12 leniency):**

- Depth may be computed as `floor(leadingSpaces / indent)`
- Leading tabs are accepted as indentation and removed from the line's content before classification (§5.2). Depth computation for tabs is implementation-defined: JToon expands each leading tab to `indent` spaces, so a leading tab contributes exactly one indentation level.
- Because comment detection precedes the tab leniency (§5.1), a tab-indented `#` line is data, not a comment.

---

## Array Length Indicators

All arrays include `[N]` to indicate element count for validation.

```toon
items[3]: a,b,c
users[2,]{id,name}:
  1,Alice
  2,Bob
```

---

## Blank Lines

**Within arrays:** Blank lines are **not allowed** in strict mode

```toon
# ❌ Invalid (blank line in array)
items[3]:
  - a

  - b
  - c
```

```toon
# ✅ Valid (no blank lines)
items[3]:
  - a
  - b
  - c
```

**Between top-level keys:** Blank lines are allowed and ignored

```toon
# ✅ Valid (blank lines between objects)
name: Alice

age: 30
```

---

## Comments

TOON supports **full-line comments** (§5.1). A line whose first character after zero or more leading spaces is `#` is a comment; decoders remove comment lines in a lexical pre-pass, in strict and non-strict mode alike.

```toon
# this line is a comment
name: Ada
```

Two restrictions matter:

- **Only spaces may precede the `#`.** A line whose leading whitespace contains a tab is *not* a comment line — the `#` is data.
- **Comments are full-line only.** There is no inline or trailing comment form, so a `#` anywhere else on a line is ordinary content: `name: Ada # kept as content`.

---

## Whitespace

### Line Terminators and the BOM

- A `CR` (`U+000D`) at end of line is a line terminator and MUST be excluded by decoders, so `CRLF` input decodes identically to `LF` input (§12).
- A single `U+FEFF` at the very start of a document is a byte-order mark and MUST be removed by decoders (§12). Anywhere else it is ordinary content.
- Tabs must not be used for indentation (§12). Non-strict mode may accept leading tabs as indentation and expand them to `indent` spaces; the encoder never emits them.

### Trailing Whitespace

Trailing **spaces** (`U+0020`) are **allowed and ignored** — decoders MUST trim them when extracting tokens (§12). That trimming applies to exactly `U+0020` and to nothing else, so a trailing **tab** is line content rather than whitespace to discard.

Trimming does **not** apply between a key and its bracket segment; whitespace there is a header syntax error (§6).

### Leading Whitespace in Values

Leading/trailing whitespace in string values requires quoting:

```python
{"text": " value "}
```

```toon
text: " value "
```

---

## Order Preservation

**Object key order** and **array element order** are **always preserved** during encoding and decoding.

```python
from collections import OrderedDict

data = OrderedDict([("z", 1), ("a", 2), ("m", 3)])
toon = encode(data)
```

```toon
z: 1
a: 2
m: 3
```

Decoding preserves order:

```python
decoded = decode(toon)
list(decoded.keys())  # ['z', 'a', 'm']
```

---

## Complete Examples

### Simple Configuration

```python
{
    "app": "myapp",
    "version": "1.0.0",
    "debug": False,
    "port": 8080
}
```

```toon
app: myapp
version: "1.0.0"
debug: false
port: 8080
```

### Nested Structure with Arrays

```python
{
    "metadata": {
        "version": 2,
        "author": "Alice"
    },
    "items": [
        {"id": 1, "name": "Item1", "qty": 10},
        {"id": 2, "name": "Item2", "qty": 5}
    ],
    "tags": ["alpha", "beta", "gamma"]
}
```

```toon
metadata:
  version: 2
  author: Alice
items[2,]{id,name,qty}:
  1,Item1,10
  2,Item2,5
tags[3]: alpha,beta,gamma
```

### Mixed Array Types

```json
{
    "data": [
        {"type": "user", "id": 1},
        {"type": "user", "id": 2, "extra": "field"},  # Non-uniform
        42,
        "hello"
    ]
}
```

```toon
data[4]:
  - type: user
    id: 1
  - type: user
    id: 2
    extra: field
  - 42
  - hello
```

---

## Token Efficiency Comparison

**JSON (177 chars):**

```json
{"users":[{"id":1,"name":"Alice","age":30,"active":true},{"id":2,"name":"Bob","age":25,"active":true},{"id":3,"name":"Charlie","age":35,"active":false}]}
```

**TOON (85 chars, 52% reduction):**

```toon
users[3,]{id,name,age,active}:
  1,Alice,30,true
  2,Bob,25,true
  3,Charlie,35,false
```

---

## See Also

- [API Reference](https://javadoc.io/doc/dev.toonformat/jtoon) - Complete function documentation
- [Official Specification](https://github.com/toon-format/spec/blob/main/SPEC.md) - Normative spec
