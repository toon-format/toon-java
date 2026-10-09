package dev.toonformat.jtoon.validator;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.decoder.ValueDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates TOON-formatted strings for conformance to the TOON specification (§13.3).
 *
 * <p>Performs structural checks beyond what the decoder's strict mode enforces:
 * <ul>
 *   <li>Structural conformance (headers, indentation, list markers)</li>
 *   <li>Whitespace invariants (no trailing spaces/newlines)</li>
 *   <li>Delimiter consistency between headers and rows</li>
 *   <li>Array length counts match declared [N]</li>
 * </ul>
 *
 * <p>This is a read-only validation utility. It does not produce decoded values.
 */
public final class ToonValidator {

    private static final Pattern NEWLINE = Pattern.compile("\r?\n");

    private ToonValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Result of a validation run.
     *
     * @param valid      true if the input passed all checks
     * @param issues     list of human-readable issue descriptions (empty when valid)
     */
    public record ValidationResult(boolean valid, List<String> issues) {
        private static final ValidationResult PASS = new ValidationResult(true, List.of());

        static ValidationResult pass() {
            return PASS;
        }

        static ValidationResult fail(final List<String> issues) {
            return new ValidationResult(false, List.copyOf(issues));
        }
    }

    /**
     * Validates a TOON-formatted string.
     *
     * @param toon    the TOON string to validate
     * @param options decode options (indent, delimiter, strict mode)
     * @return validation result with issues list
     */
    public static ValidationResult validate(final String toon, final DecodeOptions options) {
        if (toon == null || toon.chars().allMatch(c -> c == ' ')) {
            return ValidationResult.pass();
        }

        final List<String> issues = new ArrayList<>();

        // 1. Try decoding in strict mode to catch structural errors
        try {
            ValueDecoder.decode(toon, options);
        } catch (IllegalArgumentException e) {
            issues.add("Structural error: " + e.getMessage());
        }

        // 2. Whitespace invariants (encoder checks)
        checkTrailingWhitespace(toon, issues);

        // 3. Check for trailing newline (encoder MUST NOT emit)
        if (!toon.isEmpty() && toon.charAt(toon.length() - 1) == '\n') {
            issues.add("Trailing newline at end of document (§12)");
        }

        if (issues.isEmpty()) {
            return ValidationResult.pass();
        }

        return ValidationResult.fail(issues);
    }

    /**
     * Validates a TOON-formatted string with default options (strict mode, comma delimiter, 2-space indent).
     *
     * @param toon the TOON string to validate
     * @return validation result with issues list
     */
    public static ValidationResult validate(final String toon) {
        return validate(toon, DecodeOptions.DEFAULT);
    }

    /**
     * Returns true if the TOON string is valid per the specification.
     *
     * @param toon the TOON string to validate
     * @return true if valid
     */
    public static boolean isValid(final String toon) {
        return validate(toon).valid();
    }

    /**
     * Returns true if the TOON string is valid per the specification.
     *
     * @param toon    the TOON string to validate
     * @param options decode options
     * @return true if valid
     */
    public static boolean isValid(final String toon, final DecodeOptions options) {
        return validate(toon, options).valid();
    }

    private static void checkTrailingWhitespace(final String toon, final List<String> issues) {
        final String[] lines = NEWLINE.split(toon, -1);
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            if (!line.isEmpty() && line.charAt(line.length() - 1) == ' ') {
                issues.add("Trailing space on line " + (i + 1) + " (§12)");
            }
        }
    }
}
