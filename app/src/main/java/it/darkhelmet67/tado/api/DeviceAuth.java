package it.darkhelmet67.tado.api;

/** The result of starting the OAuth device-code flow. */
public final class DeviceAuth {
    public final String deviceCode;
    public final String userCode;
    public final String verificationUri;
    public final String verificationUriComplete;
    public final int intervalSeconds;
    public final int expiresInSeconds;

    public DeviceAuth(String deviceCode, String userCode, String verificationUri,
                      String verificationUriComplete, int intervalSeconds, int expiresInSeconds) {
        this.deviceCode = deviceCode;
        this.userCode = userCode;
        this.verificationUri = verificationUri;
        this.verificationUriComplete = verificationUriComplete;
        this.intervalSeconds = intervalSeconds;
        this.expiresInSeconds = expiresInSeconds;
    }
}
