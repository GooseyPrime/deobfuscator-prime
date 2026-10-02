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

public final class ToolLocator {
    private ToolLocator() {
    }

    public static File findOnPath(String... names) {
        return searchPath(System.getenv("PATH"), names);
    }

    public static File searchPath(String pathEnv, String... names) {
        if (pathEnv == null || pathEnv.trim().isEmpty() || names == null) {
            return null;
        }
        String[] dirs = pathEnv.split(File.pathSeparator);
        for (String dir : dirs) {
            if (dir == null || dir.trim().isEmpty()) {
                continue;
            }
            for (String name : names) {
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                File candidate = new File(dir, name);
                if (candidate.isFile() && (isWindows() || candidate.canExecute())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase(java.util.Locale.ROOT).startsWith("windows");
    }
}
