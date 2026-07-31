package dev.toonformat.jtoon.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.util.Headers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class KeyedObjectDecoderTest {

    private static final long PORT_VALUE = 8080L;
    private static final int AFTER_ENTRIES_LINE_INDEX = 3;

    @Test
    @DisplayName("Given keyed tabular object When parsed Then entry-keyed map returned")
    @SuppressWarnings("unchecked")
    void parseKeyedTabularObject_givenEntries_whenParsed_thenMap() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"servers[2:]{host,port}:",
            "  alpha: a.example.com,8080",
            "  beta: b.example.com,9090"};
        context.currentLine = 0;
        final Headers.KeyedHeaderMatch header = Headers.matchKeyedArrayHeader(context.lines[0]);

        // When
        final Map<String, Object> result =
            KeyedObjectDecoder.parseKeyedTabularObject(context.lines[0], header, 1, context);

        // Then
        assertEquals(2, result.size());
        final Map<String, Object> alpha = (Map<String, Object>) result.get("alpha");
        assertEquals("a.example.com", alpha.get("host"));
        assertEquals(PORT_VALUE, alpha.get("port"));
        assertEquals(AFTER_ENTRIES_LINE_INDEX, context.currentLine);
    }

    @Test
    @DisplayName("Given header without field list When parsed Then exception")
    void parseKeyedTabularObject_givenMissingFieldsHeader_whenParsed_thenThrows() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"servers[2:]:"};
        context.currentLine = 0;
        final Headers.KeyedHeaderMatch header = Headers.matchKeyedArrayHeader(context.lines[0]);

        // When / Then
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> KeyedObjectDecoder.parseKeyedTabularObject(context.lines[0], header, 1, context));
        assertTrue(ex.getMessage().contains("requires a field list"));
    }

    @Test
    @DisplayName("Given inline content after keyed header When parsed Then exception")
    void parseKeyedTabularObject_givenInlineContent_whenParsed_thenThrows() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"servers[1:]{host}: x"};
        context.currentLine = 0;
        final Headers.KeyedHeaderMatch header = Headers.matchKeyedArrayHeader(context.lines[0]);

        // When / Then
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> KeyedObjectDecoder.parseKeyedTabularObject(context.lines[0], header, 1, context));
        assertTrue(ex.getMessage().contains("Inline content after keyed header"));
    }

    @Test
    @DisplayName("Given entry count mismatch in strict mode When parsed Then exception")
    void parseKeyedTabularObject_givenCountMismatchStrict_whenParsed_thenThrows() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"servers[2:]{host,port}:",
            "  alpha: a.example.com,8080"};
        context.currentLine = 0;
        final Headers.KeyedHeaderMatch header = Headers.matchKeyedArrayHeader(context.lines[0]);

        // When / Then
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> KeyedObjectDecoder.parseKeyedTabularObject(context.lines[0], header, 1, context));
        assertTrue(ex.getMessage().contains("does not match declared length"));
    }

    @Test
    @DisplayName("Given blank line inside object in strict mode When parsed Then exception")
    void parseKeyedTabularObject_givenBlankLineInsideStrict_whenParsed_thenThrows() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"servers[1:]{host}:",
            "  alpha: a.example.com",
            "",
            "  beta: b.example.com"};
        context.currentLine = 0;
        final Headers.KeyedHeaderMatch header = Headers.matchKeyedArrayHeader(context.lines[0]);

        // When / Then
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> KeyedObjectDecoder.parseKeyedTabularObject(context.lines[0], header, 1, context));
        assertTrue(ex.getMessage().contains("Blank line inside keyed object"));
    }

    @Test
    @DisplayName("Given blank line before first entry row When parsed Then accepted")
    @SuppressWarnings("unchecked")
    void parseKeyedTabularObject_givenBlankLineBeforeFirstRow_whenParsed_thenAccepted() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"servers[1:]{host}:",
            "",
            "  alpha: a.example.com"};
        context.currentLine = 0;
        final Headers.KeyedHeaderMatch header = Headers.matchKeyedArrayHeader(context.lines[0]);

        // When
        final Map<String, Object> result =
            KeyedObjectDecoder.parseKeyedTabularObject(context.lines[0], header, 1, context);

        // Then
        assertEquals(1, result.size());
        assertInstanceOf(Map.class, result.get("alpha"));
    }

    @Test
    @DisplayName("Given over-indented line in strict mode When parsed Then exception")
    void parseKeyedTabularObject_givenOverIndentedStrict_whenParsed_thenThrows() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"servers[1:]{host}:",
            "  alpha: a.example.com",
            "    orphan"};
        context.currentLine = 0;
        final Headers.KeyedHeaderMatch header = Headers.matchKeyedArrayHeader(context.lines[0]);

        // When / Then
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> KeyedObjectDecoder.parseKeyedTabularObject(context.lines[0], header, 1, context));
        assertTrue(ex.getMessage().contains("Over-indented line"));
    }

    @Test
    @DisplayName("Given entry without colon in lenient mode When parsed Then row skipped")
    void parseKeyedTabularObject_givenMissingColonLenient_whenParsed_thenSkipped() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.options = DecodeOptions.withStrict(false);
        context.lines = new String[]{"servers[1:]{host}:",
            "  alpha: a.example.com",
            "  broken"};
        context.currentLine = 0;
        final Headers.KeyedHeaderMatch header = Headers.matchKeyedArrayHeader(context.lines[0]);

        // When
        final Map<String, Object> result =
            KeyedObjectDecoder.parseKeyedTabularObject(context.lines[0], header, 1, context);

        // Then
        assertEquals(1, result.size());
        assertEquals(AFTER_ENTRIES_LINE_INDEX, context.currentLine);
    }

    @Test
    @DisplayName("Given shallower next line When parsed Then object ends")
    void parseKeyedTabularObject_givenShallowerLine_whenParsed_thenEnds() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"servers[1:]{host}:",
            "  alpha: a.example.com",
            "next: value"};
        context.currentLine = 0;
        final Headers.KeyedHeaderMatch header = Headers.matchKeyedArrayHeader(context.lines[0]);

        // When
        final Map<String, Object> result =
            KeyedObjectDecoder.parseKeyedTabularObject(context.lines[0], header, 1, context);

        // Then
        assertEquals(1, result.size());
        assertEquals(2, context.currentLine);
    }
}
