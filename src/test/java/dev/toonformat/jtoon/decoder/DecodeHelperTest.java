package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class DecodeHelperTest {
    private final DecodeContext context = new DecodeContext();

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<DecodeHelper> constructor = DecodeHelper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    @DisplayName("Returns true or false if there's a blank line")
    void isBlankLine () {
        assertTrue(DecodeHelper.isBlankLine(""));
        assertFalse(DecodeHelper.isBlankLine("items[1]: \"\""));
    }

    @Test
    void handleUnexpectedIndentation () {
        setUpContext("a:\n   b: 1");
        assertThrows(IllegalArgumentException.class, () -> DecodeHelper.handleUnexpectedIndentation(context));
    }

    @Test
    @DisplayName("Should find the correct depth for primitive arrays")
    void findDepthForPrimitiveArrays () {
        setUpContext("tags[3]: 1,2,3");
        int result = DecodeHelper.getDepth("tags[3]: 1,2,3", context);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should find the correct depth for nested arrays")
    void findDepthForNestedArrays () {
        setUpContext("items[1]:\n  - id: 1\n    nested:\n      x: 1");
        int result = DecodeHelper.getDepth("items[1]:\n  - id: 1\n    nested:\n      x: 1", context);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should find the correct depth for tabular arrays")
    void findDepthForTabularArrays () {
        setUpContext("items[2]{\"order:id\",\"full name\"}:\n  1,Ada\n  2,Bob");
        int result = DecodeHelper.getDepth("items[2]{\"order:id\",\"full name\"}:\n  1,Ada\n  2,Bob", context);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should find the correct depth for objects")
    void findDepthForObjects () {
        setUpContext("id: 123\nname: Ada\nactive: true");
        int result = DecodeHelper.getDepth("id: 123\nname: Ada\nactive: true", context);
        assertEquals(0, result);
    }

    private void setUpContext (String toon) {
        this.context.lines = toon.split("\n", -1);
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter().toString();
    }
}
