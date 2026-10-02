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

import com.javadeobfuscator.deobfuscator.detect.FileTypeDetection;
import com.javadeobfuscator.deobfuscator.detect.FileTypeDetector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

final class UnknownTypePanel extends JPanel {
    private final JTextArea details = new JTextArea();

    UnknownTypePanel() {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel title = new JLabel("Unknown file");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JTextArea intro = new JTextArea(
                "This tab is for files that are not Java, JavaScript, .NET, or Android. "
                        + "They are reported here and are not deobfuscated."
        );
        intro.setEditable(false);
        intro.setOpaque(false);
        intro.setLineWrap(true);
        intro.setWrapStyleWord(true);
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.add(title, BorderLayout.NORTH);
        header.add(intro, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setText("No file has been routed here yet.\nResult: not run. Not deobfuscated.");
        add(new JScrollPane(details), BorderLayout.CENTER);
    }

    void showFile(File file, FileTypeDetection detection) {
        StringBuilder sb = new StringBuilder();
        sb.append("Detected type: Unknown\n");
        sb.append("Summary: ").append(detection.getSummary()).append('\n');
        sb.append(detection.getDetail()).append('\n');
        sb.append("Result: not run. Not deobfuscated.\n");
        sb.append("Tool: none\n");
        sb.append("Obfuscator guess: none\n");
        sb.append("Output location: (none)\n\n");
        if (file != null) {
            sb.append("File: ").append(file.getAbsolutePath()).append('\n');
            sb.append("Size: ").append(file.length()).append(" bytes\n");
            sb.append("Magic: ").append(readMagic(file)).append('\n');
        }
        details.setText(sb.toString());
        details.setCaretPosition(0);
    }

    private static String readMagic(File file) {
        byte[] header = new byte[16];
        try (FileInputStream in = new FileInputStream(file)) {
            int read = in.read(header);
            if (read <= 0) {
                return "(empty)";
            }
            byte[] used = new byte[read];
            System.arraycopy(header, 0, used, 0, read);
            return FileTypeDetector.hexPrefix(used, read);
        } catch (IOException ex) {
            return "(unreadable: " + ex.getMessage() + ")";
        }
    }
}
