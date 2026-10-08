package config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Application settings: config/app.properties, overridden by JVM system properties. */
public final class Settings {
    private static final Path DEFAULT_FILE = Path.of("config", "app.properties");

    private final Properties values;

    private Settings(Properties values) {
        this.values = values;
    }

    public static Settings load() {
        return load(DEFAULT_FILE);
    }

    public static Settings load(Path file) {
        Properties props = new Properties();
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("cannot read " + file, e);
            }
        }
        for (String name : System.getProperties().stringPropertyNames()) {
            if (name.startsWith("password.") || name.startsWith("session.") || name.startsWith("profile.")) {
                props.setProperty(name, System.getProperty(name));
            }
        }
        return new Settings(props);
    }

    public static Settings of(Properties props) {
        Properties copy = new Properties();
        copy.putAll(props);
        return new Settings(copy);
    }

    public String value(String key, String fallback) {
        String v = values.getProperty(key);
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    public int intValue(String key, int fallback) {
        String v = value(key, null);
        if (v == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean boolValue(String key, boolean fallback) {
        String v = value(key, null);
        return v == null ? fallback : Boolean.parseBoolean(v);
    }
}
