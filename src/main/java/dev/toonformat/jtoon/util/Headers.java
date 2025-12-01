package dev.toonformat.jtoon.util;

import java.util.regex.Pattern;

/**
 * Patterns in form of regex that must be followed in order to decode arrays, tabular, keyed arrays
 */
public class Headers {

    /**
     * Matches standalone array headers: [3], [#2], [3\t], [2|]
     * Group 1: optional # marker, Group 2: digits, Group 3: optional delimiter
     */
    public static final Pattern ARRAY_HEADER_PATTERN = Pattern.compile("^\\[(#?)(\\d+)([\\t|])?]");

    /**
     * Matches tabular array headers with field names: [2]{id,name,role}:
     * Group 1: optional # marker, Group 2: digits, Group 3: optional delimiter,
     * Group 4: field spec
     */
    public static final Pattern TABULAR_HEADER_PATTERN = Pattern.compile("^\\[(#?)(\\d+)([\\t|])?]\\{(.+)}:");

    /**
     * Matches keyed array headers: items[2]{id,name}: or tags[3]: or data[4]{id}:
     * Captures: group(1)=key, group(2)=#marker, group(3)=delimiter,
     * group(4)=optional field spec
     */
    public static final Pattern KEYED_ARRAY_PATTERN = Pattern.compile("^(.+?)\\[(#?)\\d+([\\t|])?](\\{[^}]+})?:.*$");

}
