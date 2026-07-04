/**
 * Decoding engine for converting TOON format to Java objects.
 *
 * <h2>Overview</h2>
 * <p>
 * This package contains the core decoding logic that parses TOON
 * (Token-Oriented Object Notation) format strings into Java objects
 * (Maps, Lists, and primitives). The architecture follows a line-by-line
 * parsing strategy with depth tracking based on indentation.
 * </p>
 *
 * <h2>Core Components</h2>
 *
 * <h3>ValueDecoder</h3>
 * <p>
 * The main orchestrator that manages the parsing state and routes lines to
 * appropriate decoders. Contains an inner {@code Parser} class that maintains:
 * </p>
 * <ul>
 *   <li>Current line index</li>
 *   <li>Array of input lines</li>
 *   <li>Indentation depth tracking</li>
 *   <li>Delimiter configuration</li>
 * </ul>
 *
 * <h3>PrimitiveDecoder</h3>
 * <p>
 * Handles parsing of scalar values with type inference:
 * </p>
 * <ul>
 *   <li>{@code "null"} → null</li>
 *   <li>{@code "true"/"false"} → Boolean</li>
 *   <li>Numeric strings → Long or Double</li>
 *   <li>Quoted strings → String (with unescaping)</li>
 *   <li>Bare strings → String</li>
 * </ul>
 *
 * <h3>ObjectDecoder</h3>
 * <p>
 * Parses key-value pairs and nested objects:
 * </p>
 * <ul>
 *   <li>Detects unquoted colons in {@code key: value} format</li>
 *   <li>Handles quoted keys like {@code "order:id": value}</li>
 *   <li>Uses lookahead to detect nested objects (depth increase)</li>
 *   <li>Recursively processes nested structures</li>
 * </ul>
 *
 * <h3>ArrayDecoder</h3>
 * <p>
 * Detects array type from header and delegates parsing:
 * </p>
 * <ul>
 *   <li><strong>Tabular:</strong> {@code items[2]{id,name}:} → parses rows into Maps</li>
 *   <li><strong>List:</strong> {@code items[2]:} with {@code - } prefixed lines</li>
 *   <li><strong>Primitive:</strong> {@code tags[3]: a,b,c} → inline or multiline</li>
 * </ul>
 *
 * <h2>Parsing Strategy</h2>
 *
 * <h3>Pattern Matching</h3>
 * <p>
 * Uses regex patterns to detect structure:
 * </p>
 * <ul>
 *   <li>{@code \[(#?)\d+[\t|]?]} - Standalone array header</li>
 *   <li>{@code \[(#?)\d+[\t|]?]\{(.+)\}:} - Tabular array header with fields</li>
 *   <li>{@code ^(.+?)\[(#?)\d+[\t|]?](\{[^}]+\})?:.*$} - Keyed array pattern</li>
 * </ul>
 *
 * <h3>Depth Tracking</h3>
 * <p>
 * Indentation determines nesting level:
 * </p>
 * <pre>{@code
 * user:              // depth 0
 *   id: 123          // depth 1
 *   contact:         // depth 1
 *     email: a@b.c   // depth 2
 * }</pre>
 *
 * <h3>Delimiter Awareness</h3>
 * <p>
 * Delimiter is detected from array headers or configured via DecodeOptions:
 * </p>
 * <ul>
 *   <li>{@code [2]} → comma (implicit)</li>
 *   <li>{@code [2 ]} → tab (space in header)</li>
 *   <li>{@code [2|]} → pipe (explicit)</li>
 * </ul>
 *
 * <h2>Decoding Process</h2>
 *
 * <ol>
 *   <li><strong>Entry:</strong> ValueDecoder receives TOON string and DecodeOptions</li>
 *   <li><strong>Line Splitting:</strong> Input split into array of lines</li>
 *   <li><strong>Pattern Detection:</strong> Each line analyzed for array headers, key-value pairs</li>
 *   <li><strong>Depth Calculation:</strong> Leading spaces determine nesting level</li>
 *   <li><strong>Delegation:</strong> Route to ObjectDecoder, ArrayDecoder, or PrimitiveDecoder</li>
 *   <li><strong>Recursive Parsing:</strong> Nested structures processed recursively with depth tracking</li>
 *   <li><strong>Object Assembly:</strong> Maps and Lists built from parsed components</li>
 * </ol>
 *
 * <h2>Array Format Detection</h2>
 *
 * <h3>Tabular Arrays</h3>
 * <p>
 * Header with field specification:
 * </p>
 * <pre>{@code
 * users[2]{id,name,role}:
 *   1,Alice,admin
 *   2,Bob,user
 * }</pre>
 *
 * <h3>List Arrays</h3>
 * <p>
 * Next line after header starts with {@code "- "}:
 * </p>
 * <pre>{@code
 * items[2]:
 *   - id: 1
 *     name: First
 *   - id: 2
 *     name: Second
 * }</pre>
 *
 * <h3>Primitive Arrays</h3>
 * <p>
 * Inline or multiline values without field spec or list markers:
 * </p>
 * <pre>{@code
 * tags[3]: reading,gaming,coding
 *
 * // or multiline:
 * tags[3]:
 *   reading,gaming,coding
 * }</pre>
 *
 * <h2>Error Handling</h2>
 *
 * <h3>Strict Mode (default)</h3>
 * <ul>
 *   <li>Throws IllegalArgumentException on malformed input</li>
 *   <li>Validates indentation consistency</li>
 *   <li>Requires valid array headers</li>
 * </ul>
 *
 * <h3>Lenient Mode</h3>
 * <ul>
 *   <li>Best-effort parsing</li>
 *   <li>Returns null on invalid input</li>
 *   <li>Skips malformed lines</li>
 * </ul>
 *
 * <h2>Special Parsing Cases</h2>
 *
 * <h3>Quoted Keys</h3>
 * <p>
 * {@code findUnquotedColon()} correctly handles keys with colons:
 * </p>
 * <pre>{@code
 * "order:id": 7       // Colon inside quotes is literal
 * name: Ada           // Unquoted colon separates key/value
 * }</pre>
 *
 * <h3>Escaped Strings</h3>
 * <p>
 * Delegates to StringEscaper.unescape() for:
 * </p>
 * <ul>
 *   <li>{@code \n} → newline</li>
 *   <li>{@code \t} → tab</li>
 *   <li>{@code \"} → quote</li>
 *   <li>{@code \\} → backslash</li>
 * </ul>
 *
 * <h3>Delimiter Parsing</h3>
 * <p>
 * Respects quotes when splitting values:
 * </p>
 * <pre>{@code
 * // Input: a,"b,c",d
 * // Splits to: ["a", "b,c", "d"]  (comma inside quotes preserved)
 * }</pre>
 *
 * <h2>Architecture Benefits</h2>
 *
 * <h3>Single Responsibility</h3>
 * <p>
 * Each decoder has one clear responsibility:
 * </p>
 * <ul>
 *   <li><strong>ValueDecoder:</strong> Parse state management and routing</li>
 *   <li><strong>PrimitiveDecoder:</strong> Type inference for scalars</li>
 *   <li><strong>ObjectDecoder:</strong> Key-value parsing with nesting</li>
 *   <li><strong>ArrayDecoder:</strong> Array type detection and delegation</li>
 * </ul>
 *
 * <h3>Maintainability</h3>
 * <ul>
 *   <li>Clear separation between array types</li>
 *   <li>Recursive descent mirrors TOON's indentation structure</li>
 *   <li>Regex patterns document expected formats</li>
 *   <li>Testable in isolation</li>
 * </ul>
 *
 * @since 0.2.0
 * @see dev.toonformat.jtoon.DecodeOptions
 * @see dev.toonformat.jtoon.JToon#decode(String)
 * @see dev.toonformat.jtoon.JToon#decodeToJson(String)
 */
@NullMarked
package dev.toonformat.jtoon.decoder;

import org.jspecify.annotations.NullMarked;
