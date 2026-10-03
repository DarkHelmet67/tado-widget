package it.darkhelmet67.tado.api;

/** Sink for one-line network log entries. Never receives tokens, credentials or response bodies. */
public interface NetworkLog {
    NetworkLog NONE = line -> { };

    void log(String line);
}
