package it.darkhelmet67.tado.auth;

/** OAuth tokens issued by tado°. Access tokens live ~10 minutes, refresh tokens are single-use. */
public final class Tokens {
    public final String accessToken;
    public final String refreshToken;
    public final long expiresAtMillis;

    public Tokens(String accessToken, String refreshToken, long expiresAtMillis) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAtMillis = expiresAtMillis;
    }
}
