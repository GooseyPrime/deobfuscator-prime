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
import com.javadeobfuscator.deobfuscator.tools.ExternalDeobfuscator;
import com.javadeobfuscator.deobfuscator.tools.ExternalToolRunner;
import com.javadeobfuscator.deobfuscator.tools.JobStatus;
import com.javadeobfuscator.deobfuscator.tools.ResolvedTool;
import com.javadeobfuscator.deobfuscator.tools.SettingField;
import com.javadeobfuscator.deobfuscator.tools.ToolSettings;
import com.javadeobfuscator.deobfuscator.tools.ViewerPages;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared tab for an external tool: path settings, a disabled state when the
 * tool is missing, a background run, and an original/result viewer.
 */
final class ExternalTypePanel extends JPanel {
    private final ExternalDeobfuscator backend;
    private final ToolSettings settings;
    private final Map<String, JTextField> pathFields = new LinkedHashMap<String, JTextField>();
    private final JLabel availabilityLabel = new JLabel("Checking tools...");
    private final JTextArea installArea = new JTextArea();
    private final JLabel inputLabel = new JLabel("Input: (none)");
    private final JLabel outputLabel = new JLabel("Output: (none yet)");
    private final JTextArea statusArea = new JTextArea(5, 40);
    private final JTextArea originalArea = new JTextArea();
    private final JTextArea resultArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final JButton runButton = new JButton("Run");
    private final JButton cancelButton = new JButton("Cancel");
    private final JProgressBar progress = new JProgressBar();

    private File input;
    private FileTypeDetection detection;
    private ResolvedTool resolved;
    private ExternalToolRunner runner;
    private SwingWorker<Void, String> worker;

