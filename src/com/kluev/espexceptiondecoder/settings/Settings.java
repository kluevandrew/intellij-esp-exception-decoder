package com.kluev.espexceptiondecoder.settings;

import com.intellij.ide.util.PropertiesComponent;

public class Settings {
    private static final PropertiesComponent properties = PropertiesComponent.getInstance();

    public static void set(String key, String value) {
        properties.setValue(key, value);
    }

    public static String get(String key) {
        return properties.getValue(key);
    }
}
