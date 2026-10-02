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
 * Plain status for a single attempt. The display text says a file was
 * deobfuscated only when a tool ran, finished, and the output differs.
 */
public final class JobStatus {
    private final String detectedType;
    private final String obfuscatorGuess;
    private final String toolUsed;
    private final boolean toolRan;
    private final boolean success;
    private final boolean outputChanged;
    private final File output;
    private final String detail;

    public JobStatus(String detectedType, String obfuscatorGuess, String toolUsed,
                     boolean toolRan, boolean success, boolean outputChanged,
                     File output, String detail) {
        this.detectedType = detectedType == null ? "" : detectedType;
        this.obfuscatorGuess = obfuscatorGuess == null || obfuscatorGuess.trim().isEmpty() ? "none" : obfuscatorGuess;
        this.toolUsed = toolUsed == null ? "" : toolUsed;
        this.toolRan = toolRan;
        this.success = success;
        this.outputChanged = outputChanged;
        this.output = output;
        this.detail = detail == null ? "" : detail;
    }

    public boolean isDeobfuscated() {
        return toolRan && success && outputChanged;
    }

    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Detected type: ").append(detectedType).append('\n');
        sb.append("Obfuscator guess: ").append(obfuscatorGuess).append('\n');
        sb.append("Tool: ").append(toolUsed).append('\n');
        if (!toolRan) {
            sb.append("Result: not run. Not deobfuscated.\n");
        } else if (!success) {
            sb.append("Result: failed. Not deobfuscated.\n");
        } else if (!outputChanged) {
            sb.append("Result: the tool finished, but the output matches the input. Not deobfuscated.\n");
        } else {
            sb.append("Result: success. The output differs from the input and is treated as deobfuscated.\n");
        }
        if (!detail.isEmpty()) {
            sb.append(detail).append('\n');
        }
        if (output != null) {
            sb.append("Output location: ").append(output.getAbsolutePath()).append('\n');
        } else {
            sb.append("Output location: (none)\n");
        }
        return sb.toString();
    }
}
