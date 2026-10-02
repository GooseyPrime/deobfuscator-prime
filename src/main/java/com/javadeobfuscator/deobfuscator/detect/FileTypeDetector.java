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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Identifies Java, JavaScript, .NET, and Android inputs.
 * Magic bytes win over the filename. The extension is used only when the
 * bytes are ambiguous (a ZIP container) or when no recognized magic is present.
 */
public final class FileTypeDetector {
    private static final int PREFIX_LIMIT = 1024 * 1024;
    private static final int MAX_ZIP_ENTRIES = 500;

    private enum ZipKind {
        ANDROID,
        JAVA,
        NEITHER,
        UNREADABLE
    }

    private FileTypeDetector() {
    }

    public static FileTypeDetection detect(File file) {
        if (file == null) {
            return unknown("No file", "No file was provided.");
        }
        if (!file.exists() || !file.isFile()) {
            return unknown("Unreadable file",
                    "The path does not exist or is not a file: " + file.getAbsolutePath());
        }
        byte[] prefix;
        try {
            prefix = readPrefix(file, PREFIX_LIMIT);
        } catch (IOException ex) {
            return unknown("Unreadable file", "Could not read " + file.getAbsolutePath() + ": " + ex.getMessage());
        }
        return detectBytes(prefix, file.getName(), file);
    }

    /**
     * Detect from an in-memory payload. ZIP classification reads the bytes
     * themselves, so tests can pass a complete small archive.
     */
    public static FileTypeDetection detect(byte[] data, String fileName) {
        if (data == null) {
            data = new byte[0];
        }
        return detectBytes(data, fileName == null ? "" : fileName, null);
    }

    private static FileTypeDetection detectBytes(byte[] data, String fileName, File zipSource) {
        String extension = extensionOf(fileName);
        String magic = hexPrefix(data, 8);

        if (startsWith(data, 0xCA, 0xFE, 0xBA, 0xBE)) {
            return new FileTypeDetection(DetectedFileType.JAVA,
                    "Java class file",
                    "Magic bytes CA FE BA BE. Extension '" + extensionOrNone(extension) + "' was not required.");
        }
        if (startsWith(data, 0x64, 0x65, 0x78, 0x0A)) {
            return new FileTypeDetection(DetectedFileType.ANDROID,
                    "Android DEX",
                    "Magic bytes 64 65 78 0A (dex). Extension '" + extensionOrNone(extension) + "' was not required.");
        }
        if (startsWith(data, 0x4D, 0x5A)) {
            if (hasClrDirectory(data)) {
                return new FileTypeDetection(DetectedFileType.DOTNET,
                        ".NET assembly",
                        "PE magic 4D 5A and a non-empty CLR data directory (index 14).");
            }
            return unknown("Native or non-CLR PE",
                    "Magic bytes 4D 5A (PE/MZ) but the CLR runtime directory is missing or empty. "
                            + "This is not routed as a .NET assembly. Extension '" + extensionOrNone(extension) + "'.");
        }
        if (isZipMagic(data)) {
            return classifyZip(data, extension, magic, zipSource);
        }

        if ("js".equals(extension) || "mjs".equals(extension)) {
            return new FileTypeDetection(DetectedFileType.JAVASCRIPT,
                    "JavaScript",
                    "No Java, Android, or .NET magic bytes. Extension ." + extension + " selects JavaScript. Magic prefix: " + magic + ".");
        }
        if ("jar".equals(extension) || "war".equals(extension) || "class".equals(extension)) {
            return unknown("Extension looks like Java, bytes do not",
                    "Extension ." + extension + " suggests Java, but the file is neither a class (CA FE BA BE) nor a ZIP archive. Magic prefix: " + magic + ".");
        }
        if ("apk".equals(extension)) {
            return unknown("Extension looks like Android, bytes do not",
                    "Extension .apk suggests Android, but the file is neither DEX nor a ZIP archive. Magic prefix: " + magic + ".");
        }
        if ("exe".equals(extension) || "dll".equals(extension)) {
            return unknown("Extension looks like a Windows binary, bytes do not",
                    "Extension ." + extension + " suggests a PE file, but the magic bytes are not 4D 5A. Magic prefix: " + magic + ".");
        }
        return unknown("Unrecognized file",
                "No recognized magic bytes and extension '" + extensionOrNone(extension)
                        + "' is not Java, JavaScript, .NET, or Android. Magic prefix: " + magic + ".");
    }

