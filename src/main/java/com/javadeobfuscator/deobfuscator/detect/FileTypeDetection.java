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

package com.javadeobfuscator.deobfuscator.detect;

/**
 * Result of identifying a file. {@code summary} is a short label;
 * {@code detail} explains which bytes or extension led to that label.
 */
public final class FileTypeDetection {
    private final DetectedFileType type;
    private final String summary;
    private final String detail;

    public FileTypeDetection(DetectedFileType type, String summary, String detail) {
        this.type = type;
        this.summary = summary;
        this.detail = detail;
    }

    public DetectedFileType getType() {
        return type;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetail() {
        return detail;
    }

    public String explain() {
        return summary + "\n" + detail;
    }
}
