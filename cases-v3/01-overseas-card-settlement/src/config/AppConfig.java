package config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Loads conf/app.properties; system properties and SETTLE_* environment variables override. */
public final class AppConfig {
    private final Properties props = new Properties();

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

    public String get(String key) {
        String sys = System.getProperty("settle." + key);
        if (sys != null && !sys.isBlank()) return sys.trim();
        String env = System.getenv("SETTLE_" + key.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) return env.trim();
        String v = props.getProperty(key);
        if (v == null) throw new IllegalStateException("missing config key " + key);
        return v.trim();
    }

    public Path fxqBin() { return Path.of(get("fxq.bin")); }

    public long fxqTimeoutSeconds() { return Long.parseLong(get("fxq.timeoutSeconds")); }

    public Path dataDir() { return Path.of(get("data.dir")); }

    public ZoneId businessZone() { return ZoneId.of(get("business.zone")); }

    public List<String> boardCurrencies() {
        return Arrays.stream(get("board.currencies").split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