    private static FileTypeDetection classifyZip(byte[] data, String extension, String magic, File zipSource) {
        ZipKind kind = zipSource != null ? classifyZipFile(zipSource) : classifyZipBytes(data);
        if (kind == ZipKind.UNREADABLE) {
            if ("apk".equals(extension)) {
                return new FileTypeDetection(DetectedFileType.ANDROID,
                        "Android package (extension)",
                        "ZIP magic " + magic + " and extension .apk. The archive entries could not be listed, so the extension was used.");
            }
            if ("jar".equals(extension) || "war".equals(extension)) {
                return new FileTypeDetection(DetectedFileType.JAVA,
                        "Java archive (extension)",
                        "ZIP magic " + magic + " and extension ." + extension + ". The archive entries could not be listed, so the extension was used.");
            }
            return unknown("Unreadable ZIP",
                    "ZIP magic " + magic + " but the archive entries could not be read, and extension '"
                            + extensionOrNone(extension) + "' does not identify Java or Android.");
        }
        if (kind == ZipKind.ANDROID) {
            return new FileTypeDetection(DetectedFileType.ANDROID,
                    "Android package",
                    "ZIP magic and an Android entry (AndroidManifest.xml or classes.dex). Extension '"
                            + extensionOrNone(extension) + "'.");
        }
        if (kind == ZipKind.JAVA) {
            return new FileTypeDetection(DetectedFileType.JAVA,
                    "Java archive",
                    "ZIP magic and at least one .class entry. Extension '" + extensionOrNone(extension) + "'.");
        }
        if ("apk".equals(extension)) {
            return new FileTypeDetection(DetectedFileType.ANDROID,
                    "Android package (extension)",
                    "ZIP magic with no classes.dex, AndroidManifest.xml, or .class entries. Extension .apk selects Android.");
        }
        if ("jar".equals(extension) || "war".equals(extension)) {
            return new FileTypeDetection(DetectedFileType.JAVA,
                    "Java archive (extension)",
                    "ZIP magic with no .class entries yet. Extension ." + extension + " selects Java.");
        }
        return unknown("Unrecognized ZIP",
                "ZIP magic " + magic + " but no Java classes or Android entries were found, and extension '"
                        + extensionOrNone(extension) + "' is not .jar, .war, or .apk.");
    }

    private static ZipKind classifyZipFile(File file) {
        try (ZipFile zip = new ZipFile(file)) {
            return classifyEntries(zip.entries());
        } catch (IOException ex) {
            return ZipKind.UNREADABLE;
        }
    }

