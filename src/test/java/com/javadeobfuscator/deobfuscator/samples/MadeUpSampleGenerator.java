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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes labeled made-up fixtures. These are not captured malware or real protected programs.
 */
public final class MadeUpSampleGenerator {
    private MadeUpSampleGenerator() {
    }

    public static byte[] obfuscatedClassBytes() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "sample/MadeUp", null, "java/lang/Object", null);
        writer.visitSource("MadeUp.java", null);

        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();

        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "message", "()Ljava/lang/String;", null, null);
        method.visitCode();
        method.visitInsn(Opcodes.NOP);
        method.visitInsn(Opcodes.NOP);
        method.visitInsn(Opcodes.NOP);
        method.visitLdcInsn("made-up-sample");
        method.visitInsn(Opcodes.NOP);
        Label ret = new Label();
        method.visitJumpInsn(Opcodes.GOTO, ret);
        method.visitLdcInsn("not-used");
        method.visitInsn(Opcodes.POP);
        method.visitLabel(ret);
        method.visitInsn(Opcodes.ARETURN);
        method.visitLdcInsn("unreachable-made-up");
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    public static byte[] obfuscatedJarBytes() throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("sample/MadeUp.class"));
            zip.write(obfuscatedClassBytes());
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    public static String obfuscatedJavaScript() {
        return "// MADE-UP sample. Not a real-world payload.\n"
                + "var _0x4f2a=[\"\\x6d\\x61\\x64\\x65\\x2d\\x75\\x70\",\"\\x20\\x73\\x61\\x6d\\x70\\x6c\\x65\"];\n"
                + "function _0xdec(i){return _0x4f2a[i];}\n"
                + "console.log(_0xdec(0)+_0xdec(1));\n";
    }

    public static void writeAll(File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create " + directory.getAbsolutePath());
        }
        write(new File(directory, "made-up-obfuscated.jar"), obfuscatedJarBytes());
        write(new File(directory, "made-up-obfuscated.js"), obfuscatedJavaScript().getBytes(StandardCharsets.UTF_8));
        write(new File(directory, "made-up-dotnet.exe"), SyntheticFiles.pe(true));
        write(new File(directory, "made-up-native.exe"), SyntheticFiles.pe(false));
        write(new File(directory, "made-up-android.apk"), SyntheticFiles.zipWith("AndroidManifest.xml", "<manifest/>".getBytes(StandardCharsets.UTF_8)));
        write(new File(directory, "made-up-unknown.bin"), new byte[]{0x00, 0x01, 0x02, 0x03, 0x04});
        try (OutputStreamWriter readme = new OutputStreamWriter(new FileOutputStream(new File(directory, "README.md")), StandardCharsets.UTF_8)) {
            readme.write("# Made-up samples\n\n");
            readme.write("These files were generated for the universal deobfuscator. ");
            readme.write("They are not real malware, not real protected applications, and not produced by a commercial obfuscator.\n\n");
            readme.write("- `made-up-obfuscated.jar` is a class with NOP padding, a redundant goto, and unreachable instructions.\n");
            readme.write("- `made-up-obfuscated.js` is a tiny hand-written string-table script.\n");
            readme.write("- `made-up-dotnet.exe` is a synthetic PE header with a CLR data directory. It is not a valid assembly.\n");
            readme.write("- `made-up-native.exe` is a synthetic PE header without a CLR directory.\n");
            readme.write("- `made-up-android.apk` is a ZIP that only contains a fake AndroidManifest.xml.\n");
            readme.write("- `made-up-unknown.bin` is five arbitrary bytes.\n");
        }
    }

    public static void main(String[] args) throws IOException {
        File directory = new File(args.length == 0 ? "samples/test-resources" : args[0]);
        writeAll(directory);
        System.out.println("Wrote made-up samples to " + directory.getAbsolutePath());
    }

    private static void write(File file, byte[] data) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(data);
        }
    }
}
