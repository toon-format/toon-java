package dev.toonformat.jtoon.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Provides a singleton ObjectMapper instance.
 *
 * <p>
 * By default a plain {@link JsonMapper} is used (no optimizer module such as
 * Blackbird/Afterburner is registered). This keeps JToon usable on Java 17+ regardless
 * of the Jackson version: Jackson 3.3.0's {@code BlackbirdModule} requires Java 25,
 * so registering it here would break Java 17-24 runtimes.
 * </p>
 *
 * <p>
 * Applications that want an optimizer module (e.g. {@code BlackbirdModule} on Java 25,
 * or {@code AfterburnerModule}) can supply their own {@link ObjectMapper} via
 * {@link #setInstance(ObjectMapper)} before the first use of JToon, or build one with
 * {@link #createMapper(JacksonModule...)}.
 * </p>
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
                    result = createDefaultMapper();
                    instance = result;
                }
            }
        }
        return result;
    }

    /**
     * Overrides the singleton ObjectMapper. Must be called before the first call to
     * {@link #getInstance()} (and therefore before the first use of JToon); otherwise an
     * {@link IllegalStateException} is thrown.
     *
     * @param mapper the ObjectMapper to use (must not be null)
     */
    public static void setInstance(final ObjectMapper mapper) {
        Objects.requireNonNull(mapper, "ObjectMapper cannot be null");
        synchronized (ObjectMapperSingleton.class) {
            if (instance != null) {
                throw new IllegalStateException(
                    "ObjectMapperSingleton is already initialized; call setInstance before first use.");
            }
            instance = mapper;
        }
    }

    /**
     * Returns whether the singleton has been initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * Creates the default plain ObjectMapper used by JToon (no optimizer module).
     *
     * @return a new ObjectMapper
     */
    public static ObjectMapper createDefaultMapper() {
        return JsonMapper.builder()
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.ALWAYS))
            .defaultTimeZone(TimeZone.getTimeZone("UTC")) // set a default timezone for dates
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();
    }

    /**
     * Creates an ObjectMapper based on the default configuration, with the given
     * optimizer/extension modules registered. Convenience for applications that want
     * to add e.g. {@code BlackbirdModule} (Java 25+) or {@code AfterburnerModule}.
     *
     * @param modules modules to register (may be empty)
     * @return a new ObjectMapper
     */
    public static ObjectMapper createMapper(final JacksonModule... modules) {
        final JsonMapper.Builder builder = JsonMapper.builder()
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.ALWAYS))
            .defaultTimeZone(TimeZone.getTimeZone("UTC")) // set a default timezone for dates
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        if (modules != null) {
            for (final JacksonModule module : modules) {
                if (module != null) {
                    builder.addModule(module);
                }
            }
        }
        return builder.build();
    }
}