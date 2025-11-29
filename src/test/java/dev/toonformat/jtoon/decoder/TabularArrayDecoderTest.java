package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class TabularArrayDecoderTest {
    private final DecodeContext context = new DecodeContext();

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<TabularArrayDecoder> constructor = TabularArrayDecoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    @DisplayName("Parse TOON format tabular array to JSON")
    void parseTabularArray () {
        setUpContext("[2]{id,value}:\n  1,null\n  2,\"test\"");
        List<Object> result = TabularArrayDecoder.parseTabularArray(
            "[2]{id,value}:\n  1,null\n  2,\"test\"", 0,
            Delimiter.COMMA.toString(), context);
        assertEquals("[{id=1, value=null}, {id=2, value=test}]", result.toString());
    }

    @Test
    @DisplayName("Throws an exception if the wrong delimiter is being used")
    void inCaseOfMismatchInDelimiter_ThrowAnException () {
        setUpContext("[2]{id,value}:\n  1,null\n  2,\"test\"");
        assertThrows(IllegalArgumentException.class, () -> TabularArrayDecoder.parseTabularArray(
            "[2]{id,value}:\n  1,null\n  2,\"test\"", 0,
            Delimiter.TAB.toString(), context));
    }

    private void setUpContext (String toon) {
        this.context.lines = toon.split("\n", -1);
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter().toString();
    }
}
