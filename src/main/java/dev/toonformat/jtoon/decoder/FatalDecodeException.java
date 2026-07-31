package dev.toonformat.jtoon.decoder;

/**
 * Signals a decode defect that is fatal in strict and non-strict mode alike:
 * a bare scalar line outside root primitive position (§5.2) and characters
 * after a closing quote (§7.4). {@link ValueDecoder#decode} rethrows it even
 * in lenient mode instead of converting it to {@code null}.
 */
final class FatalDecodeException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    FatalDecodeException(final String message) {
        super(message);
    }
}
