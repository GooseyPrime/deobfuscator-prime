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

package com.javadeobfuscator.deobfuscator.samples;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Tiny synthetic payloads for detector tests and the made-up samples folder.
 */
public final class SyntheticFiles {
    private SyntheticFiles() {
    }

    public static byte[] classMagic() {
        return new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0x00, 0x00, 0x00, 0x34};
    }

    public static byte[] dexMagic() {
        return new byte[]{'d', 'e', 'x', '\n', '0', '3', '5', 0};
    }

    public static byte[] zipWith(String entryName, byte[] content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(entryName));
            if (content != null) {
                zip.write(content);
            }
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    public static byte[] pe(boolean clr) {
        byte[] data = new byte[512];
        data[0] = 0x4D;
        data[1] = 0x5A;
        writeInt(data, 0x3C, 0x80);
        data[0x80] = 'P';
        data[0x81] = 'E';
        writeShort(data, 0x84, 0x14C);
        writeShort(data, 0x86, 1);
        writeShort(data, 0x94, 224);
        writeShort(data, 0x96, 0x0102);
        int optional = 0x98;
        writeShort(data, optional, 0x10B);
        writeInt(data, optional + 92, 16);
        if (clr) {
            writeInt(data, optional + 96 + (14 * 8), 0x2000);
            writeInt(data, optional + 96 + (14 * 8) + 4, 0x48);
        }
        return data;
    }

    private static void writeShort(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private static void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }
}
