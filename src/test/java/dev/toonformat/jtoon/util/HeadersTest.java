package dev.toonformat.jtoon.util;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Headers}.
 */
@DisplayName("Headers")
class HeadersTest {

    private static final long TRIPLE_LENGTH = 3L;

    @Test
    @DisplayName("constructor throws UnsupportedOperationException")
    void constructorThrowsException() throws Exception {
        final Constructor<Headers> constructor = Headers.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
    }

    @Test
    @DisplayName("ARRAY_HEADER_PATTERN matches array headers")
    void arrayHeaderPatternMatches() {
        assertNotNull(Headers.ARRAY_HEADER_PATTERN.matcher("[3]").matches());
        assertNotNull(Headers.ARRAY_HEADER_PATTERN.matcher("[#2]").matches());
        assertNotNull(Headers.ARRAY_HEADER_PATTERN.matcher("[3\t]").matches());
        assertNotNull(Headers.ARRAY_HEADER_PATTERN.matcher("[2|]").matches());
    }

    @Test
    @DisplayName("TABULAR_HEADER_PATTERN matches tabular headers")
    void tabularHeaderPatternMatches() {
        assertNotNull(Headers.TABULAR_HEADER_PATTERN.matcher("[2]{id,name,role}:").matches());
        assertNotNull(Headers.TABULAR_HEADER_PATTERN.matcher("[#3]{a,b,c}:").matches());
    }

    @Test
    @DisplayName("KEYED_ARRAY_PATTERN matches keyed arrays")
    void keyedArrayPatternMatches() {
        assertNotNull(Headers.KEYED_ARRAY_PATTERN.matcher("items[2]{id,name}:").matches());
        assertNotNull(Headers.KEYED_ARRAY_PATTERN.matcher("tags[3]:").matches());
        assertNotNull(Headers.KEYED_ARRAY_PATTERN.matcher("data[4]{id}:").matches());
    }

    @Test
    @DisplayName("KEYED_ARRAY_PATTERN matches quoted keys with spaces")
    void keyedArrayPatternQuotedKeyWithSpaces() {
        assertNotNull(Headers.KEYED_ARRAY_PATTERN.matcher("\"my items\"[3]:").matches());
        assertNotNull(Headers.KEYED_ARRAY_PATTERN.matcher("\"user name\"[2]{id,name}:").matches());
    }

    @Test
    @DisplayName("KEYED_ARRAY_PATTERN matches quoted keys with escaped quotes")
    void keyedArrayPatternEscapedQuotes() {
        // Key containing escaped quotes: "name\"with\"quotes"
        assertTrue(Headers.KEYED_ARRAY_PATTERN.matcher("\"name\\\"with\\\"quotes\"[3]:").matches());
        assertTrue(Headers.KEYED_ARRAY_PATTERN.matcher("\"key\\\"word\"[2]{a,b}:").matches());
    }

    @Test
    @DisplayName("KEYED_ARRAY_PATTERN does not match malformed patterns")
    void keyedArrayPatternNoMatch() {
        // Missing colon
        assertFalse(Headers.KEYED_ARRAY_PATTERN.matcher("items[3]").matches());
        // Missing brackets
        assertFalse(Headers.KEYED_ARRAY_PATTERN.matcher("items:").matches());
        // Negative length
        assertFalse(Headers.KEYED_ARRAY_PATTERN.matcher("items[-1]:").matches());
    }

    @Test
    @DisplayName("matchKeyedArrayHeader scans key, bracket and field spec segments")
    void matchKeyedArrayHeader_givenFullHeader_thenSegments() {
        // Given / When
        final Headers.KeyedHeaderMatch match = Headers.matchKeyedArrayHeader("items[2]{id,name}:");

        // Then
        assertNotNull(match);
        assertEquals("items", match.key());
        assertEquals(2L, match.declaredLength());
        assertFalse(match.keyed());
        assertNull(match.delimiter());
        assertTrue(match.fieldsStart() > match.keyEnd());
        assertTrue(match.headerEnd() > match.fieldsStart());
    }

    @Test
    @DisplayName("matchKeyedArrayHeader scans keyed marker and delimiter declarations")
    void matchKeyedArrayHeader_givenKeyedMarkerAndDelimiter_thenDeclared() {
        // Given / When
        final Headers.KeyedHeaderMatch match = Headers.matchKeyedArrayHeader("items[2:|]:a,b");

        // Then
        assertNotNull(match);
        assertEquals(2L, match.declaredLength());
        assertTrue(match.keyed());
        assertEquals('|', match.delimiter().charValue());
    }

