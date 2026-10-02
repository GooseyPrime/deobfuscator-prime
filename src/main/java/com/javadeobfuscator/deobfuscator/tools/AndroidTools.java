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

import com.javadeobfuscator.deobfuscator.detect.DetectedFileType;
import com.javadeobfuscator.deobfuscator.detect.FileTypeDetection;
import com.javadeobfuscator.deobfuscator.detect.FileTypeDetector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Android APK and DEX files via jadx (preferred) or apktool.
 */
public final class AndroidTools implements ExternalDeobfuscator {
    public static final String KEY_JADX = "android.jadx";
    public static final String KEY_APKTOOL = "android.apktool";
    public static final String JADX = "jadx";
    public static final String APKTOOL = "apktool";

    @Override
    public String title() {
        return "Android";
    }

    @Override
    public String description() {
        return "jadx writes Java-like sources. apktool writes Smali and decoded resources. "
                + "A commercial packer is not named unless the tool output says so.";
    }

    @Override
    public List<SettingField> settings() {
        return Arrays.asList(
                new SettingField(KEY_JADX, "jadx executable"),
                new SettingField(KEY_APKTOOL, "apktool executable")
        );
    }

    @Override
    public String installInstructions() {
        return "Android deobfuscation needs jadx or apktool. Neither is bundled.\n\n"
                + "jadx (preferred, Java-like source):\n"
                + "  Download: https://github.com/skylot/jadx/releases\n"
                + "  Debian/Ubuntu: sudo apt install jadx\n"
                + "  Command: jadx -d <output-directory> <apk-or-dex>\n\n"
                + "apktool (Smali):\n"
                + "  Install notes: https://apktool.org/docs/install\n"
                + "  Debian/Ubuntu: sudo apt install apktool\n"
                + "  Command: apktool d -f -o <output-directory> <apk>\n\n"
                + "Paste either executable path above, or put the command on PATH, then click Detect.";
    }

    @Override
    public ResolvedTool resolve(ToolSettings settings) {
        return resolve(settings, null, null);
    }

    @Override
    public ResolvedTool resolve(ToolSettings settings, File input, FileTypeDetection detection) {
        boolean dexInput = detection != null
                ? detection.getType() == DetectedFileType.ANDROID && "Android DEX".equals(detection.getSummary())
                : input != null && "Android DEX".equals(FileTypeDetector.detect(input).getSummary());
        String jadxPath = settings.get(KEY_JADX).trim();
        if (!jadxPath.isEmpty()) {
            return configured(JADX, jadxPath);
        }
        File jadx = ToolLocator.findOnPath("jadx", "jadx.bat", "jadx.cmd");
        String apktoolPath = settings.get(KEY_APKTOOL).trim();
        if (!apktoolPath.isEmpty()) {
            if (dexInput) {
                if (jadx != null) {
                    return ResolvedTool.ready(JADX, jadx, "Found jadx on PATH: " + jadx.getAbsolutePath());
                }
                return ResolvedTool.missing(JADX, "jadx is required to decompile standalone DEX files; apktool only supports APK/JAR packages.");
            }
            return configured(APKTOOL, apktoolPath);
        }
        if (jadx != null) {
            return ResolvedTool.ready(JADX, jadx, "Found jadx on PATH: " + jadx.getAbsolutePath());
        }
        if (dexInput) {
            return ResolvedTool.missing(JADX, "jadx is required to decompile standalone DEX files; apktool only supports APK/JAR packages.");
        }
        File apktool = ToolLocator.findOnPath("apktool", "apktool.bat", "apktool.cmd");
        if (apktool != null) {
            return ResolvedTool.ready(APKTOOL, apktool, "Found apktool on PATH: " + apktool.getAbsolutePath());
        }
        return ResolvedTool.missing(JADX, "jadx and apktool were not found on PATH.\n\n" + installInstructions());
    }

    @Override
    public File outputFor(ResolvedTool tool, File input) {
        return OutputLocations.directoryOutput(input);
    }

    @Override
    public List<String> command(ResolvedTool tool, File input, File output) {
        List<String> command = new ArrayList<String>();
        command.add(tool.getExecutable().getAbsolutePath());
        if (APKTOOL.equals(tool.getToolName())) {
            command.add("d");
            command.add("-f");
            command.add("-o");
            command.add(output.getAbsolutePath());
            command.add(input.getAbsolutePath());
        } else {
            command.add("-d");
            command.add(output.getAbsolutePath());
            command.add(input.getAbsolutePath());
        }
        return command;
    }

    @Override
    public ViewerPages loadViewer(File input, File output, boolean produced) throws IOException {
        String original = input != null && input.isFile() ? TextPreview.describeBinary(input) : "(no input)";
        String result;
        if (!produced || output == null || !output.exists()) {
            result = "No decoded output was written.";
        } else {
            result = TextPreview.readTree(output, ".java", ".smali", ".xml", ".txt");
        }
        return new ViewerPages("Original (binary)", original, "Decoded output", result);
    }

    @Override
    public boolean outputChanged(File input, File output) {
        return TextPreview.directoryHasFiles(output);
    }

    private static ResolvedTool configured(String name, String path) {
        File file = new File(path);
        if (!file.isFile()) {
            return ResolvedTool.missing(name, "Configured " + name + " path does not exist: " + file.getAbsolutePath());
        }
        return ResolvedTool.ready(name, file, "Using configured " + name + ": " + file.getAbsolutePath());
    }
}
