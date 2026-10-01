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

package com.javadeobfuscator.deobfuscator.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility to locate Java runtime libraries (rt.jar for Java 8 or jmods for Java 9+).
 * Java Deobfuscator requires runtime classes to resolve type hierarchies and compute frames.
 */
public class RuntimeDetector {

    public static File findRuntimeLibrary() {
        List<File> candidates = findRuntimeCandidates();
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    public static List<File> findRuntimeCandidates() {
        Set<File> candidates = new LinkedHashSet<>();

        // 1. Check current running java.home
        String javaHomeProp = System.getProperty("java.home");
        if (javaHomeProp != null && !javaHomeProp.isEmpty()) {
            checkDirectory(new File(javaHomeProp), candidates);
        }

        // 2. Check JAVA_HOME environment variable
        String javaHomeEnv = System.getenv("JAVA_HOME");
        if (javaHomeEnv != null && !javaHomeEnv.isEmpty()) {
            checkDirectory(new File(javaHomeEnv), candidates);
        }

        // 3. Check common Linux JVM directories
        File linuxJvmDir = new File("/usr/lib/jvm");
        if (linuxJvmDir.isDirectory()) {
            File[] jvms = linuxJvmDir.listFiles();
            if (jvms != null) {
                for (File jvm : jvms) {
                    if (jvm.isDirectory()) {
                        checkDirectory(jvm, candidates);
                    }
                }
            }
        }

        // 4. Check common Windows JVM directories
        String[] winRoots = {"C:\\Program Files\\Java", "C:\\Program Files (x86)\\Java", "C:\\Java"};
        for (String root : winRoots) {
            File dir = new File(root);
            if (dir.isDirectory()) {
                File[] jvms = dir.listFiles();
                if (jvms != null) {
                    for (File jvm : jvms) {
                        if (jvm.isDirectory()) {
                            checkDirectory(jvm, candidates);
                        }
                    }
                }
            }
        }

        // 5. Check common macOS JVM directories
        File macJvmDir = new File("/Library/Java/JavaVirtualMachines");
        if (macJvmDir.isDirectory()) {
            File[] jvms = macJvmDir.listFiles();
            if (jvms != null) {
                for (File jvm : jvms) {
                    File home = new File(jvm, "Contents/Home");
                    if (home.isDirectory()) {
                        checkDirectory(home, candidates);
                    } else if (jvm.isDirectory()) {
                        checkDirectory(jvm, candidates);
                    }
                }
            }
        }

        return new ArrayList<>(candidates);
    }

    private static void checkDirectory(File dir, Set<File> candidates) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        // Check for Java 8 rt.jar in jre/lib/rt.jar or lib/rt.jar
        File jreRt = new File(dir, "jre" + File.separator + "lib" + File.separator + "rt.jar");
        if (jreRt.isFile()) {
            candidates.add(jreRt);
            return;
        }
        File libRt = new File(dir, "lib" + File.separator + "rt.jar");
        if (libRt.isFile()) {
            candidates.add(libRt);
            return;
        }

        // Check for Java 9+ jmods folder
        File jmods = new File(dir, "jmods");
        if (jmods.isDirectory() && new File(jmods, "java.base.jmod").isFile()) {
            candidates.add(jmods);
            return;
        }

        // Check if directory itself is jmods
        if (new File(dir, "java.base.jmod").isFile()) {
            candidates.add(dir);
        }
    }

    public static String describeRuntime(File file) {
        if (file == null) {
            return "No runtime specified";
        }
        if (!file.exists()) {
            return "Path does not exist: " + file.getAbsolutePath();
        }
        if (file.isFile() && file.getName().endsWith(".jar")) {
            return "Java 8 Runtime Library: " + file.getAbsolutePath() + " (" + (file.length() / (1024 * 1024)) + " MB)";
        }
        if (file.isDirectory() && new File(file, "java.base.jmod").isFile()) {
            File[] mods = file.listFiles(f -> f.getName().endsWith(".jmod"));
            int count = (mods != null) ? mods.length : 0;
            return "Modern JDK Runtime (jmods directory with " + count + " modules): " + file.getAbsolutePath();
        }
        if (file.isFile() && file.getName().endsWith(".jmod")) {
            return "Java Module: " + file.getAbsolutePath();
        }
        return "Runtime Path: " + file.getAbsolutePath();
    }
}
