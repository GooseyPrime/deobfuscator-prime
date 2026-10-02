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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JavaScript via webcrack (preferred) or synchrony. npx is offered only as a
 * manual run because it may download a package.
 */
public final class JavaScriptTools implements ExternalDeobfuscator {
    public static final String KEY_WEBCRACK = "javascript.webcrack";
    public static final String KEY_SYNCHRONY = "javascript.synchrony";
    public static final String WEBCRACK = "webcrack";
    public static final String SYNCHRONY = "synchrony";
    public static final String NPX_WEBCRACK = "npx webcrack";

    @Override
    public String title() {
        return "JavaScript";
    }

    @Override
    public String description() {
        return "webcrack reads the script and writes a directory. synchrony writes a single script. "
                + "Obfuscator families such as javascript-obfuscator are a guess only after a tool reports them.";
    }

    @Override
    public List<SettingField> settings() {
        return Arrays.asList(
                new SettingField(KEY_WEBCRACK, "webcrack executable"),
                new SettingField(KEY_SYNCHRONY, "synchrony executable")
        );
    }

    @Override
    public String installInstructions() {
        return "JavaScript deobfuscation needs an external tool. None is bundled.\n\n"
                + "webcrack (preferred):\n"
                + "  1. Install Node.js from https://nodejs.org/\n"
                + "  2. npm install -g webcrack\n"
                + "  The command is: webcrack <file.js> -o <output-directory>\n\n"
                + "synchrony (alternative):\n"
                + "  npm install -g synchrony\n"
                + "  The command is: synchrony <file.js> -o <output.js>\n\n"
                + "If Node.js is installed but neither tool is global, npx can launch webcrack:\n"
                + "  npx --yes webcrack <file.js> -o <output-directory>\n"
                + "That download is not started automatically when a file is dropped.";
    }

    @Override
    public ResolvedTool resolve(ToolSettings settings) {
        String webcrackPath = settings.get(KEY_WEBCRACK).trim();
        if (!webcrackPath.isEmpty()) {
            return configured(WEBCRACK, webcrackPath);
        }
        String synchronyPath = settings.get(KEY_SYNCHRONY).trim();
        if (!synchronyPath.isEmpty()) {
            return configured(SYNCHRONY, synchronyPath);
        }
        File webcrack = ToolLocator.findOnPath("webcrack", "webcrack.cmd");
        if (webcrack != null) {
            return ResolvedTool.ready(WEBCRACK, webcrack, "Found webcrack on PATH: " + webcrack.getAbsolutePath());
        }
        File synchrony = ToolLocator.findOnPath("synchrony", "synchrony.cmd");
        if (synchrony != null) {
            return ResolvedTool.ready(SYNCHRONY, synchrony, "Found synchrony on PATH: " + synchrony.getAbsolutePath());
        }
        File npx = ToolLocator.findOnPath("npx", "npx.cmd");
        if (npx != null) {
            return ResolvedTool.manual(NPX_WEBCRACK, npx,
                    "webcrack and synchrony are not installed. npx is at " + npx.getAbsolutePath()
                            + ". Click Run to execute npx --yes webcrack. That may download webcrack. Nothing has been deobfuscated yet.");
        }
        return ResolvedTool.missing(WEBCRACK,
                "webcrack, synchrony, and npx were not found on PATH.\n\n" + installInstructions());
    }

    @Override
    public File outputFor(ResolvedTool tool, File input) {
        if (SYNCHRONY.equals(tool.getToolName())) {
            return OutputLocations.fileOutput(input, ".js");
        }
        return OutputLocations.directoryOutput(input);
    }

    @Override
    public List<String> command(ResolvedTool tool, File input, File output) {
        if (NPX_WEBCRACK.equals(tool.getToolName())) {
            return Arrays.asList(
                    tool.getExecutable().getAbsolutePath(),
                    "--yes",
                    "webcrack",
                    input.getAbsolutePath(),
                    "-o",
                    output.getAbsolutePath()
            );
        }
        return Arrays.asList(
                tool.getExecutable().getAbsolutePath(),
                input.getAbsolutePath(),
                "-o",
                output.getAbsolutePath()
        );
    }

    @Override
    public ViewerPages loadViewer(File input, File output, boolean produced) throws IOException {
        String original = input != null && input.isFile() ? TextPreview.readText(input) : "(no input)";
        String result;
        if (!produced || output == null || !output.exists()) {
            result = "No JavaScript output was written.";
        } else if (output.isDirectory()) {
            result = TextPreview.readTree(output, ".js", ".mjs", ".json", ".txt");
        } else {
            result = TextPreview.readText(output);
        }
        return new ViewerPages("Original", original, "Tool output", result);
    }

    @Override
    public boolean outputChanged(File input, File output) throws IOException {
        if (output == null || !output.exists() || input == null || !input.isFile()) {
            return false;
        }
        if (output.isDirectory()) {
            if (!TextPreview.directoryHasFiles(output)) {
                return false;
            }
            File primary = TextPreview.primaryTextFile(output, ".js", ".mjs");
            if (primary == null) {
                return true;
            }
            return TextPreview.bytesDiffer(input, primary);
        }
        return TextPreview.bytesDiffer(input, output);
    }

    private static ResolvedTool configured(String name, String path) {
        File file = new File(path);
        if (!file.isFile()) {
            return ResolvedTool.missing(name, "Configured " + name + " path does not exist: " + file.getAbsolutePath());
        }
        return ResolvedTool.ready(name, file, "Using configured " + name + ": " + file.getAbsolutePath());
    }

    public static List<String> empty() {
        return Collections.emptyList();
    }
}
