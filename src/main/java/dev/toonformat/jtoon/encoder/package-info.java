/**
 * Encoding engine for converting normalized JsonNode values to TOON format.
 * 
 * <h2>Overview</h2>
 * <p>
 * This package contains the core encoding logic that transforms Jackson JsonNode
 * representations into TOON (Token-Oriented Object Notation) format strings.
 * The architecture follows a delegation pattern with specialized encoders for
 * different data types.
 * </p>
 * 
 * <h2>Core Components</h2>
 * 
 * <h3>ValueEncoder</h3>
 * <p>
 * The main orchestrator (42 lines) that determines node type and delegates to
 * specialized encoders:
 * </p>
 * <ul>
 *   <li>Primitives → PrimitiveEncoder</li>
 *   <li>Arrays → ArrayEncoder</li>
 *   <li>Objects → ObjectEncoder</li>
 * </ul>
 * 
 * <h3>ObjectEncoder</h3>
 * <p>
 * Handles encoding of JSON objects. Iterates through object fields and recursively
 * encodes key-value pairs. Delegates arrays to ArrayEncoder and primitives to
 * PrimitiveEncoder.
 * </p>
 * 
 * <h3>ArrayEncoder</h3>
 * <p>
 * Orchestrates array encoding by detecting array type and delegating appropriately:
 * </p>
 * <ul>
 *   <li>Primitive arrays → inline format ({@code tags[3]: a,b,c})</li>
 *   <li>Uniform objects → TabularArrayEncoder for efficient tabular format</li>
 *   <li>Mixed/nested → ListItemEncoder for list format</li>
 * </ul>
 * 
 * <h3>TabularArrayEncoder</h3>
 * <p>
 * Detects and encodes uniform arrays of objects in efficient tabular format.
 * Analyzes array structure to determine if all objects have the same primitive fields.
 * When possible, produces:
 * </p>
 * <pre>{@code
 * users[3]{id,name,age}:
 *   1,Alice,30
 *   2,Bob,25
 *   3,Charlie,35
 * }</pre>
 * 
 * <h3>ListItemEncoder</h3>
 * <p>
 * Handles encoding of objects as list items in non-uniform arrays. Implements
 * the complex logic for placing the first field on the {@code "- "} line and
 * indenting remaining fields.
 * </p>
 * 
 * <h3>PrimitiveEncoder</h3>
 * <p>
 * Handles encoding of primitive values (strings, numbers, booleans, null) and object keys.
 * Implements smart quoting logic: quotes strings only when necessary to avoid ambiguity.
 * </p>
 * 
 * <h3>HeaderFormatter</h3>
 * <p>
 * Formats array and tabular structure headers following TOON syntax:
 * </p>
 * <ul>
 *   <li>{@code items[5]:} - Simple array with 5 elements</li>
 *   <li>{@code users[3]{id,name,age}:} - Tabular array with 3 rows and specified fields</li>
 *   <li>{@code data[#10|]:} - Array with length marker and pipe delimiter</li>
 * </ul>
 * 
 * <h3>LineWriter</h3>
 * <p>
 * Accumulates formatted output lines with proper indentation.
 * Manages indentation depth and joins lines with newlines.
 * </p>
 * 
 * <h2>Encoding Process</h2>
 * 
 * <ol>
 *   <li><strong>Entry:</strong> ValueEncoder receives a JsonNode and EncodeOptions</li>
 *   <li><strong>Type Detection:</strong> ValueEncoder determines if node is primitive, array, or object</li>
 *   <li><strong>Delegation:</strong> Delegates to ObjectEncoder, ArrayEncoder, or PrimitiveEncoder</li>
 *   <li><strong>Array Optimization:</strong> ArrayEncoder detects if arrays can use tabular format</li>
 *   <li><strong>Specialized Encoding:</strong> TabularArrayEncoder or ListItemEncoder handle specifics</li>
 *   <li><strong>Recursive Processing:</strong> Each encoder recursively handles nested structures</li>
 *   <li><strong>Output Building:</strong> LineWriter accumulates formatted lines with proper indentation</li>
 * </ol>
 * 
 * <h2>Architecture Benefits</h2>
 * 
 * <h3>Single Responsibility</h3>
 * <p>
 * Each encoder has one clear responsibility:
 * </p>
 * <ul>
 *   <li><strong>ValueEncoder:</strong> Route to appropriate encoder (42 lines)</li>
 *   <li><strong>ObjectEncoder:</strong> Handle object key-value pairs (62 lines)</li>
 *   <li><strong>ArrayEncoder:</strong> Orchestrate array encoding (207 lines)</li>
 *   <li><strong>TabularArrayEncoder:</strong> Detect and encode tabular format (126 lines)</li>
 *   <li><strong>ListItemEncoder:</strong> Handle list item formatting (146 lines)</li>
 * </ul>
 * 
 * <h3>Maintainability</h3>
 * <p>
 * The refactored architecture improves maintainability:
 * </p>
 * <ul>
 *   <li>Each class under 210 lines (vs. original 401-line monolith)</li>
 *   <li>Clear separation of concerns</li>
 *   <li>Easier to test in isolation</li>
 *   <li>Easier to understand and modify</li>
 * </ul>
 * 
 * <h2>Tabular Format Optimization</h2>
 * <p>
 * Arrays of uniform objects with primitive values are encoded in CSV-like tabular format:
 * </p>
 * <pre>{@code
 * // Instead of:
 * items[2]:
 *   - id: 1
 *     name: Alice
 *   - id: 2
 *     name: Bob
 * 
 * // TOON produces:
 * items[2]{id,name}:
 *   1,Alice
 *   2,Bob
 * }</pre>
 * 
 * <h2>Delimiter Support</h2>
 * <p>
 * Supports three delimiters for arrays and tabular rows:
 * </p>
 * <ul>
 *   <li><strong>Comma:</strong> Default, implicit in headers</li>
 *   <li><strong>Tab:</strong> Explicit in headers, often better tokenization</li>
 *   <li><strong>Pipe:</strong> Explicit in headers, visual clarity</li>
 * </ul>
 * 
 * @since 0.1.0
 * @see dev.toonformat.jtoon.EncodeOptions
 */
@NullMarked
package dev.toonformat.jtoon.encoder;

import org.jspecify.annotations.NullMarked;

