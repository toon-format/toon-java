package dev.toonformat.jtoon;

/**
 * Delimiter options for tabular array rows and inline primitive arrays.
 */
public enum Delimiter {
    /**
     * Comma delimiter (,) - default option
     */
    COMMA(','),

    /**
     * Tab delimiter (\t)
     */
    TAB('\t'),

    /**
     * Pipe delimiter (|)
     */
    PIPE('|');

    private final char value;

    Delimiter(char value) {
        this.value = value;
    }

    /**
     * Returns the string representation of this delimiter.
     * @return the string value of this delimiter
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public char getValue() {
        return value;
    }
}
