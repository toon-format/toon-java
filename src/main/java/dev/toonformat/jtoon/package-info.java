/**
 * JToon - Token-Oriented Object Notation encoder for Java.
 * 
 * <h2>Overview</h2>
 * <p>
 * This package provides a Java implementation of the TOON (Token-Oriented
 * Object Notation) format,
 * a compact, human-readable data format optimized for Large Language Model
 * (LLM) contexts.
 * TOON achieves 30-60% token reduction compared to JSON while maintaining
 * readability and structure.
 * </p>
 * 
 * <h2>Core Components</h2>
 * 
 * <h3>Public API</h3>
 * <ul>
 * <li>{@link dev.toonformat.jtoon.JToon} - Main entry point for encoding
 * Java objects to TOON format</li>
 * <li>{@link dev.toonformat.jtoon.EncodeOptions} - Configuration options
 * for encoding (indent, delimiter, length marker)</li>
 * <li>{@link dev.toonformat.jtoon.Delimiter} - Enum for array/tabular
 * delimiter options (comma, tab, pipe)</li>
 * </ul>
 * 
 * <h3>Encoding Pipeline</h3>
 * <ul>
 * <li>{@link dev.toonformat.jtoon.normalizer.JsonNormalizer} - Converts
 * Java objects to Jackson JsonNode representation</li>
 * <li>{@link dev.toonformat.jtoon.encoder.ValueEncoder} - Core encoder that
 * converts JsonNode to TOON format</li>
 * <li>{@link dev.toonformat.jtoon.encoder.PrimitiveEncoder} - Handles
 * encoding of primitive values and object keys</li>
 * <li>{@link dev.toonformat.jtoon.encoder.HeaderFormatter} - Formats array
 * and tabular structure headers</li>
 * <li>{@link dev.toonformat.jtoon.encoder.LineWriter} - Accumulates
 * indented lines for output</li>
 * </ul>
 * 
 * <h3>Utility Classes</h3>
 * <ul>
 * <li>{@link dev.toonformat.jtoon.util.StringValidator} - Validates when
 * strings can be unquoted</li>
 * <li>{@link dev.toonformat.jtoon.util.StringEscaper} - Escapes special
 * characters in quoted strings</li>
 * <li>{@link dev.toonformat.jtoon.util.Constants} - Shared constants used
 * throughout the package</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Basic Encoding</h3>
 * 
 * <pre>{@code
 * import dev.toonformat.jtoon.JToon;
 * import java.util.*;
 * 
 * record User(int id, String name, boolean active) {}
 * 
 * User user = new User(123, "Ada", true);
 * String jtoon = JToon.encode(user);
 * // Output:
 * // id: 123
 * // name: Ada
 * // active: true
 * }</pre>
 * 
 * <h3>Tabular Arrays</h3>
 * 
 * <pre>{@code
 * record Item(String sku, int qty, double price) {
 * }
 * 
 * record Order(List<Item> items) {
 * }
 * 
 * Order order = new Order(List.of(
 *         new Item("A1", 2, 9.99),
 *         new Item("B2", 1, 14.5)));
 * 
 * String jtoon = JToon.encode(order);
 * // Output:
 * // items[2]{sku,qty,price}:
 * // A1,2,9.99
 * // B2,1,14.5
 * }</pre>
 * 
 * <h3>Custom Options</h3>
 * 
 * <pre>{@code
 * import dev.toonformat.jtoon.*;
 * 
 * // Use tab delimiters and length markers
 * EncodeOptions options = new EncodeOptions(2, Delimiter.TAB, true);
 * String jtoon = JToon.encode(data, options);
 * 
 * // Or use builder-style methods
 * EncodeOptions opts1 = EncodeOptions.withIndent(4);
 * EncodeOptions opts2 = EncodeOptions.withDelimiter(Delimiter.PIPE);
 * EncodeOptions opts3 = EncodeOptions.withLengthMarker(true);
 * }</pre>
 * 
 * <h2>Type Conversions</h2>
 * <p>
 * The library automatically normalizes Java-specific types for LLM-safe output:
 * </p>
 * <ul>
 * <li>Numbers: Finite values in decimal form; NaN/Infinity → null; -0 → 0</li>
 * <li>BigInteger: Converted to Long if within range, otherwise string</li>
 * <li>BigDecimal: Preserved as decimal number</li>
 * <li>Temporal types: Converted to ISO-8601 strings (LocalDateTime, Instant,
 * etc.)</li>
 * <li>Optional: Unwrapped to value or null</li>
 * <li>Stream: Materialized to array</li>
 * <li>Collections: Converted to arrays</li>
 * <li>Maps: Converted to objects with string keys</li>
 * </ul>
 * 
 * <h2>Format Features</h2>
 * 
 * <h3>Indentation-Based Structure</h3>
 * <p>
 * Uses YAML-like indentation (default 2 spaces) for nested objects.
 * </p>
 * 
 * <h3>Tabular Arrays</h3>
 * <p>
 * Arrays of uniform objects with primitive values are encoded in CSV-like
 * tabular format,
 * declaring field names once in the header and then streaming rows.
 * </p>
 * 
 * <h3>Smart Quoting</h3>
 * <p>
 * Strings are quoted only when necessary (contains delimiters, special
 * characters,
 * looks like keywords/numbers, etc.). This minimizes token usage.
 * </p>
 * 
 * <h3>Delimiter Options</h3>
 * <p>
 * Arrays and tabular rows support three delimiters:
 * </p>
 * <ul>
 * <li>Comma (default): Implicit in headers - {@code items[3]: a,b,c}</li>
 * <li>Pipe: Explicit in headers - {@code items[3|]: a|b|c}</li>
 * <li>Tab: Explicit in headers - {@code items[3\t]: a\tb\tc}</li>
 * </ul>
 * 
 * <h3>Length Markers</h3>
 * <p>
 * Optional {@code #} prefix for array lengths to emphasize count vs index:
 * {@code items[#3]} instead of {@code items[3]}.
 * </p>
 * 
 * <h2>Architecture</h2>
 * 
 * <h3>Encoding Pipeline</h3>
 * <ol>
 * <li><strong>Normalization</strong>: {@code JsonNormalizer} converts Java
 * objects to JsonNode</li>
 * <li><strong>Encoding</strong>: {@code ValueEncoder} recursively encodes
 * JsonNode to TOON</li>
 * <li><strong>Output</strong>: {@code LineWriter} accumulates formatted
 * lines</li>
 * </ol>
 * 
 * <h3>Design Principles</h3>
 * <ul>
 * <li><strong>Single Responsibility</strong>: Each class has one clear
 * purpose</li>
 * <li><strong>Immutability</strong>: Configuration objects are immutable
 * records</li>
 * <li><strong>Utility Classes</strong>: Static utility classes with private
 * constructors</li>
 * <li><strong>Modern Java</strong>: Leverages Java 17 features (records, switch
 * expressions)</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 * <li>Tabular format detection is O(n×m) where n = rows, m = fields</li>
 * <li>String validation uses precompiled regex patterns</li>
 * <li>StringBuilder for efficient string concatenation</li>
 * <li>No reflection in hot paths (relies on Jackson's object mapper)</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * All public API methods are thread-safe. Internal classes are stateless
 * utility classes
 * or immutable configuration objects. The Jackson ObjectMapper instance is
 * shared and thread-safe.
 * </p>
 * 
 * <h2>See Also</h2>
 * <ul>
 * <li><a href="https://github.com/toon-format/toon-java">JToon GitHub
 * Repository</a></li>
 * <li><a href=
 * "https://github.com/toon-format/toon-java/blob/main/TOON-SPECIFICATION.md">
 * TOON Format Specification</a></li>
 * <li><a href=
 * "https://central.sonatype.com/artifact/dev.toonformat/jtoon">Maven
 * Central</a></li>
 * </ul>
 * 
 * @since 0.1.0
 * @version 0.1.0
 */
@NullMarked
package dev.toonformat.jtoon;

import org.jspecify.annotations.NullMarked;
