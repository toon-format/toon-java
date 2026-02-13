package dev.toonformat.jtoon;

/**
 * Delimiter options for tabular array rows and inline primitive arrays.
 */
public enum Delimiter {
    /**
     * Comma delimiter (,) - default option.
     */
    COMMA(","),

    /**
     * Tab delimiter (\t).
     */
    TAB("\t"),

    /**
     * Pipe delimiter (|).
     */
    PIPE("|");

    private final String value;

    Delimiter(final String delimiterValue) {
        this.value = delimiterValue;
    }

    /**
     * Returns the string representation of this delimiter.
     * @return the string value of this delimiter
     */
    @Override
    public String toString() {
        return value;
    }


    /**
     * Returns the character representation of this delimiter.
     * @return the character value of this delimiter
     */
    public char getValue() {
        return value.charAt(0);
    }
}
