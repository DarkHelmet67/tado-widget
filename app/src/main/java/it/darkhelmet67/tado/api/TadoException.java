package it.darkhelmet67.tado.api;

public class TadoException extends Exception {
    public enum Kind {
        /** Not signed in, or the refresh token was rejected: the user must sign in again. */
        AUTH_EXPIRED,
        /** HTTP 429: the daily request quota is used up. */
        RATE_LIMITED,
        /** Device-code flow: the user has not approved yet. Keep polling. */
        PENDING,
        /** Device-code flow: poll less often. */
        SLOW_DOWN,
        /** Device-code flow: the user declined. */
        DENIED,
        /** Device-code flow: the code expired. */
        EXPIRED,
        /** Could not reach tado°. */
        NETWORK,
        /** Anything else (unexpected status or payload). */
        API
    }

    public final Kind kind;

    public TadoException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public TadoException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }
}
