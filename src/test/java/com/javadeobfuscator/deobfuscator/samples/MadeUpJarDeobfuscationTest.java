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

import com.javadeobfuscator.deobfuscator.Deobfuscator;
import com.javadeobfuscator.deobfuscator.config.Configuration;
import com.javadeobfuscator.deobfuscator.config.TransformerConfig;
import com.javadeobfuscator.deobfuscator.transformers.general.peephole.PeepholeOptimizer;
import com.javadeobfuscator.deobfuscator.ui.universal.BytecodeDump;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MadeUpJarDeobfuscationTest {
    @Test
    public void peepholePassChangesTheMadeUpJar() throws Throwable {
        File dir = Files.createTempDirectory("made-up-jar").toFile();
        File input = new File(dir, "made-up-obfuscated.jar");
        File output = new File(dir, "made-up-obfuscated-deobfuscated.jar");
        try (FileOutputStream out = new FileOutputStream(input)) {
            out.write(MadeUpSampleGenerator.obfuscatedJarBytes());
        }

        Configuration configuration = new Configuration();
        configuration.setInput(input);
        configuration.setOutput(output);
        configuration.setVerify(true);
        File runtime = runtimeLibrary();
        if (runtime != null) {
            configuration.setPath(Collections.singletonList(runtime));
        }
        configuration.setTransformers(Collections.singletonList(TransformerConfig.configFor(PeepholeOptimizer.class)));

        new Deobfuscator(configuration).start();

        assertTrue(output.isFile());
        String before = BytecodeDump.dump(input);
        String after = BytecodeDump.dump(output);
        assertTrue(before.contains("NOP"));
        assertFalse(BytecodeDump.sameBytecode(before, after));
        assertFalse(after.contains("unreachable-made-up"));
    }

    private static File runtimeLibrary() {
        String home = System.getProperty("java.home");
        if (home == null) {
            return null;
        }
        File jmod = new File(home, "jmods/java.base.jmod");
        if (jmod.isFile()) {
            return jmod;
        }
        File rt = new File(home, "lib/rt.jar");
        if (rt.isFile()) {
            return rt;
        }
        return null;
    }
}
