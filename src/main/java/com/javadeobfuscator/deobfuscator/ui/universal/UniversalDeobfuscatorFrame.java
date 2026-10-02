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

import com.javadeobfuscator.deobfuscator.detect.DetectedFileType;
import com.javadeobfuscator.deobfuscator.dispatch.DeobfuscationDispatcher;
import com.javadeobfuscator.deobfuscator.dispatch.DispatchResult;
import com.javadeobfuscator.deobfuscator.tools.AndroidTools;
import com.javadeobfuscator.deobfuscator.tools.DotNetTools;
import com.javadeobfuscator.deobfuscator.tools.JavaScriptTools;
import com.javadeobfuscator.deobfuscator.tools.ToolSettings;
import com.javadeobfuscator.deobfuscator.ui.DeobfuscatorGUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.function.Consumer;

/**
 * Front door for every supported file type. The Java tab is the existing deobfuscator UI.
 */
public class UniversalDeobfuscatorFrame extends JFrame {
    public static final int TAB_FRONT = 0;
    public static final int TAB_JAVA = 1;
    public static final int TAB_JAVASCRIPT = 2;
    public static final int TAB_DOTNET = 3;
    public static final int TAB_ANDROID = 4;
    public static final int TAB_UNKNOWN = 5;

    private final DeobfuscationDispatcher dispatcher = new DeobfuscationDispatcher();
    private final JTabbedPane tabs = new JTabbedPane();
    private final FrontDoorPanel frontPanel;
    private final DeobfuscatorGUI javaPanel;
    private final ExternalTypePanel javaScriptPanel;
    private final ExternalTypePanel dotNetPanel;
    private final ExternalTypePanel androidPanel;
    private final UnknownTypePanel unknownPanel;

    public UniversalDeobfuscatorFrame() {
        super("Universal Deobfuscator");
        ToolSettings settings = ToolSettings.load();
        frontPanel = new FrontDoorPanel(new Consumer<File>() {
            @Override
            public void accept(File file) {
                openFile(file);
            }
        });
        javaPanel = new DeobfuscatorGUI();
        javaScriptPanel = new ExternalTypePanel(new JavaScriptTools(), settings);
        dotNetPanel = new ExternalTypePanel(new DotNetTools(), settings);
        androidPanel = new ExternalTypePanel(new AndroidTools(), settings);
        unknownPanel = new UnknownTypePanel();

        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 13f));
        tabs.addTab("Front", frontPanel);
        tabs.addTab("Java", new JScrollPane(javaPanel));
        tabs.addTab("JavaScript", new JScrollPane(javaScriptPanel));
        tabs.addTab(".NET", new JScrollPane(dotNetPanel));
        tabs.addTab("Android", new JScrollPane(androidPanel));
        tabs.addTab("Unknown", unknownPanel);

        JPanel root = new JPanel(new BorderLayout());
        root.add(header(), BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(footer(), BorderLayout.SOUTH);
        setContentPane(root);
        FileDrop.install(root, new Consumer<File>() {
            @Override
            public void accept(File file) {
                openFile(file);
            }
        });

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1180, 820);
        setMinimumSize(new Dimension(960, 680));
        setLocationRelativeTo(null);
    }

    public void openFile(File file) {
        DispatchResult result = dispatcher.dispatch(file);
        frontPanel.showDetection(result);
        DetectedFileType type = result.getDetection().getType();
        if (type == DetectedFileType.JAVA) {
            tabs.setSelectedIndex(TAB_JAVA);
            javaPanel.acceptRoutedFile(file, result.getDetection());
        } else if (type == DetectedFileType.JAVASCRIPT) {
            tabs.setSelectedIndex(TAB_JAVASCRIPT);
            javaScriptPanel.acceptFile(file, result.getDetection(), true);
        } else if (type == DetectedFileType.DOTNET) {
            tabs.setSelectedIndex(TAB_DOTNET);
            dotNetPanel.acceptFile(file, result.getDetection(), true);
        } else if (type == DetectedFileType.ANDROID) {
            tabs.setSelectedIndex(TAB_ANDROID);
            androidPanel.acceptFile(file, result.getDetection(), true);
        } else {
            tabs.setSelectedIndex(TAB_UNKNOWN);
            unknownPanel.showFile(file, result.getDetection());
        }
    }

    public void selectTab(int index) {
        tabs.setSelectedIndex(index);
    }

    public int getTabCount() {
        return tabs.getTabCount();
    }

    public String getTabTitle(int index) {
        return tabs.getTitleAt(index);
    }

    public DeobfuscatorGUI getJavaPanel() {
        return javaPanel;
    }

    private JComponent header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(36, 41, 46));
        header.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel title = new JLabel("Universal Deobfuscator");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitle = new JLabel("Detect a file, open its tab, and deobfuscate only when a real tool produces a change.");
        subtitle.setForeground(new Color(180, 190, 205));
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(title);
        text.add(subtitle);
        header.add(text, BorderLayout.WEST);
        return header;
    }

    private JComponent footer() {
        JLabel footer = new JLabel(" Outputs go to ./out by default. The input file is never overwritten.");
        footer.setBorder(new EmptyBorder(4, 8, 4, 8));
        return footer;
    }

    public static void launch() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }
                new UniversalDeobfuscatorFrame().setVisible(true);
            }
        });
    }
}
