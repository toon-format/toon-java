/**
 * Java object normalization to Jackson JsonNode representation.
 * 
 * <h2>Overview</h2>
 * <p>
 * This package handles the conversion of Java objects into a normalized JsonNode
 * representation that can be efficiently encoded to TOON format. It bridges the gap
 * between Java's rich type system and the simpler JSON-like structure.
 * </p>
 * 
 * <h2>Core Component</h2>
 * 
 * <h3>JsonNormalizer</h3>
 * <p>
 * The main normalization engine that converts any Java object to JsonNode.
 * Uses a chain of responsibility pattern to handle different type categories:
 * </p>
 * <ol>
 *   <li><strong>Primitives:</strong> String, Boolean, Number types</li>
 *   <li><strong>Big Numbers:</strong> BigInteger, BigDecimal</li>
 *   <li><strong>Temporal Types:</strong> LocalDateTime, Instant, etc.</li>
 *   <li><strong>Collections:</strong> Collection, Map, arrays</li>
 *   <li><strong>POJOs:</strong> Arbitrary objects via Jackson</li>
 * </ol>
 * 
 * <h2>Type Conversions</h2>
 * 
 * <h3>Number Normalization</h3>
 * <ul>
 *   <li>Non-finite values (NaN, ±Infinity) → null</li>
 *   <li>-0.0 → 0</li>
 *   <li>Whole doubles → converted to long when possible</li>
 *   <li>BigInteger → long if within range, otherwise string</li>
 * </ul>
 * 
 * <h3>Temporal Types</h3>
 * <p>
 * All temporal types are converted to ISO-8601 formatted strings:
 * </p>
 * <ul>
 *   <li>LocalDateTime → "2025-01-15T10:30:00"</li>
 *   <li>LocalDate → "2025-01-15"</li>
 *   <li>LocalTime → "10:30:00"</li>
 *   <li>ZonedDateTime → "2025-01-15T10:30:00+01:00[Europe/Paris]"</li>
 *   <li>OffsetDateTime → "2025-01-15T10:30:00+01:00"</li>
 *   <li>Instant → "2025-01-15T09:30:00Z"</li>
 *   <li>java.util.Date → converted to Instant then formatted</li>
 * </ul>
 * 
 * <h3>Special Java Types</h3>
 * <ul>
 *   <li><strong>Optional:</strong> Unwrapped to value or null</li>
 *   <li><strong>Stream:</strong> Materialized to List then normalized</li>
 *   <li><strong>Collection:</strong> Converted to ArrayNode</li>
 *   <li><strong>Map:</strong> Converted to ObjectNode with string keys</li>
 *   <li><strong>Arrays:</strong> All primitive and object arrays handled</li>
 * </ul>
 * 
 * <h2>Design Principles</h2>
 * 
 * <h3>LLM-Safe Output</h3>
 * <p>
 * All normalizations produce values that are:
 * </p>
 * <ul>
 *   <li>Deterministic (same input always produces same output)</li>
 *   <li>Unambiguous (no special number representations)</li>
 *   <li>Standard (ISO formats for dates, decimal for numbers)</li>
 * </ul>
 * 
 * <h3>Graceful Degradation</h3>
 * <p>
 * Non-serializable objects are converted to null rather than throwing exceptions.
 * This ensures the encoder can always produce output even with problematic data.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * JsonNormalizer normalizer = new JsonNormalizer();
 * 
 * // Normalize a POJO
 * record User(int id, String name, LocalDateTime created) {}
 * User user = new User(123, "Ada", LocalDateTime.now());
 * JsonNode normalized = JsonNormalizer.normalize(user);
 * 
 * // Results in:
 * // {
 * //   "id": 123,
 * //   "name": "Ada",
 * //   "created": "2025-01-15T10:30:00"
 * // }
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * The JsonNormalizer uses a shared ObjectMapper instance which is thread-safe.
 * All normalization methods are stateless and safe to call concurrently.
 * </p>
 * 
 * @since 0.1.0
 * @see tools.jackson.databind.JsonNode
 * @see tools.jackson.databind.ObjectMapper
 */
@NullMarked
package dev.toonformat.jtoon.normalizer;

import org.jspecify.annotations.NullMarked;