    @Test
    @DisplayName("matchKeyedArrayHeader scans hash marker and field spec")
    void matchKeyedArrayHeader_givenHashMarker_thenLengthDeclared() {
        // Given / When
        final Headers.KeyedHeaderMatch match = Headers.matchKeyedArrayHeader("items[#2]{a,b}:");

        // Then
        assertNotNull(match);
        assertEquals(2L, match.declaredLength());
        assertFalse(match.keyed());
        assertEquals("items", match.key());
    }

    @Test
    @DisplayName("matchKeyedArrayHeader scans quoted keys with spaces")
    void matchKeyedArrayHeader_givenQuotedKey_thenKey() {
        // Given / When
        final Headers.KeyedHeaderMatch match = Headers.matchKeyedArrayHeader("\"my items\"[3]:");

        // Then
        assertNotNull(match);
        assertEquals("\"my items\"", match.key());
        assertEquals(TRIPLE_LENGTH, match.declaredLength());
    }

    @Test
    @DisplayName("matchKeyedArrayHeader scans quoted keys with escaped quotes")
    void matchKeyedArrayHeader_givenEscapedQuoteKey_thenKey() {
        // Given / When
        final Headers.KeyedHeaderMatch match = Headers.matchKeyedArrayHeader("\"name\\\"with\\\"quotes\"[3]:");

        // Then
        assertNotNull(match);
        assertEquals("\"name\\\"with\\\"quotes\"", match.key());
    }

    @Test
    @DisplayName("matchKeyedArrayHeader scans field specs with nested braces")
    void matchKeyedArrayHeader_givenNestedFieldSpec_thenMatch() {
        // Given / When
        final Headers.KeyedHeaderMatch match = Headers.matchKeyedArrayHeader("geo[2]{point{lat,lon}}:");

        // Then
        assertNotNull(match);
        assertEquals("geo", match.key());
        assertTrue(match.fieldsStart() > match.keyEnd());
        assertTrue(match.headerEnd() > match.fieldsStart());
    }

    @Test
    @DisplayName("matchKeylessKeyedHeader scans plain keyless tabular header")
    void matchKeylessKeyedHeader_givenPlainHeader_thenNotKeyed() {
        // Given / When
        final Headers.KeyedHeaderMatch match = Headers.matchKeylessKeyedHeader("[2]{id,name}:");

        // Then
        assertNotNull(match);
        assertEquals("", match.key());
        assertFalse(match.keyed());
        assertTrue(match.fieldsStart() > 0);
    }

    @Test
    @DisplayName("matchKeylessKeyedHeader scans keyed keyless header")
    void matchKeylessKeyedHeader_givenKeyedMarker_thenKeyed() {
        // Given / When
        final Headers.KeyedHeaderMatch match = Headers.matchKeylessKeyedHeader("[2:]{id,name}:");

        // Then
        assertNotNull(match);
        assertTrue(match.keyed());
        assertTrue(match.fieldsStart() > 0);
    }

    @Test
    @DisplayName("matchKeyedArrayHeader rejects malformed headers")
    void matchKeyedArrayHeader_givenMalformed_thenNull() {
        // Missing trailing colon
        assertNull(Headers.matchKeyedArrayHeader("items[3]"));
        // Missing bracket segment
        assertNull(Headers.matchKeyedArrayHeader("items:"));
        // Empty bracket segment
        assertNull(Headers.matchKeyedArrayHeader("items[]:"));
        // Unterminated quoted key
        assertNull(Headers.matchKeyedArrayHeader("\"abc[3]:"));
        // Unbalanced field spec braces
        assertNull(Headers.matchKeyedArrayHeader("items[2]{a,b:"));
    }

    @Test
    @DisplayName("matchKeylessKeyedHeader rejects malformed headers")
    void matchKeylessKeyedHeader_givenMalformed_thenNull() {
        // Non-digit length
        assertNull(Headers.matchKeylessKeyedHeader("[x]:"));
        // Missing trailing colon
        assertNull(Headers.matchKeylessKeyedHeader("[2]{a,b}"));
    }
}