    ExternalTypePanel(ExternalDeobfuscator backend, ToolSettings settings) {
        super(new BorderLayout(8, 8));
        this.backend = backend;
        this.settings = settings;
        setBorder(new EmptyBorder(8, 8, 8, 8));
        add(controls(), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
        refreshAvailability();
        showIdleStatus();
    }

    void acceptFile(File file, FileTypeDetection detection, boolean autoRun) {
        this.input = file;
        this.detection = detection;
        inputLabel.setText("Input: " + file.getAbsolutePath());
        originalArea.setText("");
        resultArea.setText("");
        logArea.setText("");
        refreshAvailability();
        try {
            ViewerPages pages = backend.loadViewer(file, null, false);
            originalArea.setText(pages.getLeftText());
            originalArea.setCaretPosition(0);
            resultArea.setText("Not deobfuscated yet.");
        } catch (Exception ex) {
            originalArea.setText("Could not preview the input: " + ex.getMessage());
        }
        if (resolved != null && resolved.autoRun() && autoRun) {
            startRun();
        } else if (resolved != null && resolved.getMode() != ResolvedTool.Mode.MISSING) {
            setStatus(new JobStatus(detectionLabel(), "none", resolved.getToolName(),
                    false, false, false, null, resolved.getMessage()));
        }
    }

    private JComponent controls() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridwidth = 4;

        JTextArea description = new JTextArea(backend.description());
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setOpaque(false);
        panel.add(description, gbc);

        gbc.gridy++;
        availabilityLabel.setFont(availabilityLabel.getFont().deriveFont(Font.BOLD, 13f));
        panel.add(availabilityLabel, gbc);

        gbc.gridy++;
        installArea.setEditable(false);
        installArea.setLineWrap(true);
        installArea.setWrapStyleWord(true);
        installArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        installArea.setRows(6);
        JScrollPane installScroll = new JScrollPane(installArea);
        installScroll.setBorder(BorderFactory.createTitledBorder("Tool availability"));
        panel.add(installScroll, gbc);

        gbc.gridwidth = 1;
        for (SettingField field : backend.settings()) {
            gbc.gridy++;
            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(new JLabel(field.getLabel()), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            JTextField text = new JTextField(settings.get(field.getKey()));
            pathFields.put(field.getKey(), text);
            panel.add(text, gbc);
            gbc.gridx = 2;
            gbc.weightx = 0;
            panel.add(browseButton(field.getKey(), text), gbc);
        }

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1;
        JPanel detectRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton detect = new JButton("Detect tools");
        detect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                savePaths();
                refreshAvailability();
            }
        });
        detectRow.add(detect);
        panel.add(detectRow, gbc);

        gbc.gridy++;
        panel.add(inputLabel, gbc);
        gbc.gridy++;
        panel.add(outputLabel, gbc);

        gbc.gridy++;
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                savePaths();
                refreshAvailability();
                startRun();
            }
        });
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (runner != null) {
                    runner.cancel();
                }
                if (worker != null) {
                    worker.cancel(true);
                }
            }
        });
        progress.setIndeterminate(false);
        progress.setPreferredSize(new Dimension(160, 18));
        progress.setVisible(false);
        actions.add(runButton);
        actions.add(cancelButton);
        actions.add(progress);
        panel.add(actions, gbc);
        return panel;
    }

    private JComponent body() {
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createTitledBorder("Status"));

        originalArea.setEditable(false);
        originalArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane left = new JScrollPane(originalArea);
        left.setBorder(BorderFactory.createTitledBorder("Original"));
        JScrollPane right = new JScrollPane(resultArea);
        right.setBorder(BorderFactory.createTitledBorder("Result"));
        JSplitPane viewer = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        viewer.setResizeWeight(0.5);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));
        logScroll.setPreferredSize(new Dimension(200, 120));

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.add(statusScroll, BorderLayout.NORTH);
        center.add(viewer, BorderLayout.CENTER);
        center.add(logScroll, BorderLayout.SOUTH);
        return center;
    }

    private JButton browseButton(final String key, final JTextField text) {
        JButton browse = new JButton("Browse...");
        browse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Locate " + key);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (chooser.showOpenDialog(ExternalTypePanel.this) == JFileChooser.APPROVE_OPTION) {
                    text.setText(chooser.getSelectedFile().getAbsolutePath());
                    savePaths();
                    refreshAvailability();
                }
            }
        });
        return browse;
    }

    private void savePaths() {
        for (Map.Entry<String, JTextField> entry : pathFields.entrySet()) {
            settings.set(entry.getKey(), entry.getValue().getText());
        }
    }

    private void refreshAvailability() {
        resolved = backend.resolve(settings);
        installArea.setText(resolved.getMessage() + "\n\n" + backend.installInstructions());
        installArea.setCaretPosition(0);
        if (resolved.getMode() == ResolvedTool.Mode.MISSING) {
            availabilityLabel.setText("Not available: " + resolved.getToolName() + " is missing.");
            availabilityLabel.setForeground(new Color(160, 30, 30));
            runButton.setEnabled(false);
        } else if (resolved.getMode() == ResolvedTool.Mode.MANUAL_ONLY) {
            availabilityLabel.setText("Manual only: " + resolved.getToolName() + " can be invoked. Nothing has run yet.");
            availabilityLabel.setForeground(new Color(140, 90, 0));
            runButton.setEnabled(input != null);
        } else {
            availabilityLabel.setText("Available: " + resolved.getMessage());
            availabilityLabel.setForeground(new Color(0, 110, 40));
            runButton.setEnabled(input != null);
        }
        if (input == null) {
            showIdleStatus();
        } else if (resolved.getMode() == ResolvedTool.Mode.MISSING) {
            setStatus(new JobStatus(detectionLabel(), "none", resolved.getToolName(),
                    false, false, false, null, resolved.getMessage()));
        }
    }

    private void showIdleStatus() {
        if (input == null) {
            setStatus(new JobStatus(backend.title(), "none", backend.title() + " external tool",
                    false, false, false, null, "No file loaded. Drop a file on the front page."));
        }
    }

    private void startRun() {
        if (input == null) {
            setStatus(new JobStatus(backend.title(), "none", backend.title(),
                    false, false, false, null, "No input file."));
            return;
        }
        if (resolved == null || !resolved.canRun()) {
            refreshAvailability();
            return;
        }
        final File output;
        final List<String> command;
        try {
            output = backend.outputFor(resolved, input);
            command = backend.command(resolved, input, output);
        } catch (RuntimeException ex) {
            setStatus(new JobStatus(detectionLabel(), "none", resolved.getToolName(),
                    false, false, false, null, ex.getMessage()));
            return;
        }
        outputLabel.setText("Output: " + output.getAbsolutePath());
        runButton.setEnabled(false);
        cancelButton.setEnabled(true);
        progress.setVisible(true);
        progress.setIndeterminate(true);
        logArea.setText("");
        appendLog("Command: " + join(command));
        final ResolvedTool tool = resolved;
        runner = new ExternalToolRunner();
        worker = new SwingWorker<Void, String>() {
            private int exitCode = -1;
            private String failure;

            @Override
            protected Void doInBackground() {
                try {
                    exitCode = runner.run(command, new java.util.function.Consumer<String>() {
                        @Override
                        public void accept(String line) {
                            publish(line);
                        }
                    });
                } catch (Exception ex) {
                    failure = ex.getMessage();
                    publish("ERROR: " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) {
                    appendLog(line);
                }
            }

            @Override
            protected void done() {
                progress.setVisible(false);
                progress.setIndeterminate(false);
                cancelButton.setEnabled(false);
                runButton.setEnabled(resolved != null && resolved.canRun() && input != null);
                boolean ran = failure == null;
                boolean success = ran && exitCode == 0 && output.exists();
                boolean changed = false;
                String detail = failure == null ? "Exit code " + exitCode + "." : failure;
                if (success) {
                    try {
                        changed = backend.outputChanged(input, output);
                    } catch (Exception ex) {
                        success = false;
                        detail = "Output could not be compared: " + ex.getMessage();
                    }
                } else if (ran && exitCode != 0) {
                    detail = "Exit code " + exitCode + ". See the log. Not deobfuscated.";
                } else if (ran) {
                    detail = "The process exited 0 but did not create " + output.getAbsolutePath();
                }
                try {
                    ViewerPages pages = backend.loadViewer(input, output, success && output.exists());
                    originalArea.setText(pages.getLeftText());
                    originalArea.setCaretPosition(0);
                    resultArea.setText(pages.getRightText());
                    resultArea.setCaretPosition(0);
                } catch (Exception ex) {
                    resultArea.setText("Could not read the output: " + ex.getMessage());
                }
                setStatus(new JobStatus(detectionLabel(), "none (external tool does not report a guess unless its log does)",
                        tool.getToolName(), ran, success, changed, output.exists() ? output : null, detail));
            }
        };
        worker.execute();
    }

    private void setStatus(JobStatus status) {
        statusArea.setText(status.toDisplayString());
        statusArea.setCaretPosition(0);
    }

    private String detectionLabel() {
        if (detection == null) {
            return backend.title();
        }
        return detection.getType().getDisplayName() + " — " + detection.getSummary();
    }

    private void appendLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private static String join(List<String> command) {
        StringBuilder sb = new StringBuilder();
        for (String part : command) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part);
        }
        return sb.toString();
    }
}
