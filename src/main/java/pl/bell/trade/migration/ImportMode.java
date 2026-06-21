package pl.bell.trade.migration;

public enum ImportMode {
    REPLACE,
    ADD,
    MAX;

    public static ImportMode parse(String raw, ImportMode fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
