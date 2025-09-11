package com.example.slotmachine;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final Properties config = new Properties();
    private static String currentConfigFile = "src/main/resources/configs/smallconfig.properties";

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (FileInputStream input = new FileInputStream(currentConfigFile)) {
            config.clear();
            config.load(input);
        } catch (IOException e) {
            System.err.println("Failed to load configuration file: " + currentConfigFile);
            e.printStackTrace();
        }
    }

    public static void setConfigFile(String configFileName) {
        currentConfigFile = "src/main/resources/configs/" + configFileName;
        loadConfig();
    }

    public static int get(String key) {
        return Integer.parseInt(config.getProperty(key));
    }
}