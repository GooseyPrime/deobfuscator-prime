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

import com.javadeobfuscator.deobfuscator.detect.FileTypeDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class TextPreview {
    public static final int MAX_CHARS = 200000;
    private static final int MAX_BYTES = 2_000_000;

    private TextPreview() {
    }

    public static String readText(File file) throws IOException {
        byte[] data = readLimited(file, MAX_BYTES);
        String text = new String(data, StandardCharsets.UTF_8);
        if (text.length() > MAX_CHARS) {
            return text.substring(0, MAX_CHARS) + "\n\n[truncated]";
        }
        return text;
    }

    public static String printableStrings(File file) throws IOException {
        byte[] data = readLimited(file, MAX_BYTES);
        StringBuilder current = new StringBuilder();
        StringBuilder all = new StringBuilder();
        for (byte value : data) {
            int c = value & 0xFF;
            if (c >= 32 && c < 127) {
                current.append((char) c);
            } else {
                flushString(current, all);
            }
            if (all.length() >= MAX_CHARS) {
                break;
            }
        }
        flushString(current, all);
        if (all.length() == 0) {
            return "(no printable strings in the first " + data.length + " bytes)\nMagic: " + magicOf(file);
        }
        if (all.length() > MAX_CHARS) {
            return all.substring(0, MAX_CHARS) + "\n\n[truncated]";
        }
        return all.toString();
    }

    public static String describeBinary(File file) throws IOException {
        byte[] prefix = readLimited(file, 64);
        return "Binary file\nSize: " + file.length() + " bytes\nMagic: " + FileTypeDetector.hexPrefix(prefix, 16);
    }

    public static boolean textDiffers(String left, String right) {
        return !normalize(left).equals(normalize(right));
    }

    public static boolean bytesDiffer(File left, File right) throws IOException {
        if (left == null || right == null || !left.isFile() || !right.isFile()) {
            return false;
        }
        if (left.length() != right.length()) {
            return true;
        }
        return !Arrays.equals(digest(left), digest(right));
    }

    public static boolean directoryHasFiles(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        return countFiles(dir) > 0;
    }

    public static int countFiles(File dir) {
        File[] children = dir.listFiles();
        if (children == null) {
            return 0;
        }
        int count = 0;
        for (File child : children) {
            if (child.isDirectory()) {
                count += countFiles(child);
            } else if (child.isFile()) {
                count++;
            }
        }
        return count;
    }

    public static File primaryTextFile(File output, String... extensions) {
        if (output == null) {
            return null;
        }
        if (output.isFile()) {
            return output;
        }
        if (!output.isDirectory()) {
            return null;
        }
        List<File> matches = new ArrayList<File>();
        collect(output, matches, extensions, 0);
        File preferred = null;
        for (File match : matches) {
            String name = match.getName().toLowerCase(Locale.ROOT);
            if (name.contains("deobfuscated")) {
                return match;
            }
            if (preferred == null) {
                preferred = match;
            }
        }
        return preferred;
    }

    public static String readTree(File output, String... extensions) throws IOException {
        if (output == null || !output.exists()) {
            return "(no output)";
        }
        if (output.isFile()) {
            return readText(output);
        }
        List<File> matches = new ArrayList<File>();
        collect(output, matches, extensions, 0);
        if (matches.isEmpty()) {
            return "Output directory has " + countFiles(output) + " file(s) and none with extensions "
                    + Arrays.toString(extensions) + ".";
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (File match : matches) {
            if (shown >= 8 || sb.length() >= MAX_CHARS) {
                sb.append("\n[further files omitted]\n");
                break;
            }
            sb.append("----- ").append(match.getAbsolutePath()).append(" -----\n");
            sb.append(readText(match)).append('\n');
            shown++;
        }
        return sb.toString();
    }

    private static void collect(File dir, List<File> matches, String[] extensions, int depth) {
        if (depth > 6) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children);
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, matches, extensions, depth + 1);
            } else if (child.isFile() && hasExtension(child.getName(), extensions)) {
                matches.add(child);
            }
        }
    }

    private static boolean hasExtension(String name, String[] extensions) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (lower.endsWith(extension.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static void flushString(StringBuilder current, StringBuilder all) {
        if (current.length() >= 4) {
            all.append(current).append('\n');
        }
        current.setLength(0);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n").trim();
    }

    private static byte[] digest(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 is unavailable", ex);
        }
    }

    private static byte[] readLimited(File file, int max) throws IOException {
        int size = (int) Math.min(file.length(), max);
        byte[] data = new byte[size];
        try (FileInputStream in = new FileInputStream(file)) {
            int offset = 0;
            while (offset < size) {
                int read = in.read(data, offset, size - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            if (offset == data.length) {
                return data;
            }
            byte[] trimmed = new byte[offset];
            System.arraycopy(data, 0, trimmed, 0, offset);
            return trimmed;
        }
    }

    private static String magicOf(File file) throws IOException {
        return FileTypeDetector.hexPrefix(readLimited(file, 16), 16);
    }
}
