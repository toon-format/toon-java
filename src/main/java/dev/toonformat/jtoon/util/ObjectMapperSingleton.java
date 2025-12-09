package dev.toonformat.jtoon.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.MapperFeature;
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
                .defaultTimeZone(TimeZone.getTimeZone("UTC")) // set a default timezone for dates
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
        }
        return INSTANCE;
    }
}
