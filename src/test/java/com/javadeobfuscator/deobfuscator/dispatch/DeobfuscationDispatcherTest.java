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

package com.javadeobfuscator.deobfuscator.dispatch;

import com.javadeobfuscator.deobfuscator.detect.DetectedFileType;
import com.javadeobfuscator.deobfuscator.samples.SyntheticFiles;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class DeobfuscationDispatcherTest {
    private final DeobfuscationDispatcher dispatcher = new DeobfuscationDispatcher();

    @Test
    public void routesEachSupportedTypeToItsHandler() throws Exception {
        assertEquals("Java", dispatcher.dispatch(SyntheticFiles.classMagic(), "A.class").getHandler().tabTitle());
        assertEquals(DetectedFileType.JAVA, dispatcher.dispatch(SyntheticFiles.zipWith("A.class", SyntheticFiles.classMagic()), "a.jar").getDetection().getType());
        assertEquals("JavaScript", dispatcher.dispatch("var a=1;".getBytes(StandardCharsets.UTF_8), "a.js").getHandler().tabTitle());
        assertEquals(".NET", dispatcher.dispatch(SyntheticFiles.pe(true), "a.exe").getHandler().tabTitle());
        assertEquals("Android", dispatcher.dispatch(SyntheticFiles.dexMagic(), "classes.dex").getHandler().tabTitle());
        assertEquals("Android", dispatcher.dispatch(SyntheticFiles.zipWith("AndroidManifest.xml", new byte[]{'<'}), "a.apk").getHandler().tabTitle());
        assertEquals("Unknown", dispatcher.dispatch(new byte[]{1, 2, 3, 4}, "mystery.bin").getHandler().tabTitle());
    }

    @Test
    public void magicBeatsAMisleadingExtension() {
        DispatchResult result = dispatcher.dispatch(SyntheticFiles.classMagic(), "script.js");
        assertEquals(DetectedFileType.JAVA, result.getDetection().getType());
        assertEquals("Java", result.getHandler().tabTitle());
    }

    @Test
    public void nativeExecutableIsNotSentToDotNet() {
        DispatchResult result = dispatcher.dispatch(SyntheticFiles.pe(false), "native.exe");
        assertEquals(DetectedFileType.UNKNOWN, result.getDetection().getType());
        assertEquals("Unknown", result.getHandler().tabTitle());
    }
}
