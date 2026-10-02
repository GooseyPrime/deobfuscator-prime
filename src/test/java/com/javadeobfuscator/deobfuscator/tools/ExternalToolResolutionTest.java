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

import com.javadeobfuscator.deobfuscator.detect.FileTypeDetection;
import com.javadeobfuscator.deobfuscator.detect.FileTypeDetector;
import com.javadeobfuscator.deobfuscator.samples.SyntheticFiles;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

public class ExternalToolResolutionTest {
    @Test
    public void pathSearchFindsAnExecutableByName() throws Exception {
        File dir = Files.createTempDirectory("tool-path").toFile();
        File tool = new File(dir, "webcrack");
        Files.write(tool.toPath(), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        if (!ToolLocator.isWindows()) {
            assertTrue(tool.setExecutable(true));
        }
        File found = ToolLocator.searchPath(dir.getAbsolutePath(), "webcrack");
        assertEquals(tool.getAbsolutePath(), found.getAbsolutePath());
        assertEquals(null, ToolLocator.searchPath(dir.getAbsolutePath(), "missing-tool"));
    }

    @Test
    public void pathSearchIgnoresNonExecutableFilesOnPosix() throws Exception {
        assumeFalse(ToolLocator.isWindows());
        File dir = Files.createTempDirectory("tool-path-nonexec").toFile();
        File tool = new File(dir, "jadx");
        Files.write(tool.toPath(), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(tool.setExecutable(false));
        assertEquals(null, ToolLocator.searchPath(dir.getAbsolutePath(), "jadx"));
    }

    @Test
    public void externalToolTimeoutDoesNotWaitForStdoutToClose() throws Exception {
        assumeFalse(ToolLocator.isWindows());
        long start = System.nanoTime();
        try {
            new ExternalToolRunner(100, TimeUnit.MILLISECONDS)
                    .run(java.util.Arrays.asList("sh", "-c", "exec sleep 5"), null);
            fail("Expected timeout");
        } catch (IOException ex) {
            assertTrue(ex.getMessage().contains("Timed out"));
            assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 3);
        }
    }

    @Test
    public void configuredMissingWebcrackIsNotReplacedByPath() throws Exception {
        File settingsFile = File.createTempFile("tools", ".properties");
        ToolSettings settings = new ToolSettings(settingsFile);
        settings.set(JavaScriptTools.KEY_WEBCRACK, new File(settingsFile.getParentFile(), "no-such-webcrack").getAbsolutePath());
        ResolvedTool resolved = new JavaScriptTools().resolve(settings);
        assertEquals(ResolvedTool.Mode.MISSING, resolved.getMode());
        assertTrue(resolved.getMessage().contains("does not exist"));
    }

    @Test
    public void configuredWebcrackBuildsTheExpectedCommand() throws Exception {
        File dir = Files.createTempDirectory("webcrack-cmd").toFile();
        File tool = new File(dir, "webcrack");
        assertTrue(tool.createNewFile());
        File settingsFile = new File(dir, "tools.properties");
        ToolSettings settings = new ToolSettings(settingsFile);
        settings.set(JavaScriptTools.KEY_WEBCRACK, tool.getAbsolutePath());
        JavaScriptTools tools = new JavaScriptTools();
        ResolvedTool resolved = tools.resolve(settings);
        assertEquals(ResolvedTool.Mode.READY, resolved.getMode());
        File input = new File(dir, "in.js");
        File output = new File(dir, "out");
        List<String> command = tools.command(resolved, input, output);
        assertEquals(tool.getAbsolutePath(), command.get(0));
        assertEquals(input.getAbsolutePath(), command.get(1));
        assertEquals("-o", command.get(2));
        assertEquals(output.getAbsolutePath(), command.get(3));
    }

    @Test
    public void missingDe4dotExplainsHowToInstallIt() {
        File settingsFile = new File(System.getProperty("java.io.tmpdir"), "de4dot-missing-" + System.nanoTime() + ".properties");
        ToolSettings settings = new ToolSettings(settingsFile);
        ResolvedTool resolved = new DotNetTools().resolve(settings);
        if (resolved.getMode() != ResolvedTool.Mode.MISSING && ToolLocator.findOnPath("de4dot", "de4dot.exe") != null) {
            return;
        }
        assertEquals(ResolvedTool.Mode.MISSING, resolved.getMode());
        assertTrue(resolved.getMessage().contains("de4dot"));
        assertTrue(new DotNetTools().installInstructions().contains("https://github.com/de4dot/de4dot/releases"));
    }

    @Test
    public void missingAndroidToolsExplainBothInstalls() {
        ResolvedTool resolved = new AndroidTools().resolve(new ToolSettings(new File("target", "no-android-tools-" + System.nanoTime() + ".properties")));
        if (ToolLocator.findOnPath("jadx", "apktool") != null && resolved.canRun()) {
            return;
        }
        assertFalse(resolved.canRun());
        String instructions = new AndroidTools().installInstructions();
        assertTrue(instructions.contains("jadx"));
        assertTrue(instructions.contains("apktool"));
    }

    @Test
    public void jadxAndApktoolCommandsDiffer() throws Exception {
        File dir = Files.createTempDirectory("android-cmd").toFile();
        File jadx = new File(dir, "jadx");
        assertTrue(jadx.createNewFile());
        AndroidTools tools = new AndroidTools();
        ResolvedTool resolved = ResolvedTool.ready(AndroidTools.JADX, jadx, "test");
        File input = new File(dir, "app.apk");
        File output = new File(dir, "decoded");
        List<String> command = tools.command(resolved, input, output);
        assertEquals("-d", command.get(1));
        assertEquals(output.getAbsolutePath(), command.get(2));

        ResolvedTool apktool = ResolvedTool.ready(AndroidTools.APKTOOL, jadx, "test");
        List<String> apktoolCommand = tools.command(apktool, input, output);
        assertEquals("d", apktoolCommand.get(1));
        assertEquals("-f", apktoolCommand.get(2));
        assertEquals("-o", apktoolCommand.get(3));
    }

    @Test
    public void apktoolIsNotReadyForStandaloneDex() throws Exception {
        File dir = Files.createTempDirectory("android-dex-tools").toFile();
        File apktool = new File(dir, "apktool");
        assertTrue(apktool.createNewFile());
        ToolSettings settings = new ToolSettings(new File(dir, "tools.properties"));
        settings.set(AndroidTools.KEY_APKTOOL, apktool.getAbsolutePath());
        FileTypeDetection dex = FileTypeDetector.detect(SyntheticFiles.dexMagic(), "classes.dex");
        ResolvedTool resolved = new AndroidTools().resolve(settings, null, dex);
        assertEquals(ResolvedTool.Mode.MISSING, resolved.getMode());
        assertTrue(resolved.getMessage().contains("jadx is required"));
    }

    @Test
    public void javascriptChangeDetectionComparesBeyondThePreviewLimit() throws Exception {
        File dir = Files.createTempDirectory("javascript-full-compare").toFile();
        File input = new File(dir, "input.js");
        File output = new File(dir, "output.js");
        byte[] original = new byte[2_000_001];
        byte[] changed = new byte[2_000_001];
        original[original.length - 1] = 'a';
        changed[changed.length - 1] = 'b';
        Files.write(input.toPath(), original);
        Files.write(output.toPath(), changed);
        assertTrue(new JavaScriptTools().outputChanged(input, output));
    }
}
