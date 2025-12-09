package dev.toonformat.jtoon.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.afterburner.AfterburnerModule;

import java.util.TimeZone;

/**
 * Provides a singleton ObjectMapper instance.
 */
public final class ObjectMapperSingleton {
    /**
     * Holds the singleton ObjectMapper.
     */
    private static ObjectMapper INSTANCE;

    private ObjectMapperSingleton() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Returns the singleton ObjectMapper.
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.ALWAYS))
                .addModule(new AfterburnerModule()) // Speeds up Jackson by 20–40% in most real-world cases
                // .disable(MapperFeature.DEFAULT_VIEW_INCLUSION) in Jackson 3 this is default disabled
                // .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) in Jackson 3 this is default disabled
                // .configure(SerializationFeature.INDENT_OUTPUT, false)  in Jackson 3 this is default false
                .defaultTimeZone(TimeZone.getTimeZone("UTC")) // set a default timezone for dates
                .build();
        }
        return INSTANCE;
    }
}
