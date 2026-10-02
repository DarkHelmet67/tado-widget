package it.darkhelmet67.tado.auth;

/** Persists the OAuth tokens. The production implementation is {@link SecureTokenStore}. */
public interface TokenStore {
    /** @return the stored tokens, or null when the user is not signed in */
    Tokens load();

    void save(Tokens tokens);

    void clear();
}
