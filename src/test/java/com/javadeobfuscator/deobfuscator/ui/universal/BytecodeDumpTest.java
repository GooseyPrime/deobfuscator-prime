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

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BytecodeDumpTest {
    @Test
    public void fullComparisonDetectsChangesAfterDisplayTruncation() throws Exception {
        File dir = Files.createTempDirectory("bytecode-full-compare").toFile();
        File original = new File(dir, "original.jar");
        File transformed = new File(dir, "transformed.jar");
        writeJar(original, false);
        writeJar(transformed, true);

        assertTrue(BytecodeDump.sameBytecode(BytecodeDump.dump(original), BytecodeDump.dump(transformed)));
        assertFalse(BytecodeDump.sameBytecode(original, transformed));
    }

    private static void writeJar(File file, boolean changeLaterClass) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            addClass(zip, "sample/Large.class", largeClass());
            addClass(zip, "sample/Later.class", laterClass(changeLaterClass));
        }
    }

    private static void addClass(ZipOutputStream zip, String name, byte[] data) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private static byte[] largeClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "sample/Large", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "padding", "()V", null, null);
        method.visitCode();
        for (int i = 0; i < 25000; i++) {
            method.visitInsn(Opcodes.NOP);
        }
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] laterClass(boolean changed) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "sample/Later", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "value", "()V", null, null);
        method.visitCode();
        if (changed) {
            method.visitInsn(Opcodes.NOP);
        }
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
