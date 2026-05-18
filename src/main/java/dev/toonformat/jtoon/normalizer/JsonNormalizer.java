package dev.toonformat.jtoon.normalizer;

import dev.toonformat.jtoon.util.ObjectMapperSingleton;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ShortNode;
import tools.jackson.databind.node.StringNode;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Normalizes Java objects to Jackson JsonNode representation.
 * Handles Java-specific types like LocalDateTime, Optional, Stream, etc.
 */
public final class JsonNormalizer {

    /**
     * Shared ObjectMapper instance configured for JSON normalization.
     */
    public static final ObjectMapper MAPPER = ObjectMapperSingleton.getInstance();

    /**
     * maximal allowed nesting depth of list.
     */
    public static final int MAX_ALLOWED_NESTING_DEPTH = 256;

    private static final ThreadLocal<Integer> DEPTH_COUNTER = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Map<Object, Boolean>> VISITED =
        ThreadLocal.withInitial(IdentityHashMap::new);

    private static final List<Function<Object, JsonNode>> NORMALIZERS = List.of(
        JsonNormalizer::tryNormalizePrimitive,
        JsonNormalizer::tryNormalizeBigNumber,
        JsonNormalizer::tryNormalizeTemporal,
        JsonNormalizer::tryNormalizeCollection,
        JsonNormalizer::tryNormalizePojo);

