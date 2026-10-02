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

import com.javadeobfuscator.deobfuscator.detect.FileTypeDetection;

import java.io.File;

public final class DispatchResult {
    private final File file;
    private final FileTypeDetection detection;
    private final TypeHandler handler;

    public DispatchResult(File file, FileTypeDetection detection, TypeHandler handler) {
        this.file = file;
        this.detection = detection;
        this.handler = handler;
    }

    public File getFile() {
        return file;
    }

    public FileTypeDetection getDetection() {
        return detection;
    }

    public TypeHandler getHandler() {
        return handler;
    }
}
