package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class ObjectDecoderTest {
    private final DecodeContext context = new DecodeContext();

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<ObjectDecoder> constructor = ObjectDecoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    @DisplayName("Should parse scalar value to JSON")
    void parseBareScalarValue () {
        setUpContext("v: \"true\"");
        Object result = ObjectDecoder.parseBareScalarValue("v: \"true\"", 0, context);
        assertEquals("v: \"true\"", result.toString());
    }

    @Test
    @DisplayName("Should parse item value to JSON")
    void parseObjectItemValue () {
        setUpContext("note: \"a,b\"");
        Object result = ObjectDecoder.parseObjectItemValue("note: \"a,b\"", 0, context);
        assertEquals("note: \"a,b\"", result.toString());
    }

    private void setUpContext (String toon) {
        this.context.lines = toon.split("\n", -1);
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter().toString();
    }
}
