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

/**
 * Where an external tool was found, or why it cannot be launched.
 * {@link Mode#MANUAL_ONLY} means the user must click Run (for example npx,
 * which may download a package). Dropping a file does not auto-run that mode.
 */
public final class ResolvedTool {
    public enum Mode {
        READY,
        MANUAL_ONLY,
        MISSING
    }

    private final Mode mode;
    private final String toolName;
    private final File executable;
    private final String message;

    private ResolvedTool(Mode mode, String toolName, File executable, String message) {
        this.mode = mode;
        this.toolName = toolName;
        this.executable = executable;
        this.message = message;
    }

    public static ResolvedTool ready(String toolName, File executable, String message) {
        return new ResolvedTool(Mode.READY, toolName, executable, message);
    }

    public static ResolvedTool manual(String toolName, File executable, String message) {
        return new ResolvedTool(Mode.MANUAL_ONLY, toolName, executable, message);
    }

    public static ResolvedTool missing(String toolName, String message) {
        return new ResolvedTool(Mode.MISSING, toolName, null, message);
    }

    public Mode getMode() {
        return mode;
    }

    public String getToolName() {
        return toolName;
    }

    public File getExecutable() {
        return executable;
    }

    public String getMessage() {
        return message;
    }

    public boolean canRun() {
        return mode == Mode.READY || mode == Mode.MANUAL_ONLY;
    }

    public boolean autoRun() {
        return mode == Mode.READY;
    }
}
