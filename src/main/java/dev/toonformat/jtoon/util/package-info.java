/**
 * Utility classes for string validation, escaping, and shared constants.
 * 
 * <h2>Overview</h2>
 * <p>
 * This package provides low-level utility functions used throughout the JToon library.
 * These classes handle string processing, validation rules, and shared constants.
 * </p>
 * 
 * <h2>Core Components</h2>
 * 
 * <h3>StringValidator</h3>
 * <p>
 * Validates whether strings and keys can be used unquoted in TOON format.
 * Implements TOON's smart quoting rules to minimize token usage while avoiding ambiguity.
 * </p>
 * 
 * <p><strong>String Validation Rules:</strong></p>
 * <p>Strings must be quoted if they:</p>
 * <ul>
 *   <li>Are empty</li>
 *   <li>Have leading or trailing whitespace</li>
 *   <li>Look like keywords (true, false, null)</li>
 *   <li>Look like numbers (123, 3.14, 1e-6)</li>
 *   <li>Contain special characters (colon, quotes, backslash)</li>
 *   <li>Contain structural characters (brackets, braces)</li>
 *   <li>Contain control characters (newline, tab, etc.)</li>
 *   <li>Contain the active delimiter</li>
 *   <li>Start with "- " (list marker)</li>
 * </ul>
 * 
 * <p><strong>Key Validation Rules:</strong></p>
 * <p>Object keys can be unquoted if they match:</p>
 * <pre>{@code ^[A-Z_][\\w.]*$}</pre>
 * <p>(Start with letter or underscore, contain only word characters and dots)</p>
 * 
 * <h3>StringEscaper</h3>
 * <p>
 * Handles character escaping for quoted strings. Escapes special characters
 * that would otherwise cause parsing issues:
 * </p>
 * <ul>
 *   <li>{@code \} → {@code \\}</li>
 *   <li>{@code "} → {@code \"}</li>
 *   <li>Newline → {@code \n}</li>
 *   <li>Carriage return → {@code \r}</li>
 *   <li>Tab → {@code \t}</li>
 * </ul>
 * 
 * <h3>Constants</h3>
 * <p>
 * Shared string constants used throughout the encoding process:
 * </p>
 * <ul>
 *   <li><strong>Structural:</strong> COLON, COMMA, SPACE, brackets, braces</li>
 *   <li><strong>Literals:</strong> NULL_LITERAL, TRUE_LITERAL, FALSE_LITERAL</li>
 *   <li><strong>List markers:</strong> LIST_ITEM_MARKER, LIST_ITEM_PREFIX</li>
 *   <li><strong>Escape characters:</strong> BACKSLASH, DOUBLE_QUOTE</li>
 * </ul>
 * 
 * <h2>Delimiter-Aware Validation</h2>
 * <p>
 * StringValidator implements delimiter-aware validation: strings containing the
 * active delimiter must be quoted, while other delimiters are safe. This allows
 * the same data to be encoded with different delimiters without unnecessary quoting.
 * </p>
 * 
 * <h3>Example:</h3>
 * <pre>{@code
 * // With comma delimiter
 * StringValidator.isSafeUnquoted("a,b", ",")  → false (must quote)
 * StringValidator.isSafeUnquoted("a|b", ",")  → true  (pipe is safe)
 * 
 * // With pipe delimiter
 * StringValidator.isSafeUnquoted("a,b", "|")  → true  (comma is safe)
 * StringValidator.isSafeUnquoted("a|b", "|")  → false (must quote)
 * }</pre>
 * 
 * <h2>Design Principles</h2>
 * 
 * <h3>Utility Class Pattern</h3>
 * <p>
 * All classes in this package follow the utility class pattern:
 * </p>
 * <ul>
 *   <li>Final classes (cannot be extended)</li>
 *   <li>Private constructors (cannot be instantiated)</li>
 *   <li>Static methods only (no instance state)</li>
 * </ul>
 * 
 * <h3>Performance Optimization</h3>
 * <ul>
 *   <li><strong>Precompiled Patterns:</strong> Regex patterns compiled once at class load</li>
 *   <li><strong>Guard Clauses:</strong> Fast-fail checks before expensive operations</li>
 *   <li><strong>No Allocation:</strong> Methods operate on existing strings without copying</li>
 * </ul>
 * 
 * <h3>Thread Safety</h3>
 * <p>
 * All utility methods are stateless and thread-safe. Precompiled Pattern instances
 * are immutable and safe to share across threads.
 * </p>
 * 
 * <h2>Testing</h2>
 * <p>
 * These utilities have comprehensive test coverage including:
 * </p>
 * <ul>
 *   <li>StringEscaperTest: 62 tests covering all escape scenarios</li>
 *   <li>StringValidatorTest: 48 tests covering all validation rules</li>
 * </ul>
 * 
 * @since 0.1.0
 */
@NullMarked
package dev.toonformat.jtoon.util;

import org.jspecify.annotations.NullMarked;

