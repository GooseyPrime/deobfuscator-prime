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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ExternalToolRunner {
    public static final long DEFAULT_TIMEOUT_MINUTES = 5L;

    private volatile Process process;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    public ExternalToolRunner() {
        this(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    ExternalToolRunner(long timeout, TimeUnit timeoutUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    public int run(List<String> command, Consumer<String> lineConsumer) throws IOException, InterruptedException {
        if (command == null || command.isEmpty()) {
            throw new IOException("No command to run.");
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process started = builder.start();
        this.process = started;
        AtomicReference<Throwable> readerFailure = new AtomicReference<Throwable>();
        Thread outputReader = new Thread(new Runnable() {
            @Override
            public void run() {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (lineConsumer != null) {
                            lineConsumer.accept(line);
                        }
                    }
                } catch (IOException | RuntimeException ex) {
                    readerFailure.set(ex);
                }
            }
        }, "ExternalTool-OutputReader");
        outputReader.setDaemon(true);
        outputReader.start();
        long deadline = System.nanoTime() + timeoutUnit.toNanos(timeout);
        boolean finished;
        try {
            finished = started.waitFor(timeout, timeoutUnit);
        } catch (InterruptedException ex) {
            started.destroyForcibly();
            closeOutput(started);
            outputReader.interrupt();
            outputReader.join(1000L);
            throw ex;
        }
        if (!finished) {
            started.destroyForcibly();
            closeOutput(started);
            outputReader.join(1000L);
            throw new IOException("Timed out after " + timeout + " " + timeoutUnit.toString().toLowerCase() + ".");
        }
        long remaining = deadline - System.nanoTime();
        if (remaining > 0) {
            long waitMillis = TimeUnit.NANOSECONDS.toMillis(remaining);
            int waitNanos = (int) (remaining - TimeUnit.MILLISECONDS.toNanos(waitMillis));
            outputReader.join(waitMillis, waitNanos);
        }
        if (outputReader.isAlive()) {
            closeOutput(started);
            outputReader.join(1000L);
            throw new IOException("Timed out after " + timeout + " " + timeoutUnit.toString().toLowerCase() + ".");
        }
        Throwable failure = readerFailure.get();
        if (failure instanceof IOException) {
            throw (IOException) failure;
        }
        if (failure != null) {
            throw new IOException("Could not process external tool output.", failure);
        }
        return started.exitValue();
    }

    private static void closeOutput(Process process) {
        try {
            process.getInputStream().close();
        } catch (IOException ignored) {
        }
    }

    public void cancel() {
        Process current = process;
        if (current != null) {
            current.destroyForcibly();
        }
    }
}
