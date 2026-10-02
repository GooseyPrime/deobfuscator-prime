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

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

final class FileDrop {
    private FileDrop() {
    }

    static void install(JComponent component, final Consumer<File> onFile) {
        component.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    Object data = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!(data instanceof List)) {
                        return false;
                    }
                    List<?> files = (List<?>) data;
                    if (files.isEmpty() || !(files.get(0) instanceof File)) {
                        return false;
                    }
                    final File file = (File) files.get(0);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            onFile.accept(file);
                        }
                    });
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }
        });
    }
}