    private JsonNormalizer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }


    /**
     * Parses a JSON string into a JsonNode using the shared ObjectMapper.
     * <p>
     * This centralizes JSON parsing concerns to keep the public API thin and
     * maintain separation of responsibilities between parsing, normalization,
     * and encoding.
     * </p>
     *
     * @param json The JSON string to parse (must be non-blank)
     * @return Parsed JsonNode
     * @throws IllegalArgumentException if the input is blank or not valid JSON
     */
    public static JsonNode parse(final String json) {
        if (json == null) {
            throw new IllegalArgumentException("JSON string cannot be null");
        }
        if (json.isBlank()) {
            throw new IllegalArgumentException("JSON string cannot be blank");
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Normalizes any Java object to a JsonNode.
     *
     * @param value The value to normalize
     * @return The normalized JsonNode
     * @throws IllegalArgumentException if nesting depth exceeds MAX_DEPTH or circular reference detected
     */
    public static JsonNode normalize(final Object value) {
        final int currentDepth = DEPTH_COUNTER.get();
        if (currentDepth > MAX_ALLOWED_NESTING_DEPTH) {
            DEPTH_COUNTER.remove();
            throw new IllegalArgumentException("Maximum nesting depth exceeded: " + MAX_ALLOWED_NESTING_DEPTH);
        }
        DEPTH_COUNTER.set(currentDepth + 1);
        try {
            return normalizeInternal(value);
        } finally {
            DEPTH_COUNTER.set(currentDepth);
        }
    }

    private static JsonNode normalizeInternal(final Object value) {
        if (value == null) {
            return NullNode.getInstance();
        } else if (value instanceof JsonNode jsonNode) {
            return jsonNode;
        }
        final Map<Object, Boolean> visited = VISITED.get();
        if (visited.containsKey(value)) {
            throw new IllegalArgumentException("Circular reference detected");
        }
        visited.put(value, Boolean.TRUE);
        try {
            if (value instanceof Optional<?>) {
                return normalize(((Optional<?>) value).orElse(null));
            } else if (value instanceof Stream<?>) {
                return normalize(((Stream<?>) value).toList());
            } else if (value.getClass().isArray()) {
                return normalizeArray(value);
            } else {
                return normalizeWithStrategy(value);
            }
        } finally {
            visited.remove(value);
        }
    }

    /**
     * Attempts normalization using chain of responsibility pattern.
     */
    private static JsonNode normalizeWithStrategy(final Object value) {
        return NORMALIZERS.stream()
            .map(normalizer -> normalizer.apply(value))
            .filter(Objects::nonNull)
            .findFirst()
            .orElseGet(NullNode::getInstance);
    }

    /**
     * Attempts to normalize primitive types and their wrappers.
     * Returns null if the value is not a primitive type.
     */
    private static JsonNode tryNormalizePrimitive(final Object value) {
        if (value instanceof String stringValue) {
            return StringNode.valueOf(stringValue);
        } else if (value instanceof Boolean boolValue) {
            return BooleanNode.valueOf(boolValue);
        } else if (value instanceof Integer intValue) {
            return IntNode.valueOf(intValue);
        } else if (value instanceof Long longValue) {
            return LongNode.valueOf(longValue);
        } else if (value instanceof Double doubleValue) {
            return normalizeDouble(doubleValue);
        } else if (value instanceof Float floatValue) {
            return normalizeFloat(floatValue);
        } else if (value instanceof Short shortValue) {
            return ShortNode.valueOf(shortValue);
        } else if (value instanceof Byte byteValue) {
            return IntNode.valueOf(byteValue);
        } else {
            return null;
        }
    }

    /**
     * Normalizes Double values handling special cases.
     */
    private static JsonNode normalizeDouble(final Double value) {
        if (!Double.isFinite(value)) {
            return NullNode.getInstance();
        }
        if (value == 0.0) {
            return IntNode.valueOf(0);
        }
        return tryConvertToLong(value)
            .orElseGet(() -> DoubleNode.valueOf(value));
    }

    /**
     * Normalizes Float values handling special cases.
     */
    private static JsonNode normalizeFloat(final Float value) {
        return Float.isFinite(value)
            ? FloatNode.valueOf(value)
            : NullNode.getInstance();
    }

    /**
     * Attempts to convert a double to a long if it's a whole number.
     */
    private static Optional<JsonNode> tryConvertToLong(final Double value) {
        if (value != Math.floor(value)) {
            return Optional.empty();
        }
        if (value > Long.MAX_VALUE || value < Long.MIN_VALUE) {
            return Optional.empty();
        }
        final long longVal = value.longValue();
        return Optional.of(LongNode.valueOf(longVal));
    }

    /**
     * Attempts to normalize BigInteger and BigDecimal.
     * Returns null if the value is not a big number type.
     */
    private static JsonNode tryNormalizeBigNumber(final Object value) {
        if (value instanceof BigInteger bigInteger) {
            return normalizeBigInteger(bigInteger);
        } else if (value instanceof BigDecimal bigDecimal) {
            return DecimalNode.valueOf(bigDecimal);
        } else {
            return null;
        }
    }

    /**
     * Normalizes BigInteger, converting to long if within range.
     */
    private static JsonNode normalizeBigInteger(final BigInteger value) {
        final boolean fitsInLong = value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0
            && value.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0;
        return fitsInLong
            ? LongNode.valueOf(value.longValue())
            : StringNode.valueOf(value.toString());
    }

    /**
     * Attempts to normalize temporal types (date/time) to ISO strings.
     * Returns null if the value is not a temporal type.
     */
    private static JsonNode tryNormalizeTemporal(final Object value) {
        if (value instanceof LocalDateTime ldt) {
            return formatTemporal(ldt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (value instanceof LocalDate ld) {
            return formatTemporal(ld, DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (value instanceof LocalTime lt) {
            return formatTemporal(lt, DateTimeFormatter.ISO_LOCAL_TIME);
        } else if (value instanceof ZonedDateTime zonedDateTime) {
            return formatTemporal(zonedDateTime, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        } else if (value instanceof OffsetDateTime offsetDateTime) {
            return formatTemporal(offsetDateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else if (value instanceof Calendar calendar) {
            return StringNode.valueOf(calendar.toInstant().toString());
        } else if (value instanceof Instant instant) {
            return StringNode.valueOf(instant.toString());
        } else if (value instanceof java.sql.Timestamp timestamp) {
            return formatTemporal(timestamp.toLocalDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (value instanceof java.sql.Date date) {
            return formatTemporal(date.toLocalDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (value instanceof java.sql.Time time) {
            return formatTemporal(time.toLocalTime(), DateTimeFormatter.ISO_LOCAL_TIME);
        } else if (value instanceof Date date) {
            return StringNode.valueOf(LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault()).toString());
        } else {
            return null;
        }
    }

    /**
     * Helper method to format temporal values consistently.
     */
    private static <T> JsonNode formatTemporal(final T temporal, final DateTimeFormatter formatter) {
        return StringNode.valueOf(formatter.format((java.time.temporal.TemporalAccessor) temporal));
    }

    /**
     * Attempts to normalize collections (Collection and Map).
     * Returns null if the value is not a collection type.
     */
    private static JsonNode tryNormalizeCollection(final Object value) {
        if (value instanceof Collection<?>) {
            return normalizeCollection((Collection<?>) value);
        } else if (value instanceof Map<?, ?>) {
            return normalizeMap((Map<?, ?>) value);
        } else {
            return null;
        }
    }

    /**
     * Normalizes a Collection to an ArrayNode.
     */
    private static ArrayNode normalizeCollection(final Collection<?> collection) {
        final ArrayNode arrayNode = MAPPER.createArrayNode();
        for (Object item : collection) {
            arrayNode.add(normalize(item));
        }
        return arrayNode;
    }

    /**
     * Normalizes a Map to an ObjectNode.
     */
    private static ObjectNode normalizeMap(final Map<?, ?> map) {
        final ObjectNode objectNode = MAPPER.createObjectNode();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            objectNode.set(String.valueOf(entry.getKey()), normalize(entry.getValue()));
        }
        return objectNode;
    }

    /**
     * Attempts to normalize POJOs using Jackson's default conversion.
     * Returns null for non-serializable objects.
     */
    private static JsonNode tryNormalizePojo(final Object value) {
        try {
            return MAPPER.valueToTree(value);
        } catch (Exception e) {
            return NullNode.getInstance();
        }
    }

    /**
     * Normalizes primitive arrays to ArrayNode without auto-boxing overhead.
     * Uses direct array population to avoid IntFunction lambda allocations.
     */
    private static JsonNode normalizeArray(final Object array) {
        if (array instanceof int[] intArray) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (int i : intArray) {
                node.add(IntNode.valueOf(i));
            }
            return node;
        } else if (array instanceof long[] longArray) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (long l : longArray) {
                node.add(LongNode.valueOf(l));
            }
            return node;
        } else if (array instanceof double[] doubleArray) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (final double d : doubleArray) {
                node.add(Double.isFinite(d) ? DoubleNode.valueOf(d) : NullNode.getInstance());
            }
            return node;
        } else if (array instanceof float[] floatArray) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (final float f : floatArray) {
                node.add(Float.isFinite(f) ? FloatNode.valueOf(f) : NullNode.getInstance());
            }
            return node;
        } else if (array instanceof boolean[] boolArray) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (boolean b : boolArray) {
                node.add(BooleanNode.valueOf(b));
            }
            return node;
        } else if (array instanceof byte[] byteArray) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (byte by : byteArray) {
                node.add(IntNode.valueOf(by));
            }
            return node;
        } else if (array instanceof short[] shortArray) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (short s : shortArray) {
                node.add(ShortNode.valueOf(s));
            }
            return node;
        } else if (array instanceof char[] charArray) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (char c : charArray) {
                node.add(StringNode.valueOf(String.valueOf(c)));
            }
            return node;
        } else if (array instanceof Object[] objArray) {
            final ArrayNode node = MAPPER.createArrayNode();
            for (Object o : objArray) {
                node.add(normalize(o));
            }
            return node;
        } else {
            return MAPPER.createArrayNode();
        }
    }
}
