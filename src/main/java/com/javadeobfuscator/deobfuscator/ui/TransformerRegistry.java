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

import java.util.*;

public class TransformerRegistry {

    public static class TransformerItem {
        private final String id;
        private final String category;
        private final String name;
        private final String description;

        public TransformerItem(String id, String category, String name, String description) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getCategory() {
            return category;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return category + " > " + name;
        }
    }

    private static final List<TransformerItem> ALL_TRANSFORMERS = new ArrayList<>();
    private static final Map<String, List<String>> PRESETS = new LinkedHashMap<>();

    static {
        // --- Allatori ---
        register("allatori.StringEncryptionTransformer", "Allatori", "String Encryption", "Decrypts Allatori encrypted strings (standard and legacy modes).");
        register("allatori.FlowObfuscationTransformer", "Allatori", "Flow Obfuscation", "Removes opaque predicates, dead jumps, and flow obfuscation created by Allatori.");
        register("allatori.LightFlowObfuscationTransformer", "Allatori", "Light Flow Obfuscation", "Lightweight flow deobfuscation for Allatori.");

        // --- Zelix KlassMaster ---
        register("zelix.string.SimpleStringEncryptionTransformer", "Zelix KlassMaster", "Simple String Encryption", "Decrypts standard XOR/keyed string encryption from ZKM.");
        register("zelix.string.EnhancedStringEncryptionTransformer", "Zelix KlassMaster", "Enhanced String Encryption", "Decrypts multi-layer/enhanced string encryption from ZKM.");
        register("zelix.ReflectionObfuscationTransformer", "Zelix KlassMaster", "Reflection Obfuscation", "Inlines and resolves obfuscated reflection calls from ZKM.");
        register("zelix.FlowObfuscationTransformer", "Zelix KlassMaster", "Flow Obfuscation", "Cleans control flow and exception handler traps inserted by ZKM.");

        // --- Stringer ---
        register("stringer.StringEncryptionTransformer", "Stringer", "String Encryption", "Decrypts Stringer encrypted strings.");
        register("stringer.InvokedynamicTransformer", "Stringer", "InvokeDynamic Obfuscation", "Resolves invokedynamic bootstrap calls to explicit method calls.");
        register("stringer.ReflectionObfuscationTransformer", "Stringer", "Reflection Obfuscation", "Inlines Stringer reflection wrappers.");
        register("stringer.HideAccessObfuscationTransformer", "Stringer", "HideAccess Obfuscation", "Restores hidden field/method access.");
        register("stringer.ResourceEncryptionTransformer", "Stringer", "Resource Encryption", "Decrypts Stringer encrypted embedded resources.");
        register("stringer.v3.StringEncryptionTransformer", "Stringer", "String Encryption (v3)", "Decrypts Stringer v3 encrypted strings.");
        register("stringer.v3.InvokedynamicTransformer", "Stringer", "InvokeDynamic (v3)", "Resolves Stringer v3 invokedynamic instructions.");

        // --- DashO ---
        register("dasho.StringEncryptionTransformer", "DashO", "String Encryption", "Decrypts Dash-O encrypted strings.");
        register("dasho.FlowObfuscationTransformer", "DashO", "Flow Obfuscation", "Cleans Dash-O control flow flattening and loops.");
        register("dasho.FakeExceptionTransformer", "DashO", "Fake Exception Removal", "Removes dummy try-catch blocks inserted by Dash-O.");
        register("dasho.TamperObfuscationTransformer", "DashO", "Tamper Check Removal", "Removes anti-tamper and integrity check methods.");

        // --- Radon ---
        register("special.RadonTransformer", "Radon", "Radon Transformer", "Deobfuscates Radon obfuscator: flow, numbers, strings, and invokedynamic.");
        register("special.RadonTransformerV2", "Radon", "Radon Transformer (v2)", "Deobfuscates Radon v2 obfuscator.");

        // --- General Peephole ---
        register("general.peephole.PeepholeOptimizer", "Peephole", "Peephole Optimizer", "Runs a suite of bytecode simplifications (constant folding, dead code removal, nop elimination).");
        register("general.peephole.ConstantFolder", "Peephole", "Constant Folder", "Folds static arithmetic and constant expressions.");
        register("general.peephole.DeadCodeRemover", "Peephole", "Dead Code Remover", "Removes unreachable bytecode instructions and basic blocks.");
        register("general.peephole.GotoRearranger", "Peephole", "Goto Rearranger", "Eliminates redundant goto chains.");
        register("general.peephole.NopRemover", "Peephole", "NOP Remover", "Removes unnecessary NOP instructions.");
        register("general.peephole.RedundantGotoRemover", "Peephole", "Redundant GOTO Remover", "Removes goto instructions that point directly to the next instruction.");
        register("general.peephole.RedundantTrapRemover", "Peephole", "Redundant Trap Remover", "Removes empty or useless try-catch handlers.");
        register("general.peephole.StackOperationSimplifier", "Peephole", "Stack Simplifier", "Simplifies redundant DUP, SWAP, and POP instruction sequences.");
        register("general.peephole.TrapHandlerMerger", "Peephole", "Trap Handler Merger", "Merges duplicate try-catch handlers.");

        // --- Removers ---
        register("general.removers.LineNumberRemover", "Removers", "Line Number Remover", "Strips line number debug tables.");
        register("general.removers.LocalVariableRemover", "Removers", "Local Variable Remover", "Strips local variable debug tables.");
        register("general.removers.IllegalSignatureRemover", "Removers", "Illegal Signature Remover", "Fixes or removes invalid generic signatures that crash decompilers.");
        register("general.removers.IllegalVarargsRemover", "Removers", "Illegal Varargs Remover", "Fixes invalid ACC_VARARGS flags.");
        register("general.removers.IllegalAnnotationRemover", "Removers", "Illegal Annotation Remover", "Removes corrupt annotations designed to crash decompilers.");
        register("general.removers.SyntheticBridgeRemover", "Removers", "Synthetic Bridge Remover", "Removes confusing synthetic bridge methods.");

        // --- Normalizers (Remapping) ---
        register("normalizer.ClassNormalizer", "Normalizers", "Class Normalizer", "Renames obfuscated class names to readable names (Class0, Class1, ...) or uses mapping file.");
        register("normalizer.MethodNormalizer", "Normalizers", "Method Normalizer", "Renames obfuscated method names (Method0, Method1, ...) or uses mapping file.");
        register("normalizer.FieldNormalizer", "Normalizers", "Field Normalizer", "Renames obfuscated field names (Field0, Field1, ...) or uses mapping file.");
        register("normalizer.PackageNormalizer", "Normalizers", "Package Normalizer", "Reorganizes obfuscated package hierarchies.");
        register("normalizer.PackageTruncator", "Normalizers", "Package Truncator", "Flattens deep nested obfuscated package directories.");
        register("normalizer.DuplicateRenamer", "Normalizers", "Duplicate Renamer", "Resolves duplicate member names caused by casing or overload obfuscation.");
        register("normalizer.SourceFileClassNormalizer", "Normalizers", "SourceFile Normalizer", "Renames classes based on their SourceFile debug attribute if present.");
        register("normalizer.EnumNormalizer", "Normalizers", "Enum Normalizer", "Restores proper enum class declarations.");

        // --- Special & Other Obfuscators ---
        register("special.BinscureTransformer", "Special", "Binscure Transformer", "Deobfuscates Binscure obfuscator protection.");
        register("special.BisGuardTransformer", "Special", "BisGuard Transformer", "Deobfuscates BisGuard class encryption.");
        register("special.SuperblaubeereTransformer", "Special", "Superblaubeere Transformer", "Deobfuscates Superblaubeere obfuscator.");
        register("special.ParamorphismTransformer", "Special", "Paramorphism Transformer", "Deobfuscates Paramorphism obfuscation.");
        register("special.TryCatchFixer", "Special", "Try-Catch Fixer", "Fixes malformed exception handler ranges.");
        register("special.UselessArithmeticTransformer", "Special", "Useless Arithmetic Remover", "Removes obfuscated math expressions and dead arithmetic.");
        register("special.InvalidClassRemover", "Special", "Invalid Class Remover", "Strips intentionally corrupt dummy classes.");
        register("smoke.StringEncryptionTransformer", "Smoke", "Smoke String Encryption", "Decrypts Smoke obfuscator strings.");
        register("smoke.NumberObfuscationTransformer", "Smoke", "Smoke Number Obfuscation", "Deobfuscates Smoke obfuscator numbers.");
        register("skidsuite2.StringEncryptionTransformer", "SkidSuite", "SkidSuite String Encryption", "Decrypts SkidSuite2 strings.");
        register("skidsuite2.FakeExceptionTransformer", "SkidSuite", "SkidSuite Fake Exceptions", "Removes SkidSuite2 dummy exceptions.");
        register("antireleak.StringEncryptionTransformer", "AntiReleak", "AntiReleak String Encryption", "Decrypts AntiReleak strings.");
        register("antireleak.InvokedynamicTransformer", "AntiReleak", "AntiReleak InvokeDynamic", "Resolves AntiReleak invokedynamic calls.");

        // --- Presets ---
        PRESETS.put("Allatori Deobfuscation", Arrays.asList(
                "allatori.StringEncryptionTransformer",
                "allatori.FlowObfuscationTransformer",
                "general.peephole.PeepholeOptimizer"
        ));

        PRESETS.put("Zelix KlassMaster (ZKM)", Arrays.asList(
                "zelix.ReflectionObfuscationTransformer",
                "zelix.string.EnhancedStringEncryptionTransformer",
                "zelix.FlowObfuscationTransformer",
                "general.peephole.PeepholeOptimizer"
        ));

        PRESETS.put("Stringer", Arrays.asList(
                "stringer.InvokedynamicTransformer",
                "stringer.ReflectionObfuscationTransformer",
                "stringer.HideAccessObfuscationTransformer",
                "stringer.StringEncryptionTransformer",
                "general.peephole.PeepholeOptimizer"
        ));

        PRESETS.put("Dash-O", Arrays.asList(
                "dasho.FakeExceptionTransformer",
                "dasho.StringEncryptionTransformer",
                "dasho.FlowObfuscationTransformer",
                "general.peephole.PeepholeOptimizer"
        ));

        PRESETS.put("Radon", Arrays.asList(
                "special.RadonTransformer",
                "general.peephole.PeepholeOptimizer"
        ));

        PRESETS.put("Peephole Cleanup & Optimization", Arrays.asList(
                "general.peephole.PeepholeOptimizer",
                "general.removers.IllegalSignatureRemover",
                "general.removers.IllegalVarargsRemover"
        ));

        PRESETS.put("Deobfuscate & Rename All (Normalizers)", Arrays.asList(
                "general.peephole.PeepholeOptimizer",
                "normalizer.PackageNormalizer",
                "normalizer.ClassNormalizer",
                "normalizer.FieldNormalizer",
                "normalizer.MethodNormalizer"
        ));
    }

    private static void register(String id, String category, String name, String description) {
        ALL_TRANSFORMERS.add(new TransformerItem(id, category, name, description));
    }

    public static List<TransformerItem> getAllTransformers() {
        return Collections.unmodifiableList(ALL_TRANSFORMERS);
    }

    public static TransformerItem findById(String id) {
        if (id == null) return null;
        for (TransformerItem item : ALL_TRANSFORMERS) {
            if (item.getId().equalsIgnoreCase(id) ||
                    ("com.javadeobfuscator.deobfuscator.transformers." + item.getId()).equalsIgnoreCase(id)) {
                return item;
            }
        }
        return new TransformerItem(id, "Custom", id, "Custom transformer: " + id);
    }

    public static Map<String, List<String>> getPresets() {
        return Collections.unmodifiableMap(PRESETS);
    }
}
