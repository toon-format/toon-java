package dev.toonformat.jtoon.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.blackbird.BlackbirdModule;
import java.util.TimeZone;

/**
 * Provides a singleton ObjectMapper instance.
 */
public final class ObjectMapperSingleton {
    /**
     * Holds the singleton ObjectMapper.
     */
    @Nullable
    private static volatile ObjectMapper instance;

    private ObjectMapperSingleton() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Returns the singleton ObjectMapper.
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getInstance() {
        ObjectMapper result = instance;
        if (result == null) {
            synchronized (ObjectMapperSingleton.class) {
                result = instance;
                if (result == null) {
                    result = JsonMapper.builder()
                        .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.ALWAYS))
                        .addModule(new BlackbirdModule()) // Speeds up Jackson by 20-40%
                                                              // using MethodHandles (faster than Afterburner)
                        .defaultTimeZone(TimeZone.getTimeZone("UTC")) // set a default timezone for dates
                        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                        .build();
                    instance = result;
                }
            }
        }
        return result;
    }
}
