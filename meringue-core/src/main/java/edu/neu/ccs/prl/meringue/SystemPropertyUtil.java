package edu.neu.ccs.prl.meringue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public final class SystemPropertyUtil {
    private SystemPropertyUtil() {
        throw new AssertionError();
    }

    public static void load(File file) throws IOException {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            p.load(in);
        }
        for (String key : p.stringPropertyNames()) {
            System.setProperty(key, p.getProperty(key));
        }
    }

    public static void store(File file, String name, Properties properties) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            properties.store(out, name);
        }
    }

    public static void loadSystemProperties(String key) throws IOException {
        String path = System.getProperty(key);
        if (path != null) {
            load(new File(path));
        }
    }
}