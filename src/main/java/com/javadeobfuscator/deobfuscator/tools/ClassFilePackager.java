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

import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The Java engine reads ZIP archives. A single .class dropped on the front
 * door is packed into a new jar under {@code out/} and the original file is left untouched.
 */
public final class ClassFilePackager {
    private ClassFilePackager() {
    }

    public static boolean looksLikeClass(File file) throws IOException {
        byte[] header = new byte[4];
        try (FileInputStream in = new FileInputStream(file)) {
            int read = in.read(header);
            return read == 4
                    && (header[0] & 0xFF) == 0xCA
                    && (header[1] & 0xFF) == 0xFE
                    && (header[2] & 0xFF) == 0xBA
                    && (header[3] & 0xFF) == 0xBE;
        }
    }

    public static File packageAsJar(File classFile) throws IOException {
        byte[] data = readAll(classFile);
        ClassReader reader = new ClassReader(data);
        String entryName = reader.getClassName() + ".class";
        File jar = OutputLocations.fileOutput(classFile, ".jar");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jar))) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(data);
            zip.closeEntry();
        }
        return jar;
    }

    private static byte[] readAll(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            int offset = 0;
            while (offset < data.length) {
                int read = in.read(data, offset, data.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
        }
        return data;
    }
}
