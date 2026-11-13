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
            throw new ConfigurationException("No se encontró el archivo de configuración '" + filePath + "'. "
                    + "Copie 'db.properties.example' como 'db.properties' en la raíz del proyecto "
                    + "y escriba sus credenciales de MySQL.", e);
        } catch (IOException e) {
            throw new ConfigurationException("No se pudo leer el archivo de configuración '" + filePath + "'.", e);
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
            throw new ConfigurationException("La propiedad '" + key + "' falta o está vacía en '" + filePath + "'.");
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
