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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * .NET assemblies via de4dot. A Windows .exe build is launched with mono when
 * this process is not running on Windows.
 */
public final class DotNetTools implements ExternalDeobfuscator {
    public static final String KEY_DE4DOT = "dotnet.de4dot";
    public static final String DE4DOT = "de4dot";

    @Override
    public String title() {
        return ".NET";
    }

    @Override
    public String description() {
        return "de4dot cleans common .NET obfuscators (Confuser, Dotfuscator, .NET Reactor, and others) "
                + "when those protections are actually present. This tab does not guess a protector until de4dot reports one.";
    }

    @Override
    public List<SettingField> settings() {
        return Collections.singletonList(new SettingField(KEY_DE4DOT, "de4dot executable"));
    }

    @Override
    public String installInstructions() {
        return "de4dot is required and is not bundled.\n\n"
                + "1. Download a release from https://github.com/de4dot/de4dot/releases\n"
                + "2. Put de4dot or de4dot.exe on PATH, or paste the path above and click Detect.\n"
                + "3. On Linux and macOS, de4dot.exe also needs Mono (the mono command on PATH).\n\n"
                + "Command used here:\n"
                + "  de4dot <assembly> -o <output>";
    }

    @Override
    public ResolvedTool resolve(ToolSettings settings) {
        String configured = settings.get(KEY_DE4DOT).trim();
        File executable;
        if (!configured.isEmpty()) {
            executable = new File(configured);
            if (!executable.isFile()) {
                return ResolvedTool.missing(DE4DOT, "Configured de4dot path does not exist: " + executable.getAbsolutePath()
                        + "\n\n" + installInstructions());
            }
        } else {
            executable = ToolLocator.findOnPath("de4dot", "de4dot.exe", "de4dot.bat");
            if (executable == null) {
                return ResolvedTool.missing(DE4DOT, "de4dot was not found on PATH.\n\n" + installInstructions());
            }
        }
        if (needsMono(executable)) {
            File mono = ToolLocator.findOnPath("mono");
            if (mono == null) {
                return ResolvedTool.missing(DE4DOT,
                        "Found " + executable.getAbsolutePath() + " but it is a Windows executable and mono is not on PATH.\n\n"
                                + installInstructions());
            }
        }
        return ResolvedTool.ready(DE4DOT, executable, "Using de4dot: " + executable.getAbsolutePath());
    }

    @Override
    public File outputFor(ResolvedTool tool, File input) {
        String name = input.getName().toLowerCase(Locale.ROOT);
        String extension = ".bin";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            extension = input.getName().substring(dot);
        }
        return OutputLocations.fileOutput(input, extension);
    }

    @Override
    public List<String> command(ResolvedTool tool, File input, File output) {
        List<String> command = new ArrayList<String>();
        File executable = tool.getExecutable();
        if (needsMono(executable)) {
            File mono = ToolLocator.findOnPath("mono");
            if (mono == null) {
                throw new IllegalStateException("mono is not on PATH.");
            }
            command.add(mono.getAbsolutePath());
        }
        command.add(executable.getAbsolutePath());
        command.add(input.getAbsolutePath());
        command.add("-o");
        command.add(output.getAbsolutePath());
        return command;
    }

    @Override
    public ViewerPages loadViewer(File input, File output, boolean produced) throws IOException {
        String original = input != null && input.isFile()
                ? TextPreview.describeBinary(input) + "\n\nPrintable strings:\n" + TextPreview.printableStrings(input)
                : "(no input)";
        String result;
        if (!produced || output == null || !output.isFile()) {
            result = "No assembly was written.";
        } else {
            result = TextPreview.describeBinary(output) + "\n\nPrintable strings:\n" + TextPreview.printableStrings(output);
        }
        return new ViewerPages("Original assembly", original, "de4dot output", result);
    }

    @Override
    public boolean outputChanged(File input, File output) throws IOException {
        return TextPreview.bytesDiffer(input, output);
    }

    static boolean needsMono(File executable) {
        return executable != null
                && executable.getName().toLowerCase(Locale.ROOT).endsWith(".exe")
                && !ToolLocator.isWindows();
    }
}
