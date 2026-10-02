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
 * File kinds the universal front door can route.
 * Detection uses magic bytes first and the filename extension second.
 */
public enum DetectedFileType {
    JAVA("Java"),
    JAVASCRIPT("JavaScript"),
    DOTNET(".NET"),
    ANDROID("Android"),
    UNKNOWN("Unknown");

    private final String displayName;

    DetectedFileType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
