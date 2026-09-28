package pl.gpxplorer.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public record DatabaseConfig(String url, String user, String password) {
    private static final String RESOURCE = "/pl/gpxplorer/db.properties";

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        try (InputStream in = DatabaseConfig.class.getResourceAsStream(RESOURCE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Nie udalo sie wczytac " + RESOURCE, e);
        }
        return new DatabaseConfig(
                value(properties, "db.url", "GPXPLORER_DB_URL"),
                value(properties, "db.user", "GPXPLORER_DB_USER"),
                value(properties, "db.password", "GPXPLORER_DB_PASSWORD")
        );
    }

    private static String value(Properties properties, String key, String envName) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return properties.getProperty(key);
    }
}
