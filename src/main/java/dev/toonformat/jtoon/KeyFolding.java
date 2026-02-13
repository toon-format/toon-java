package dev.toonformat.jtoon;

/**
 * Enable key folding to collapse single-key wrapper chains.
 */
public enum KeyFolding {
    /**
     * Safe mode:
     * When set to 'safe', nested objects with single keys are collapsed into dotted paths.
     * (e.g., data.metadata.items instead of nested indentation).
     */
    SAFE,
    
    /**
     * Off mode: default.
     */
    OFF
}

