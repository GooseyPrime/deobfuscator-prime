/*
 * Copyright 2026 GooseyPrime / java-deobfuscator
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.javadeobfuscator.deobfuscator.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Remembers optional external-tool paths. Missing tools stay missing;
 * this file never invents a successful run.
 */
public final class ToolSettings {
    private final File file;
    private final Properties properties = new Properties();

    public ToolSettings(File file) {
        this.file = file;
        if (file != null && file.isFile()) {
            try (FileInputStream in = new FileInputStream(file)) {
                properties.load(in);
            } catch (IOException ignored) {
                properties.clear();
            }
        }
    }

    public static ToolSettings load() {
        File dir = new File(System.getProperty("user.home"), ".deobfuscator-prime");
        return new ToolSettings(new File(dir, "tool-paths.properties"));
    }

    public String get(String key) {
        String value = properties.getProperty(key, "");
        return value == null ? "" : value;
    }

    public void set(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value.trim());
        }
        save();
    }

    private void save() {
        if (file == null) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            properties.store(out, "Optional external deobfuscator tool paths");
        } catch (IOException ignored) {
            // The UI still uses the in-memory value for this session.
        }
    }
}
