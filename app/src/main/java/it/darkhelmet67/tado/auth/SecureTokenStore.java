package it.darkhelmet67.tado.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/** Stores tokens in {@link EncryptedSharedPreferences}, keyed by the Android Keystore. */
public class SecureTokenStore implements TokenStore {
    private static final String TAG = "SecureTokenStore";
    private static final String FILE = "tado_secure";
    private static final String KEY_ACCESS = "access";
    private static final String KEY_REFRESH = "refresh";
    private static final String KEY_EXPIRES = "expires";

    private final Context context;
    private SharedPreferences prefs;
    private volatile String lastError;

    public SecureTokenStore(Context context) {
        this.context = context.getApplicationContext();
    }

    private synchronized SharedPreferences prefs() {
        if (prefs == null) {
            try {
                MasterKey key = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                prefs = EncryptedSharedPreferences.create(context, FILE, key,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            } catch (Exception e) {
                Log.e(TAG, "Secure storage unavailable", e);
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }
        return prefs;
    }

    @Override
    public Tokens load() {
        SharedPreferences p = prefs();
        if (p == null) return null;
        try {
            String access = p.getString(KEY_ACCESS, null);
            String refresh = p.getString(KEY_REFRESH, null);
            if (access == null || refresh == null) return null;
            return new Tokens(access, refresh, p.getLong(KEY_EXPIRES, 0));
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not read tokens", e);
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            return null;
        }
    }

    @Override
    public void save(Tokens tokens) {
        SharedPreferences p = prefs();
        if (p == null) return;
        p.edit()
                .putString(KEY_ACCESS, tokens.accessToken)
                .putString(KEY_REFRESH, tokens.refreshToken)
                .putLong(KEY_EXPIRES, tokens.expiresAtMillis)
                .apply();
    }

    @Override
    public String lastError() {
        return lastError;
    }

    @Override
    public void clear() {
        SharedPreferences p = prefs();
        if (p != null) p.edit().clear().apply();
    }
}
