package it.darkhelmet67.tado.api;

public final class Zone {
    public final long id;
    public final String name;
    public final String type;

    public Zone(long id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
