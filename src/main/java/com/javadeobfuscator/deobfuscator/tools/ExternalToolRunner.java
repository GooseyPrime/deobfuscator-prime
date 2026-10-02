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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ExternalToolRunner {
    public static final long DEFAULT_TIMEOUT_MINUTES = 5L;

    private volatile Process process;

    public int run(List<String> command, Consumer<String> lineConsumer) throws IOException, InterruptedException {
        if (command == null || command.isEmpty()) {
            throw new IOException("No command to run.");
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process started = builder.start();
        this.process = started;
        StringBuilder captured = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                captured.append(line).append('\n');
                if (lineConsumer != null) {
                    lineConsumer.accept(line);
                }
            }
        }
        boolean finished = started.waitFor(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!finished) {
            started.destroyForcibly();
            throw new IOException("Timed out after " + DEFAULT_TIMEOUT_MINUTES + " minutes.");
        }
        return started.exitValue();
    }

    public void cancel() {
        Process current = process;
        if (current != null) {
            current.destroyForcibly();
        }
    }
}
