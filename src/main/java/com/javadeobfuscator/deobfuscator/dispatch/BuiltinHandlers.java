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

package com.javadeobfuscator.deobfuscator.dispatch;

import com.javadeobfuscator.deobfuscator.detect.DetectedFileType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BuiltinHandlers {
    private BuiltinHandlers() {
    }

    public static List<TypeHandler> all() {
        return Collections.unmodifiableList(Arrays.asList(
                javaHandler(),
                javaScriptHandler(),
                dotNetHandler(),
                androidHandler(),
                unknownHandler()
        ));
    }

    public static TypeHandler javaHandler() {
        return handler(DetectedFileType.JAVA, "Java",
                "Built-in Java bytecode deobfuscator. Detect obfuscators, choose transformers, and view the result.");
    }

    public static TypeHandler javaScriptHandler() {
        return handler(DetectedFileType.JAVASCRIPT, "JavaScript",
                "Runs webcrack or synchrony when Node.js can invoke one of them. Neither tool is bundled.");
    }

    public static TypeHandler dotNetHandler() {
        return handler(DetectedFileType.DOTNET, ".NET",
                "Runs de4dot when it is installed. de4dot is not bundled.");
    }

    public static TypeHandler androidHandler() {
        return handler(DetectedFileType.ANDROID, "Android",
                "Runs jadx or apktool when one of them is installed. Neither tool is bundled.");
    }

    public static TypeHandler unknownHandler() {
        return handler(DetectedFileType.UNKNOWN, "Unknown",
                "The file type was not recognized. It is reported here and is not deobfuscated.");
    }

    private static TypeHandler handler(final DetectedFileType type, final String title, final String description) {
        return new TypeHandler() {
            @Override
            public DetectedFileType type() {
                return type;
            }

            @Override
            public String tabTitle() {
                return title;
            }

            @Override
            public String description() {
                return description;
            }
        };
    }
}
