package config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/** conf/app.properties, overridable by -Ddesk.KEY, DESK_KEY env vars, or {@link #with}. */
public final class AppConfig {
    private final Properties props = new Properties();
    private final Map<String, String> overrides = new HashMap<>();

    private AppConfig() {}

    public static AppConfig load() {
        return load(Path.of("conf", "app.properties"));
    }

    public static AppConfig load(Path file) {
        AppConfig cfg = new AppConfig();
        try (InputStream in = Files.newInputStream(file)) {
            cfg.props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read config " + file, e);
        }
        return cfg;
    }

    /** Copy of this config with one key replaced (used by tests and the ops console). */
    public AppConfig with(String key, String value) {
        AppConfig copy = new AppConfig();
        copy.props.putAll(props);
        copy.overrides.putAll(overrides);
        copy.overrides.put(key, value);
        return copy;
    }

    public String get(String key) {
        if (overrides.containsKey(key)) return overrides.get(key);
        String sys = System.getProperty("desk." + key);
        if (sys != null && !sys.isBlank()) return sys.trim();
        String env = System.getenv("DESK_" + key.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) return env.trim();
        String v = props.getProperty(key);
        if (v == null) throw new IllegalStateException("missing config key " + key);
        return v.trim();
    }

    public Path pgBin() { return Path.of(get("pg.bin")); }

    public String merchantId() { return get("pg.mid"); }

    public long pgTimeoutSeconds() { return Long.parseLong(get("pg.timeoutSeconds")); }

    public Path pgHome() { return Path.of(get("pg.home")); }

    public int maxLinesPerBatch() { return Integer.parseInt(get("refund.maxLinesPerBatch")); }

    public String batchPrefix() { return get("refund.batchPrefix"); }

    public Path workDir() { return Path.of(get("refund.workDir")); }

    public Path catalogFile() { return Path.of(get("catalog.file")); }
}
