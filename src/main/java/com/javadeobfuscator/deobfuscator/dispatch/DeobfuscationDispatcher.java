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
import com.javadeobfuscator.deobfuscator.detect.FileTypeDetection;
import com.javadeobfuscator.deobfuscator.detect.FileTypeDetector;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Routes a detected file to exactly one handler. Routing is not a claim
 * that deobfuscation succeeded.
 */
public final class DeobfuscationDispatcher {
    private final Map<DetectedFileType, TypeHandler> handlers = new EnumMap<DetectedFileType, TypeHandler>(DetectedFileType.class);

    public DeobfuscationDispatcher() {
        this(BuiltinHandlers.all());
    }

    public DeobfuscationDispatcher(List<TypeHandler> handlerList) {
        for (TypeHandler handler : handlerList) {
            handlers.put(handler.type(), handler);
        }
        if (!handlers.containsKey(DetectedFileType.UNKNOWN)) {
            throw new IllegalArgumentException("A handler for Unknown is required so unrecognized files are not dropped.");
        }
    }

    public DispatchResult dispatch(File file) {
        FileTypeDetection detection = FileTypeDetector.detect(file);
        return new DispatchResult(file, detection, handlerFor(detection.getType()));
    }

    public DispatchResult dispatch(byte[] data, String fileName) {
        FileTypeDetection detection = FileTypeDetector.detect(data, fileName);
        return new DispatchResult(null, detection, handlerFor(detection.getType()));
    }

    public TypeHandler handlerFor(DetectedFileType type) {
        TypeHandler handler = handlers.get(type);
        if (handler == null) {
            handler = handlers.get(DetectedFileType.UNKNOWN);
        }
        return handler;
    }
}
