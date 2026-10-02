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

package com.javadeobfuscator.deobfuscator.ui.universal;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Bytecode text for the Java result view. Frames and debug attributes are
 * skipped so a comparison is about instructions, not recomputed stack maps.
 */
public final class BytecodeDump {
    private static final int MAX_CHARS = 180000;

    private BytecodeDump() {
    }

    public static String dump(File file) throws IOException {
        if (file == null || !file.isFile()) {
            return "(no file)";
        }
        StringBuilder sb = new StringBuilder();
        if (isZip(file)) {
            try (ZipFile zip = new ZipFile(file)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                        continue;
                    }
                    appendClass(sb, entry.getName(), zip.getInputStream(entry));
                    if (sb.length() >= MAX_CHARS) {
                        break;
                    }
                }
            }
        } else {
            try (InputStream in = new FileInputStream(file)) {
                appendClass(sb, file.getName(), in);
            }
        }
        if (sb.length() == 0) {
            return "(no class files)";
        }
        if (sb.length() > MAX_CHARS) {
            return sb.substring(0, MAX_CHARS) + "\n\n[truncated]";
        }
        return sb.toString();
    }

    public static boolean sameBytecode(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private static void appendClass(StringBuilder sb, String name, InputStream in) throws IOException {
        byte[] data = readAll(in);
        sb.append("===== ").append(name).append(" =====\n");
        if (data.length < 4 || (data[0] & 0xFF) != 0xCA || (data[1] & 0xFF) != 0xFE) {
            sb.append("(not a class file, ").append(data.length).append(" bytes)\n");
            return;
        }
        try {
            ClassReader reader = new ClassReader(new ByteArrayInputStream(data));
            StringWriter writer = new StringWriter();
            TraceClassVisitor visitor = new TraceClassVisitor(null, new Textifier(), new PrintWriter(writer));
            reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            sb.append(writer);
        } catch (RuntimeException ex) {
            sb.append("(could not disassemble: ").append(ex.getMessage()).append(")\n");
        }
        sb.append('\n');
    }

    private static boolean isZip(File file) throws IOException {
        byte[] header = new byte[4];
        try (InputStream in = new FileInputStream(file)) {
            int read = in.read(header);
            return read == 4 && header[0] == 0x50 && header[1] == 0x4B;
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    static String normalize(String dump) {
        if (dump == null) {
            return "";
        }
        String[] lines = dump.replace("\r\n", "\n").split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("MAXSTACK") || trimmed.startsWith("MAXLOCALS")) {
                continue;
            }
            sb.append(trimmed).append('\n');
        }
        return sb.toString();
    }
}
