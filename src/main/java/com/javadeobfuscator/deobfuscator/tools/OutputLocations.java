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
import java.io.IOException;

/**
 * Output paths under the working directory's {@code out} folder.
 * A returned path is never the input file, and an existing path is not reused.
 */
public final class OutputLocations {
    public static final String DIRECTORY_NAME = "out";

    private OutputLocations() {
    }

    public static File jarOutput(File input) {
        return unique(new File(directory(), stripExtension(input.getName()) + "-deobfuscated.jar"), input, true);
    }

    public static File fileOutput(File input, String extensionWithDot) {
        String ext = extensionWithDot.startsWith(".") ? extensionWithDot : "." + extensionWithDot;
        return unique(new File(directory(), stripExtension(input.getName()) + "-deobfuscated" + ext), input, true);
    }

    public static File directoryOutput(File input) {
        return unique(new File(directory(), stripExtension(input.getName()) + "-deobfuscated"), input, false);
    }

    public static File directory() {
        File dir = new File(DIRECTORY_NAME);
        if (!dir.exists() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IllegalStateException("Could not create output directory " + dir.getAbsolutePath());
        }
        return dir;
    }

    public static boolean sameFile(File left, File right) {
        if (left == null || right == null) {
            return false;
        }
        try {
            return left.getCanonicalFile().equals(right.getCanonicalFile());
        } catch (IOException ex) {
            return left.getAbsoluteFile().equals(right.getAbsoluteFile());
        }
    }

    static File unique(File candidate, File input, boolean fileName) {
        File parent = candidate.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IllegalStateException("Could not create " + parent.getAbsolutePath());
        }
        File current = candidate;
        int suffix = 2;
        while (sameFile(current, input) || current.exists()) {
            String name = candidate.getName();
            String nextName;
            int dot = name.lastIndexOf('.');
            if (fileName && dot > 0) {
                nextName = name.substring(0, dot) + "-" + suffix + name.substring(dot);
            } else {
                nextName = name + "-" + suffix;
            }
            current = new File(candidate.getParentFile(), nextName);
            suffix++;
            if (suffix > 1000) {
                throw new IllegalStateException("Could not find a free output name near " + candidate.getAbsolutePath());
            }
        }
        return current;
    }

    static String stripExtension(String name) {
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        String base = slash >= 0 ? name.substring(slash + 1) : name;
        int dot = base.lastIndexOf('.');
        if (dot <= 0) {
            return base;
        }
        return base.substring(0, dot);
    }
}
