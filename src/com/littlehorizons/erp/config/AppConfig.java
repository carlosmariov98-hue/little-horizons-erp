package com.littlehorizons.erp.config;

import com.littlehorizons.erp.exception.ConfigurationException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Loads application settings from a .properties file.
 * Single responsibility: read configuration values. It knows nothing about the database itself.
 */
public class AppConfig {

    public static final String DEFAULT_CONFIG_FILE = "db.properties";

    private final String filePath;
    private final Properties properties = new Properties();

    /**
     * Loads the default configuration file (db.properties in the working directory).
     *
     * @throws ConfigurationException if the file is missing or cannot be read
     */
    public AppConfig() throws ConfigurationException {
        this(DEFAULT_CONFIG_FILE);
    }

    /**
     * Loads the configuration file located at the given path.
     *
     * @param filePath path of the .properties file
     * @throws ConfigurationException if the file is missing or cannot be read
     */
    public AppConfig(String filePath) throws ConfigurationException {
        this.filePath = filePath;
        load();
    }

    private void load() throws ConfigurationException {
        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Configuration file '" + filePath + "' was not found. "
                    + "Copy 'db.properties.example' to 'db.properties' in the project root "
                    + "and fill in your MySQL credentials.", e);
        } catch (IOException e) {
            throw new ConfigurationException("Configuration file '" + filePath + "' could not be read.", e);
        }
    }

    /**
     * Returns the value of a mandatory property.
     *
     * @param key property name
     * @return the trimmed value
     * @throws ConfigurationException if the property is missing or blank
     */
    public String getRequired(String key) throws ConfigurationException {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigurationException("Property '" + key + "' is missing or empty in '" + filePath + "'.");
        }
        return value.trim();
    }

    /**
     * Returns the value of an optional property.
     *
     * @param key          property name
     * @param defaultValue value returned when the property is not defined
     * @return the trimmed value, or the default value
     */
    public String getOptional(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null ? defaultValue : value.trim();
    }
}