    private static ZipKind classifyZipBytes(byte[] data) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
            boolean android = false;
            boolean java = false;
            ZipEntry entry;
            int seen = 0;
            while ((entry = zis.getNextEntry()) != null && seen < MAX_ZIP_ENTRIES) {
                seen++;
                String name = entry.getName();
                if (isAndroidEntry(name)) {
                    android = true;
                    break;
                }
                if (name.endsWith(".class")) {
                    java = true;
                }
                zis.closeEntry();
            }
            if (android) {
                return ZipKind.ANDROID;
            }
            if (java) {
                return ZipKind.JAVA;
            }
            return ZipKind.NEITHER;
        } catch (IOException ex) {
            return ZipKind.UNREADABLE;
        }
    }

    private static ZipKind classifyEntries(Enumeration<? extends ZipEntry> entries) {
        boolean android = false;
        boolean java = false;
        int seen = 0;
        while (entries.hasMoreElements() && seen < MAX_ZIP_ENTRIES) {
            seen++;
            String name = entries.nextElement().getName();
            if (isAndroidEntry(name)) {
                android = true;
                break;
            }
            if (name.endsWith(".class")) {
                java = true;
            }
        }
        if (android) {
            return ZipKind.ANDROID;
        }
        if (java) {
            return ZipKind.JAVA;
        }
        return ZipKind.NEITHER;
    }

    static boolean isAndroidEntry(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String base = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        if ("AndroidManifest.xml".equals(base)) {
            return true;
        }
        if ("classes.dex".equals(base)) {
            return true;
        }
        return base.startsWith("classes") && base.endsWith(".dex") && base.length() > "classes.dex".length();
    }

    /**
     * True when a PE image has a CLR runtime header with a non-zero size.
     * Layout: DOS e_lfanew, PE signature, COFF, optional header data directory 14.
     */
    static boolean hasClrDirectory(byte[] data) {
        if (data == null || data.length < 0x40 || !startsWith(data, 0x4D, 0x5A)) {
            return false;
        }
        int pe = readLeInt(data, 0x3C);
        if (pe < 0 || pe > data.length - 24) {
            return false;
        }
        if ((data[pe] & 0xFF) != 'P' || (data[pe + 1] & 0xFF) != 'E' || data[pe + 2] != 0 || data[pe + 3] != 0) {
            return false;
        }
        int sizeOpt = readLeShort(data, pe + 20);
        int optional = pe + 24;
        if (sizeOpt < 96 || optional < 0 || optional > data.length - 2) {
            return false;
        }
        if (optional + sizeOpt > data.length) {
            return false;
        }
        int magic = readLeShort(data, optional);
        int numberOffset;
        int directoryOffset;
        if (magic == 0x10B) {
            numberOffset = 92;
            directoryOffset = 96;
        } else if (magic == 0x20B) {
            numberOffset = 108;
            directoryOffset = 112;
        } else {
            return false;
        }
        if (numberOffset + 4 > sizeOpt) {
            return false;
        }
        int directories = readLeInt(data, optional + numberOffset);
        if (directories <= 14) {
            return false;
        }
        int clr = directoryOffset + (14 * 8);
        if (clr + 8 > sizeOpt) {
            return false;
        }
        int rva = readLeInt(data, optional + clr);
        int size = readLeInt(data, optional + clr + 4);
        return rva != 0 && size != 0;
    }

    private static boolean isZipMagic(byte[] data) {
        return startsWith(data, 0x50, 0x4B, 0x03, 0x04)
                || startsWith(data, 0x50, 0x4B, 0x05, 0x06)
                || startsWith(data, 0x50, 0x4B, 0x07, 0x08);
    }

    private static boolean startsWith(byte[] data, int... magic) {
        if (data == null || data.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if ((data[i] & 0xFF) != magic[i]) {
                return false;
            }
        }
        return true;
    }

    static int readLeInt(byte[] data, int offset) {
        if (offset < 0 || offset + 4 > data.length) {
            return -1;
        }
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    static int readLeShort(byte[] data, int offset) {
        if (offset < 0 || offset + 2 > data.length) {
            return -1;
        }
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    static String extensionOf(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        String name = fileName;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < name.length()) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static String hexPrefix(byte[] data, int count) {
        if (data == null || data.length == 0) {
            return "(empty)";
        }
        int n = Math.min(count, data.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format(Locale.ROOT, "%02X", data[i] & 0xFF));
        }
        return sb.toString();
    }

    private static byte[] readPrefix(File file, int max) throws IOException {
        int toRead = (int) Math.min(Math.max(file.length(), 0L), max);
        byte[] buffer = new byte[toRead];
        int offset = 0;
        try (InputStream in = new FileInputStream(file)) {
            while (offset < toRead) {
                int read = in.read(buffer, offset, toRead - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
        }
        if (offset == buffer.length) {
            return buffer;
        }
        byte[] trimmed = new byte[offset];
        System.arraycopy(buffer, 0, trimmed, 0, offset);
        return trimmed;
    }

    private static FileTypeDetection unknown(String summary, String detail) {
        return new FileTypeDetection(DetectedFileType.UNKNOWN, summary, detail);
    }

    private static String extensionOrNone(String extension) {
        return extension.isEmpty() ? "(none)" : extension;
    }
}
