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

import java.io.File;
import java.util.List;

/**
 * Optional external deobfuscator invoked by a type tab.
 */
public interface ExternalDeobfuscator {
    String title();

    String description();

    List<SettingField> settings();

    String installInstructions();

    ResolvedTool resolve(ToolSettings settings);

    File outputFor(ResolvedTool tool, File input);

    List<String> command(ResolvedTool tool, File input, File output);

    ViewerPages loadViewer(File input, File output, boolean produced) throws Exception;

    boolean outputChanged(File input, File output) throws Exception;
}
