package dev.toonformat.jtoon.encoder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class ArrayEncoderTest {
    ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void isArrayOfPrimitives() {
        //given
        ObjectNode dataTable = MAPPER.createObjectNode();

        //when
        boolean arrayOfArrays = ArrayEncoder.isArrayOfPrimitives(dataTable);

        //then
        assertFalse(arrayOfArrays);
    }

    @Test
    void isArrayOfArrays() {
        //given
        ObjectNode dataTable = MAPPER.createObjectNode();

        //when
        boolean arrayOfArrays = ArrayEncoder.isArrayOfArrays(dataTable);

        //then
        assertFalse(arrayOfArrays);
    }

    @Test
    void isArrayOfObjects() {
        //given
        ObjectNode dataTable = MAPPER.createObjectNode();

        //when
        boolean arrayOfArrays = ArrayEncoder.isArrayOfObjects(dataTable);

        //then
        assertFalse(arrayOfArrays);
    }

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<ArrayEncoder> constructor = ArrayEncoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
                assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }
}