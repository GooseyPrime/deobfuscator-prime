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

import com.javadeobfuscator.deobfuscator.samples.SyntheticFiles;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;

public class FileTypeDetectorTest {
    @Test
    public void classMagicIsJavaEvenWithAForeignExtension() {
        FileTypeDetection detection = FileTypeDetector.detect(SyntheticFiles.classMagic(), "payload.js");
        assertEquals(DetectedFileType.JAVA, detection.getType());
    }

    @Test
    public void dexMagicIsAndroid() {
        FileTypeDetection detection = FileTypeDetector.detect(SyntheticFiles.dexMagic(), "classes.bin");
        assertEquals(DetectedFileType.ANDROID, detection.getType());
    }

    @Test
    public void zipOfClassFilesIsJava() throws Exception {
        byte[] jar = SyntheticFiles.zipWith("sample/MadeUp.class", SyntheticFiles.classMagic());
        FileTypeDetection detection = FileTypeDetector.detect(jar, "renamed.bin");
        assertEquals(DetectedFileType.JAVA, detection.getType());
    }

    @Test
    public void warExtensionSelectsJavaWhenTheZipHasNoClasses() throws Exception {
        byte[] war = SyntheticFiles.zipWith("WEB-INF/web.xml", "<web/>".getBytes(StandardCharsets.UTF_8));
        FileTypeDetection detection = FileTypeDetector.detect(war, "app.war");
        assertEquals(DetectedFileType.JAVA, detection.getType());
    }

    @Test
    public void androidManifestSelectsAndroidAheadOfJarExtension() throws Exception {
        byte[] apk = SyntheticFiles.zipWith("AndroidManifest.xml", "<manifest/>".getBytes(StandardCharsets.UTF_8));
        FileTypeDetection detection = FileTypeDetector.detect(apk, "misnamed.jar");
        assertEquals(DetectedFileType.ANDROID, detection.getType());
    }

    @Test
    public void classesDexInsideZipIsAndroid() throws Exception {
        byte[] apk = SyntheticFiles.zipWith("classes.dex", SyntheticFiles.dexMagic());
        FileTypeDetection detection = FileTypeDetector.detect(apk, "app.apk");
        assertEquals(DetectedFileType.ANDROID, detection.getType());
    }

    @Test
    public void nestedAndroidEntriesDoNotRouteAJavaArchiveToAndroid() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            addEntry(zip, "docs/AndroidManifest.xml", "<manifest/>".getBytes(StandardCharsets.UTF_8));
            addEntry(zip, "assets/classes.dex", SyntheticFiles.dexMagic());
            addEntry(zip, "sample/MadeUp.class", SyntheticFiles.classMagic());
        }
        assertEquals(DetectedFileType.JAVA, FileTypeDetector.detect(bytes.toByteArray(), "library.jar").getType());
    }

    @Test
    public void javascriptExtensionIsUsedWhenMagicIsAbsent() {
        byte[] script = "function a(){return 1;}".getBytes(StandardCharsets.UTF_8);
        assertEquals(DetectedFileType.JAVASCRIPT, FileTypeDetector.detect(script, "app.js").getType());
        assertEquals(DetectedFileType.JAVASCRIPT, FileTypeDetector.detect(script, "app.mjs").getType());
    }

    @Test
    public void clrPeIsDotNetAndNativePeIsUnknown() {
        assertEquals(DetectedFileType.DOTNET, FileTypeDetector.detect(SyntheticFiles.pe(true), "tool.exe").getType());
        FileTypeDetection nativePe = FileTypeDetector.detect(SyntheticFiles.pe(false), "tool.exe");
        assertEquals(DetectedFileType.UNKNOWN, nativePe.getType());
    }

    @Test
    public void randomBytesAreUnknownAndExplainThemselves() {
        FileTypeDetection detection = FileTypeDetector.detect(new byte[]{0x00, 0x01, 0x02, 0x03}, "notes.bin");
        assertEquals(DetectedFileType.UNKNOWN, detection.getType());
        org.junit.Assert.assertTrue(detection.getDetail().contains("00 01 02 03"));
    }

    @Test
    public void javaExtensionWithoutMagicIsNotSilentlyTreatedAsJava() {
        FileTypeDetection detection = FileTypeDetector.detect("not a class".getBytes(StandardCharsets.UTF_8), "Fake.class");
        assertEquals(DetectedFileType.UNKNOWN, detection.getType());
        org.junit.Assert.assertTrue(detection.getDetail().toLowerCase().contains("java"));
    }

    @Test
    public void emptyFileIsUnknown() {
        assertEquals(DetectedFileType.UNKNOWN, FileTypeDetector.detect(new byte[0], "empty.dat").getType());
    }

    private static void addEntry(ZipOutputStream zip, String name, byte[] data) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }
}
