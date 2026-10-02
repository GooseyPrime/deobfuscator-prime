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

import com.javadeobfuscator.deobfuscator.dispatch.DispatchResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.function.Consumer;

final class FrontDoorPanel extends JPanel {
    private final JTextArea detectionArea = new JTextArea();
    private final Consumer<File> onFile;

    FrontDoorPanel(Consumer<File> onFile) {
        super(new BorderLayout(12, 12));
        this.onFile = onFile;
        setBorder(new EmptyBorder(16, 16, 16, 16));
        add(intro(), BorderLayout.NORTH);
        add(dropZone(), BorderLayout.CENTER);
        add(detectionPanel(), BorderLayout.SOUTH);
        FileDrop.install(this, onFile);
    }

    private JComponent intro() {
        JTextArea intro = new JTextArea(
                "Drop a file here, or browse. The type is detected from magic bytes first and the extension second, "
                        + "then the matching tab opens.\n\n"
                        + "Java (.jar, .class, .war) uses the built-in deobfuscator.\n"
                        + "JavaScript (.js, .mjs) uses webcrack or synchrony when one can be invoked.\n"
                        + ".NET assemblies (PE with a CLR header) use de4dot when it is installed.\n"
                        + "Android (.apk, .dex) uses jadx or apktool when one is installed.\n"
                        + "Anything else is reported as Unknown and is not processed.\n"
                        + "Output is written under the out folder. The input file is never overwritten."
        );
        intro.setEditable(false);
        intro.setLineWrap(true);
        intro.setWrapStyleWord(true);
        intro.setOpaque(false);
        intro.setFont(intro.getFont().deriveFont(14f));
        return intro;
    }

    private JComponent dropZone() {
        JPanel zone = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(47, 93, 140));
                float[] dash = {8f, 6f};
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash, 0f));
                g2.drawRoundRect(12, 12, Math.max(1, getWidth() - 24), Math.max(1, getHeight() - 24), 16, 16);
                g2.dispose();
            }
        };
        zone.setBackground(new Color(244, 247, 251));
        zone.setPreferredSize(new Dimension(400, 220));
        JLabel label = new JLabel("Drop a file to detect and route it");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        JButton browse = new JButton("Browse...");
        browse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browse();
            }
        });
        JPanel stack = new JPanel(new GridLayout(2, 1, 0, 12));
        stack.setOpaque(false);
        stack.add(label);
        JPanel buttonRow = new JPanel();
        buttonRow.setOpaque(false);
        buttonRow.add(browse);
        stack.add(buttonRow);
        zone.add(stack);
        FileDrop.install(zone, onFile);
        return zone;
    }

    private JComponent detectionPanel() {
        detectionArea.setEditable(false);
        detectionArea.setRows(5);
        detectionArea.setLineWrap(true);
        detectionArea.setWrapStyleWord(true);
        detectionArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detectionArea.setText("No file selected yet.");
        JScrollPane scroll = new JScrollPane(detectionArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Last detection"));
        scroll.setPreferredSize(new Dimension(400, 140));
        return scroll;
    }

    void showDetection(DispatchResult result) {
        String file = result.getFile() == null ? "(memory)" : result.getFile().getAbsolutePath();
        detectionArea.setText(
                "File: " + file + "\n"
                        + "Detected type: " + result.getDetection().getType().getDisplayName() + "\n"
                        + "Summary: " + result.getDetection().getSummary() + "\n"
                        + result.getDetection().getDetail() + "\n"
                        + "Routed to tab: " + result.getHandler().tabTitle()
        );
        detectionArea.setCaretPosition(0);
    }

    private void browse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a file to deobfuscate");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            onFile.accept(chooser.getSelectedFile());
        }
    }
}
