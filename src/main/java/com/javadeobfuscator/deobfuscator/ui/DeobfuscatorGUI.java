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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.javadeobfuscator.deobfuscator.Deobfuscator;
import com.javadeobfuscator.deobfuscator.config.Configuration;
import com.javadeobfuscator.deobfuscator.config.TransformerConfig;
import com.javadeobfuscator.deobfuscator.config.TransformerConfigDeserializer;
import com.javadeobfuscator.deobfuscator.config.TransformerConfigSerializer;
import com.javadeobfuscator.deobfuscator.exceptions.NoClassInPathException;
import com.javadeobfuscator.deobfuscator.exceptions.PreventableStackOverflowError;
import com.javadeobfuscator.deobfuscator.transformers.Transformer;
import com.javadeobfuscator.deobfuscator.transformers.normalizer.AbstractNormalizer;
import com.javadeobfuscator.deobfuscator.transformers.special.RadonConfig;
import com.javadeobfuscator.deobfuscator.detect.FileTypeDetection;
import com.javadeobfuscator.deobfuscator.tools.ClassFilePackager;
import com.javadeobfuscator.deobfuscator.tools.JobStatus;
import com.javadeobfuscator.deobfuscator.tools.OutputLocations;
import com.javadeobfuscator.deobfuscator.transformers.special.RadonTransformer;
import com.javadeobfuscator.deobfuscator.transformers.special.RadonTransformerV2;
import com.javadeobfuscator.deobfuscator.transformers.special.RadonV2Config;
import com.javadeobfuscator.deobfuscator.ui.universal.BytecodeDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/**
 * Modern, intuitive Graphical User Interface for Java Deobfuscator.
 * Supports configuration of end-user variables, secrets, classpath,
 * obfuscator detection, and real-time execution monitoring.
 */
public class DeobfuscatorGUI extends JPanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeobfuscatorGUI.class);

    // Tabbed pane
    private JTabbedPane tabbedPane;

    // --- Tab 1: Target Files & Quick Start ---
    private JTextField inputJarField;
    private JTextField outputJarField;
    private JCheckBox verifyBox;
    private JCheckBox patchAsmBox;
    private JCheckBox smartRedoBox;
    private JCheckBox deleteUselessBox;
    private JCheckBox paramorphismBox;
    private JCheckBox paramorphismV2Box;
    private JButton detectObfuscatorBtn;
    private JLabel detectStatusLabel;

    // --- Tab 2: User Variables & Secrets ---
    private JTextField runtimePathField;
    private JButton autoDetectRuntimeBtn;
    private JLabel runtimeStatusLabel;
    private JTextField mappingFileField;
    private JComboBox<RadonConfig.FlowMode> radonFlowModeCombo;
    private JComboBox<RadonConfig.NumberMode> radonNumberModeCombo;
    private JComboBox<RadonConfig.StringPoolMode> radonStringPoolCombo;
    private JCheckBox radonIndyBox;
    private JCheckBox radonFastIndyBox;
    private JCheckBox radonStringBox;
    private JCheckBox radonTrashBox;
    private DefaultListModel<String> ignoredClassesModel;
    private JList<String> ignoredClassesList;
    private JTextField newIgnoredPatternField;

    // --- Tab 3: Classpath & Dependencies ---
    private DefaultListModel<File> librariesModel;
    private JList<File> librariesList;

    // --- Tab 4: Transformer Pipeline ---
    private JTextField transformerFilterField;
    private DefaultListModel<TransformerRegistry.TransformerItem> availableTransformersModel;
    private JList<TransformerRegistry.TransformerItem> availableTransformersList;
    private DefaultListModel<TransformerRegistry.TransformerItem> activeTransformersModel;
    private List<TransformerConfig> loadedTransformerConfigs;
    private JList<TransformerRegistry.TransformerItem> activeTransformersList;
    private JComboBox<String> presetCombo;

    // --- Tab 5: Console & Execution Log ---
    private JTextArea consoleArea;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton runBtn;
    private JButton stopBtn;

    // --- Tab 6: Result view ---
    private JTextArea resultStatusArea;
    private JTextArea originalViewArea;
    private JTextArea transformedViewArea;
    private boolean quietDialogs;
    private String routedTypeSummary = "";
    private String obfuscatorGuess = "not scanned";
    private Runnable runSettledCallback;

    // Threading / execution state
    private Thread runningThread;
    private PrintStream originalOut;
    private PrintStream originalErr;

    public DeobfuscatorGUI() {
        super(new BorderLayout(0, 5));
        setBorder(new EmptyBorder(6, 8, 6, 8));
        initUI();
    }

    private void initUI() {
        JPanel north = new JPanel(new BorderLayout());
        north.add(createMenuBar(), BorderLayout.NORTH);
        north.add(createHeaderPanel(), BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.BOLD, 12f));
        tabbedPane.addTab("Target Files", createTargetFilesTab());
        tabbedPane.addTab("Variables & Secrets", createVariablesAndSecretsTab());
        tabbedPane.addTab("Classpath & Libs", createClasspathTab());
        tabbedPane.addTab("Transformers", createTransformersTab());
        tabbedPane.addTab("Console Log", createConsoleTab());
        tabbedPane.addTab("Result", createResultTab());
        add(tabbedPane, BorderLayout.CENTER);

        add(createBottomBar(), BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::performAutoDetectRuntimeQuietly);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("Reset to Defaults");
        newItem.addActionListener(e -> resetToDefaults());
        JMenuItem loadItem = new JMenuItem("Load Configuration YAML...");
        loadItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
        loadItem.addActionListener(e -> openConfigFileChooser());
        JMenuItem saveItem = new JMenuItem("Save Configuration YAML...");
        saveItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
        saveItem.addActionListener(e -> saveConfigFileChooser());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newItem);
        fileMenu.addSeparator();
        fileMenu.add(loadItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // Presets Menu
        JMenu presetsMenu = new JMenu("Presets");
        for (String presetName : TransformerRegistry.getPresets().keySet()) {
            JMenuItem presetItem = new JMenuItem(presetName);
            presetItem.addActionListener(e -> applyPreset(presetName));
            presetsMenu.add(presetItem);
        }
        menuBar.add(presetsMenu);

        // Tools Menu
        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem detectRuntimeItem = new JMenuItem("Auto-Detect Java Runtime");
        detectRuntimeItem.addActionListener(e -> performAutoDetectRuntime());
        JMenuItem detectObfuscatorItem = new JMenuItem("Detect Obfuscators on Input JAR");
        detectObfuscatorItem.addActionListener(e -> performObfuscatorDetection());
        toolsMenu.add(detectRuntimeItem);
        toolsMenu.add(detectObfuscatorItem);
        menuBar.add(toolsMenu);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpGuideItem = new JMenuItem("Secrets & Variables Guide");
        helpGuideItem.addActionListener(e -> showSecretsGuideDialog());
        JMenuItem aboutItem = new JMenuItem("About Java Deobfuscator");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(helpGuideItem);
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(36, 41, 46));
        header.setBorder(new EmptyBorder(10, 15, 10, 15));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        JLabel titleLabel = new JLabel("Java Deobfuscator");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(Color.WHITE);
        JLabel subtitleLabel = new JLabel("Bytecode Recovery, String Decryption & Control Flow Normalization");
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        subtitleLabel.setForeground(new Color(180, 190, 205));
        titles.add(titleLabel);
        titles.add(subtitleLabel);
        header.add(titles, BorderLayout.WEST);

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        headerButtons.setOpaque(false);

        JButton loadBtn = new JButton("📂 Load Config");
        loadBtn.addActionListener(e -> openConfigFileChooser());
        JButton saveBtn = new JButton("💾 Save Config");
        saveBtn.addActionListener(e -> saveConfigFileChooser());

        headerButtons.add(loadBtn);
        headerButtons.add(saveBtn);
        header.add(headerButtons, BorderLayout.EAST);

        return header;
    }

    // =========================================================================
    // Tab 1: Target Files & Quick Start
    // =========================================================================
    private JPanel createTargetFilesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        // Input JAR
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        topGrid.add(new JLabel("Input Obfuscated JAR: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputJarField = new JTextField();
        topGrid.add(inputJarField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseInputBtn = new JButton("Browse...");
        browseInputBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Input Obfuscated JAR");
            chooser.setFileFilter(new FileNameExtensionFilter("Java Archive (*.jar, *.zip)", "jar", "zip"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                inputJarField.setText(selected.getAbsolutePath());
                // Auto-suggest output JAR if output is currently empty
                if (outputJarField.getText().trim().isEmpty()) {
                    outputJarField.setText(OutputLocations.jarOutput(selected).getAbsolutePath());
                }
            }
        });
        topGrid.add(browseInputBtn, gbc);

        // Output JAR
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        topGrid.add(new JLabel("Output Deobfuscated JAR: *"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        outputJarField = new JTextField();
        topGrid.add(outputJarField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseOutputBtn = new JButton("Browse...");
        browseOutputBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Destination Output JAR");
            chooser.setFileFilter(new FileNameExtensionFilter("Java Archive (*.jar)", "jar"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                String path = selected.getAbsolutePath();
                if (!path.toLowerCase().endsWith(".jar")) {
                    path += ".jar";
                }
                outputJarField.setText(path);
            }
        });
        topGrid.add(browseOutputBtn, gbc);

        panel.add(topGrid, BorderLayout.NORTH);

        // Center: Obfuscator Detection Panel + Options Panel
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));

        // Obfuscation Detector Section
        JPanel detectorBox = new JPanel(new BorderLayout(8, 8));
        detectorBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "🔍 Automated Obfuscator Detector",
                TitledBorder.LEFT, TitledBorder.TOP, detectorBox.getFont().deriveFont(Font.BOLD)));

        JPanel detectorTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        detectObfuscatorBtn = new JButton("Run Obfuscator Detection");
        detectObfuscatorBtn.setFont(detectObfuscatorBtn.getFont().deriveFont(Font.BOLD));
        detectObfuscatorBtn.addActionListener(e -> performObfuscatorDetection());
        detectorTop.add(detectObfuscatorBtn);

        detectStatusLabel = new JLabel("Click to scan input JAR for known obfuscators (Allatori, Zelix, Stringer, Dash-O, Smoke, etc.)");
        detectStatusLabel.setForeground(Color.GRAY);
        detectorTop.add(detectStatusLabel);
        detectorBox.add(detectorTop, BorderLayout.NORTH);

        JTextArea detectorHelp = new JTextArea(
                "Tip: The detector analyzes your input bytecode using signature rules and recommends the exact " +
                        "pipeline of transformers to apply. After scanning, you can automatically load the recommended transformers with one click."
        );
        detectorHelp.setEditable(false);
        detectorHelp.setLineWrap(true);
        detectorHelp.setWrapStyleWord(true);
        detectorHelp.setBackground(detectorBox.getBackground());
        detectorHelp.setBorder(new EmptyBorder(4, 10, 6, 10));
        detectorBox.add(detectorHelp, BorderLayout.CENTER);

        centerPanel.add(detectorBox);

        // General Engine Options
        JPanel optionsBox = new JPanel(new GridLayout(3, 2, 8, 4));
        optionsBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "General Engine Options",
                TitledBorder.LEFT, TitledBorder.TOP, optionsBox.getFont().deriveFont(Font.BOLD)));

        verifyBox = new JCheckBox("Verify Bytecode with CheckClassAdapter (verify)", true);
        verifyBox.setToolTipText("Validates output class bytecode structure against JVM specification.");

        patchAsmBox = new JCheckBox("Patch ASM Exploits (patchAsm)", false);
        patchAsmBox.setToolTipText("Enables Cafedude stripping of invalid attributes designed to crash ASM.");

        smartRedoBox = new JCheckBox("Smart Redo (smartRedo)", false);
        smartRedoBox.setToolTipText("Reruns transformers when inter-dependent obfuscations are detected.");

        deleteUselessBox = new JCheckBox("Delete Useless Junk Classes (deleteUselessClasses)", false);
        deleteUselessBox.setToolTipText("Dumps dead or dummy classes inserted as distractions.");

        paramorphismBox = new JCheckBox("Paramorphism Mode (paramorphism)", false);
        paramorphismBox.setToolTipText("Filters duplicate fake class files used by Paramorphism.");

        paramorphismV2Box = new JCheckBox("Paramorphism V2 Mode (paramorphismV2)", false);
        paramorphismV2Box.setToolTipText("Handles .class/ directory trick used by Paramorphism v2.");

        optionsBox.add(verifyBox);
        optionsBox.add(patchAsmBox);
        optionsBox.add(smartRedoBox);
        optionsBox.add(deleteUselessBox);
        optionsBox.add(paramorphismBox);
        optionsBox.add(paramorphismV2Box);

        centerPanel.add(optionsBox);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Tab 2: User Variables & Secrets
    // =========================================================================
    private JPanel createVariablesAndSecretsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top info header
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(235, 245, 255));
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 210, 240)),
                new EmptyBorder(8, 10, 8, 10)
        ));
        JLabel bannerTitle = new JLabel("🔑 User-Specified Variables & Environment Secrets");
        bannerTitle.setFont(bannerTitle.getFont().deriveFont(Font.BOLD, 13f));
        bannerTitle.setForeground(new Color(10, 60, 130));
        JLabel bannerText = new JLabel("<html>Deobfuscation depends on user-provided paths and secrets: " +
                "<b>Java Runtime (rt.jar / jmods)</b> to resolve class inheritance, <b>Mapping Files</b> for renamers, and " +
                "<b>Decryption Parameters</b> for specific obfuscators. Configure them below before running.</html>");
        banner.add(bannerTitle, BorderLayout.NORTH);
        banner.add(bannerText, BorderLayout.CENTER);
        panel.add(banner, BorderLayout.NORTH);

        // Center content in a scrollable panel
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // --- 1. Java Runtime Library (rt.jar / jmods) ---
        JPanel runtimeGroup = new JPanel(new BorderLayout(6, 6));
        runtimeGroup.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "1. Java Runtime Library Paths (rt.jar / jmods) *",
                TitledBorder.LEFT, TitledBorder.TOP, runtimeGroup.getFont().deriveFont(Font.BOLD)));

        JPanel runtimeInput = new JPanel(new BorderLayout(6, 4));
        runtimePathField = new JTextField();
        runtimeInput.add(runtimePathField, BorderLayout.CENTER);

        JPanel runtimeBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        autoDetectRuntimeBtn = new JButton("🔍 Auto-Detect");
        autoDetectRuntimeBtn.addActionListener(e -> performAutoDetectRuntime());
        JButton browseRuntimeBtn = new JButton("Browse...");
        browseRuntimeBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select rt.jar or jmods Directory");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                runtimePathField.setText(chooser.getSelectedFile().getAbsolutePath());
                updateRuntimeStatus();
            }
        });
        runtimeBtns.add(autoDetectRuntimeBtn);
        runtimeBtns.add(browseRuntimeBtn);
        runtimeInput.add(runtimeBtns, BorderLayout.EAST);
        runtimeGroup.add(runtimeInput, BorderLayout.NORTH);

        runtimeStatusLabel = new JLabel("Status: Click 'Auto-Detect' to locate your Java runtime automatically.");
        runtimeStatusLabel.setFont(runtimeStatusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        runtimeGroup.add(runtimeStatusLabel, BorderLayout.CENTER);

        JTextArea runtimeHelp = new JTextArea(
                "Where and how to get it:\n" +
                        "• Java 8: Located at <JDK_PATH>/jre/lib/rt.jar (Windows: C:\\Program Files\\Java\\jdk1.8.0_*\\jre\\lib\\rt.jar, Linux: /usr/lib/jvm/java-8-openjdk/jre/lib/rt.jar).\n" +
                        "• Modern Java (9, 11, 17, 21+): Located at <JDK_PATH>/jmods directory (containing java.base.jmod).\n" +
                        "• Why needed: Java Deobfuscator requires standard Java class bytecode to calculate type inheritance and frame stacks. Without it, a NoClassInPathException will be thrown.\n" +
                        "• Separate multiple runtime paths with '" + File.pathSeparator + "'."
        );
        runtimeHelp.setEditable(false);
        runtimeHelp.setBackground(runtimeGroup.getBackground());
        runtimeHelp.setBorder(new EmptyBorder(4, 6, 4, 6));
        runtimeHelp.setFont(runtimeHelp.getFont().deriveFont(11f));
        runtimeGroup.add(runtimeHelp, BorderLayout.SOUTH);

        content.add(runtimeGroup);
        content.add(Box.createVerticalStrut(8));

        // --- 2. Remapping / Mapping File ---
        JPanel mappingGroup = new JPanel(new BorderLayout(6, 6));
        mappingGroup.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "2. Remapping & Mapping File (Mapping Dictionary / Secrets)",
                TitledBorder.LEFT, TitledBorder.TOP, mappingGroup.getFont().deriveFont(Font.BOLD)));

        JPanel mappingInput = new JPanel(new BorderLayout(6, 4));
        mappingFileField = new JTextField();
        mappingInput.add(mappingFileField, BorderLayout.CENTER);

        JButton browseMappingBtn = new JButton("Browse...");
        browseMappingBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Mapping File (mappings.txt / mappings.srg / etc.)");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                mappingFileField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        mappingInput.add(browseMappingBtn, BorderLayout.EAST);
        mappingGroup.add(mappingInput, BorderLayout.NORTH);

        JTextArea mappingHelp = new JTextArea(
                "Where and how to get it:\n" +
                        "• Normalizers (ClassNormalizer, MethodNormalizer, FieldNormalizer): Mapping file format maps original obfuscated names to clean names.\n" +
                        "• Minecraft: Notch-to-SRG, SRG-to-MCP, or Intermediary-to-Yarn mapping files from FabricMC or MCPbot.\n" +
                        "• If specified, remappers write out the mapping dictionary or read predefined remappings from this file."
        );
        mappingHelp.setEditable(false);
        mappingHelp.setBackground(mappingGroup.getBackground());
        mappingHelp.setBorder(new EmptyBorder(4, 6, 4, 6));
        mappingHelp.setFont(mappingHelp.getFont().deriveFont(11f));
        mappingGroup.add(mappingHelp, BorderLayout.SOUTH);

        content.add(mappingGroup);
        content.add(Box.createVerticalStrut(8));

        // --- 3. Radon & Specific Obfuscator Secrets ---
        JPanel radonGroup = new JPanel(new GridLayout(2, 4, 8, 6));
        radonGroup.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "3. Radon & Obfuscator Specific Parameters",
                TitledBorder.LEFT, TitledBorder.TOP, radonGroup.getFont().deriveFont(Font.BOLD)));

        radonGroup.add(new JLabel("Flow Mode:"));
        radonFlowModeCombo = new JComboBox<>(RadonConfig.FlowMode.values());
        radonFlowModeCombo.setSelectedItem(RadonConfig.FlowMode.COMBINED);
        radonGroup.add(radonFlowModeCombo);

        radonGroup.add(new JLabel("Number Mode:"));
        radonNumberModeCombo = new JComboBox<>(RadonConfig.NumberMode.values());
        radonNumberModeCombo.setSelectedItem(RadonConfig.NumberMode.NEW);
        radonGroup.add(radonNumberModeCombo);

        radonGroup.add(new JLabel("String Pool:"));
        radonStringPoolCombo = new JComboBox<>(RadonConfig.StringPoolMode.values());
        radonStringPoolCombo.setSelectedItem(RadonConfig.StringPoolMode.NEW);
        radonGroup.add(radonStringPoolCombo);

        JPanel radonChecks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        radonIndyBox = new JCheckBox("Indy", true);
        radonFastIndyBox = new JCheckBox("FastIndy", true);
        radonStringBox = new JCheckBox("Strings", true);
        radonTrashBox = new JCheckBox("Trash", false);
        radonChecks.add(radonIndyBox);
        radonChecks.add(radonFastIndyBox);
        radonChecks.add(radonStringBox);
        radonChecks.add(radonTrashBox);
        radonGroup.add(radonChecks);

        content.add(radonGroup);
        content.add(Box.createVerticalStrut(8));

        // --- 4. Ignored Classes (Exclusion Regex) ---
        JPanel ignoredGroup = new JPanel(new BorderLayout(6, 6));
        ignoredGroup.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "4. Ignored Classes & Packages (Regex Filter)",
                TitledBorder.LEFT, TitledBorder.TOP, ignoredGroup.getFont().deriveFont(Font.BOLD)));

        ignoredClassesModel = new DefaultListModel<>();
        ignoredClassesList = new JList<>(ignoredClassesModel);
        ignoredClassesList.setVisibleRowCount(3);
        JScrollPane ignoredScroll = new JScrollPane(ignoredClassesList);
        ignoredGroup.add(ignoredScroll, BorderLayout.CENTER);

        JPanel ignoredControls = new JPanel(new BorderLayout(4, 0));
        newIgnoredPatternField = new JTextField();
        newIgnoredPatternField.setToolTipText("e.g. ^org/apache/.* or ^com/google/.*");
        ignoredControls.add(newIgnoredPatternField, BorderLayout.CENTER);

        JPanel ignoredBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton addPatternBtn = new JButton("Add Pattern");
        addPatternBtn.addActionListener(e -> {
            String text = newIgnoredPatternField.getText().trim();
            if (!text.isEmpty()) {
                ignoredClassesModel.addElement(text);
                newIgnoredPatternField.setText("");
            }
        });
        JButton removePatternBtn = new JButton("Remove");
        removePatternBtn.addActionListener(e -> {
            int idx = ignoredClassesList.getSelectedIndex();
            if (idx != -1) {
                ignoredClassesModel.remove(idx);
            }
        });
        ignoredBtns.add(addPatternBtn);
        ignoredBtns.add(removePatternBtn);
        ignoredControls.add(ignoredBtns, BorderLayout.EAST);
        ignoredGroup.add(ignoredControls, BorderLayout.SOUTH);

        content.add(ignoredGroup);
        content.add(Box.createVerticalStrut(8));

        // --- 5. Recommended JVM Settings ---
        JPanel jvmGroup = new JPanel(new BorderLayout(4, 4));
        jvmGroup.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "5. Recommended JVM Settings (-Xss128m -Xmx4G)",
                TitledBorder.LEFT, TitledBorder.TOP, jvmGroup.getFont().deriveFont(Font.BOLD)));
        JLabel jvmLabel = new JLabel("<html><b>Crucial:</b> Control-flow and Peephole deobfuscators use deep recursion. " +
                "Always run Java Deobfuscator with <code>-Xss128m</code> to prevent <code>PreventableStackOverflowError</code>, " +
                "and at least <code>-Xmx2G</code> for memory. (Included automatically when using <code>run.sh</code> / <code>run.bat</code>).</html>");
        jvmGroup.add(jvmLabel, BorderLayout.CENTER);
        content.add(jvmGroup);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Tab 3: Classpath & Dependencies
    // =========================================================================
    private JPanel createClasspathTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel info = new JLabel("<html><b>External Classpath Libraries:</b> If your obfuscated JAR uses third-party libraries " +
                "(e.g., Minecraft, Bukkit, Spring, Netty, Android SDK), add them here so class hierarchies and method descriptors can be fully resolved.</html>");
        panel.add(info, BorderLayout.NORTH);

        librariesModel = new DefaultListModel<>();
        librariesList = new JList<>(librariesModel);
        panel.add(new JScrollPane(librariesList), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton addJarBtn = new JButton("Add JAR(s)...");
        addJarBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Dependency JAR(s)");
            chooser.setMultiSelectionEnabled(true);
            chooser.setFileFilter(new FileNameExtensionFilter("Java Archive (*.jar)", "jar"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                for (File f : chooser.getSelectedFiles()) {
                    librariesModel.addElement(f);
                }
            }
        });

        JButton addFolderBtn = new JButton("Add Folder...");
        addFolderBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Folder Containing Libraries");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                librariesModel.addElement(chooser.getSelectedFile());
            }
        });

        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.addActionListener(e -> {
            int[] indices = librariesList.getSelectedIndices();
            for (int i = indices.length - 1; i >= 0; i--) {
                librariesModel.remove(indices[i]);
            }
        });

        JButton clearBtn = new JButton("Clear All");
        clearBtn.addActionListener(e -> librariesModel.clear());

        btnPanel.add(addJarBtn);
        btnPanel.add(addFolderBtn);
        btnPanel.add(removeBtn);
        btnPanel.add(clearBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // Tab 4: Transformer Pipeline
    // =========================================================================
    private JPanel createTransformersTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top: Preset selector
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        presetPanel.add(new JLabel("Apply Quick Preset:"));
        presetCombo = new JComboBox<>(TransformerRegistry.getPresets().keySet().toArray(new String[0]));
        JButton applyPresetBtn = new JButton("Apply Preset");
        applyPresetBtn.addActionListener(e -> {
            String selected = (String) presetCombo.getSelectedItem();
            if (selected != null) {
                applyPreset(selected);
            }
        });
        presetPanel.add(presetCombo);
        presetPanel.add(applyPresetBtn);
        panel.add(presetPanel, BorderLayout.NORTH);

        // Center: Two lists with transfer controls
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 12, 0));

        // Left: Available transformers
        JPanel leftPanel = new JPanel(new BorderLayout(6, 6));
        leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Available Transformers",
                TitledBorder.LEFT, TitledBorder.TOP, leftPanel.getFont().deriveFont(Font.BOLD)));

        JPanel filterPanel = new JPanel(new BorderLayout(4, 0));
        filterPanel.add(new JLabel("Search:"), BorderLayout.WEST);
        transformerFilterField = new JTextField();
        transformerFilterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTransformers(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTransformers(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTransformers(); }
        });
        filterPanel.add(transformerFilterField, BorderLayout.CENTER);
        leftPanel.add(filterPanel, BorderLayout.NORTH);

        availableTransformersModel = new DefaultListModel<>();
        availableTransformersList = new JList<>(availableTransformersModel);
        leftPanel.add(new JScrollPane(availableTransformersList), BorderLayout.CENTER);

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton addBtn = new JButton("Add to Pipeline >>");
        addBtn.setFont(addBtn.getFont().deriveFont(Font.BOLD));
        addBtn.addActionListener(e -> {
            List<TransformerRegistry.TransformerItem> selected = availableTransformersList.getSelectedValuesList();
            for (TransformerRegistry.TransformerItem item : selected) {
                activeTransformersModel.addElement(item);
            }
        });
        leftBtns.add(addBtn);
        leftPanel.add(leftBtns, BorderLayout.SOUTH);

        centerPanel.add(leftPanel);

        // Right: Active Pipeline
        JPanel rightPanel = new JPanel(new BorderLayout(6, 6));
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Active Transformation Pipeline (Runs Sequentially)",
                TitledBorder.LEFT, TitledBorder.TOP, rightPanel.getFont().deriveFont(Font.BOLD)));

        activeTransformersModel = new DefaultListModel<>();
        activeTransformersList = new JList<>(activeTransformersModel);
        rightPanel.add(new JScrollPane(activeTransformersList), BorderLayout.CENTER);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton moveUpBtn = new JButton("▲ Move Up");
        moveUpBtn.addActionListener(e -> moveActiveTransformer(-1));
        JButton moveDownBtn = new JButton("▼ Move Down");
        moveDownBtn.addActionListener(e -> moveActiveTransformer(1));
        JButton removeBtn = new JButton("<< Remove");
        removeBtn.addActionListener(e -> {
            int[] indices = activeTransformersList.getSelectedIndices();
            for (int i = indices.length - 1; i >= 0; i--) {
                activeTransformersModel.remove(indices[i]);
            }
        });
        JButton clearActiveBtn = new JButton("Clear All");
        clearActiveBtn.addActionListener(e -> activeTransformersModel.clear());

        rightBtns.add(moveUpBtn);
        rightBtns.add(moveDownBtn);
        rightBtns.add(removeBtn);
        rightBtns.add(clearActiveBtn);
        rightPanel.add(rightBtns, BorderLayout.SOUTH);

        centerPanel.add(rightPanel);

        panel.add(centerPanel, BorderLayout.CENTER);

        // Populate available transformers initially
        filterTransformers();

        return panel;
    }

    private void filterTransformers() {
        String filter = transformerFilterField != null ? transformerFilterField.getText().trim().toLowerCase() : "";
        availableTransformersModel.clear();
        for (TransformerRegistry.TransformerItem item : TransformerRegistry.getAllTransformers()) {
            if (filter.isEmpty() ||
                    item.getName().toLowerCase().contains(filter) ||
                    item.getCategory().toLowerCase().contains(filter) ||
                    item.getId().toLowerCase().contains(filter)) {
                availableTransformersModel.addElement(item);
            }
        }
    }

    private void moveActiveTransformer(int direction) {
        int index = activeTransformersList.getSelectedIndex();
        if (index == -1) return;
        int target = index + direction;
        if (target < 0 || target >= activeTransformersModel.getSize()) return;

        TransformerRegistry.TransformerItem item = activeTransformersModel.remove(index);
        activeTransformersModel.add(target, item);
        activeTransformersList.setSelectedIndex(target);
    }

    public void applyPreset(String presetName) {
        List<String> ids = TransformerRegistry.getPresets().get(presetName);
        if (ids == null) return;
        activeTransformersModel.clear();
        for (String id : ids) {
            activeTransformersModel.addElement(TransformerRegistry.findById(id));
        }
    }

    // =========================================================================
    // Tab 5: Console & Execution Log
    // =========================================================================
    private JPanel createConsoleTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setBackground(new Color(24, 24, 24));
        consoleArea.setForeground(new Color(220, 220, 220));
        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleArea.setCaretColor(Color.WHITE);
        JScrollPane scroll = new JScrollPane(consoleArea);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        JButton clearBtn = new JButton("Clear Console");
        clearBtn.addActionListener(e -> consoleArea.setText(""));

        JButton copyBtn = new JButton("Copy All");
        copyBtn.addActionListener(e -> {
            String text = consoleArea.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            JOptionPane.showMessageDialog(this, "Console output copied to clipboard.", "Copied", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton saveLogBtn = new JButton("Save Log to File...");
        saveLogBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Log to File");
            chooser.setSelectedFile(new File("deobfuscator.log"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(chooser.getSelectedFile()), StandardCharsets.UTF_8)) {
                    writer.write(consoleArea.getText());
                    JOptionPane.showMessageDialog(this, "Log saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Failed to save log: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        toolBar.add(clearBtn);
        toolBar.add(copyBtn);
        toolBar.add(saveLogBtn);
        panel.add(toolBar, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // Bottom Action Bar
    // =========================================================================
    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 4));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                new EmptyBorder(6, 4, 4, 4)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(180, 20));
        progressBar.setVisible(false);
        left.add(progressBar);

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        left.add(statusLabel);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        stopBtn = new JButton("⏹ Cancel");
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> cancelDeobfuscation());

        runBtn = new JButton("▶ Run Deobfuscator");
        runBtn.setFont(runBtn.getFont().deriveFont(Font.BOLD, 13f));
        runBtn.setBackground(new Color(40, 167, 69));
        runBtn.setForeground(Color.WHITE);
        runBtn.addActionListener(e -> startDeobfuscation());

        right.add(stopBtn);
        right.add(runBtn);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    // =========================================================================
    // Auto-Detection & Helpers
    // =========================================================================
    private void performAutoDetectRuntimeQuietly() {
        File detected = RuntimeDetector.findRuntimeLibrary();
        if (detected != null && runtimePathField.getText().trim().isEmpty()) {
            runtimePathField.setText(detected.getAbsolutePath());
            updateRuntimeStatus();
        }
    }

    private void performAutoDetectRuntime() {
        File detected = RuntimeDetector.findRuntimeLibrary();
        if (detected != null) {
            runtimePathField.setText(detected.getAbsolutePath());
            updateRuntimeStatus();
            JOptionPane.showMessageDialog(this,
                    "Found Java Runtime:\n" + RuntimeDetector.describeRuntime(detected),
                    "Runtime Auto-Detected", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Could not automatically locate rt.jar or jmods directory.\n" +
                            "Please manually select rt.jar (Java 8) or the jmods folder (Java 9+) using the 'Browse...' button.",
                    "Runtime Not Found", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateRuntimeStatus() {
        String path = runtimePathField.getText().trim();
        if (path.isEmpty()) {
            runtimeStatusLabel.setText("Status: No runtime specified.");
            runtimeStatusLabel.setForeground(Color.RED);
        } else {
            File f = new File(path);
            if (f.exists()) {
                runtimeStatusLabel.setText("✓ " + RuntimeDetector.describeRuntime(f));
                runtimeStatusLabel.setForeground(new Color(0, 128, 0));
            } else {
                runtimeStatusLabel.setText("✗ Specified path does not exist!");
                runtimeStatusLabel.setForeground(Color.RED);
            }
        }
    }

    private void performObfuscatorDetection() {
        String inputPath = inputJarField.getText().trim();
        if (inputPath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please specify an Input Obfuscated JAR before running detection.",
                    "Input JAR Missing", JOptionPane.WARNING_MESSAGE);
            tabbedPane.setSelectedIndex(0);
            return;
        }

        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            JOptionPane.showMessageDialog(this,
                    "The specified Input JAR does not exist: " + inputPath,
                    "File Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        detectStatusLabel.setText("Scanning input JAR for obfuscators...");
        detectObfuscatorBtn.setEnabled(false);

        new SwingWorker<List<ObfuscationDetectorService.DetectionResult>, Void>() {
            @Override
            protected List<ObfuscationDetectorService.DetectionResult> doInBackground() throws Exception {
                List<File> runtimePaths = getRuntimePaths();
                List<File> libraries = getLibraries();
                try {
                    return ObfuscationDetectorService.detect(inputFile, runtimePaths, libraries);
                } catch (Throwable t) {
                    throw new Exception(t.getMessage(), t);
                }
            }

            @Override
            protected void done() {
                detectObfuscatorBtn.setEnabled(true);
                detectStatusLabel.setText("Detection scan complete.");
                try {
                    List<ObfuscationDetectorService.DetectionResult> results = get();
                    if (results.isEmpty()) {
                        obfuscatorGuess = "none detected";
                        JOptionPane.showMessageDialog(DeobfuscatorGUI.this,
                                "No known obfuscators were detected on this file.\n" +
                                        "(Note: Name obfuscation alone without string/flow encryption may not trigger detectors).",
                                "Detection Results", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Detected ").append(results.size()).append(" obfuscator signatures:\n\n");
                        Set<String> recommendedIds = new LinkedHashSet<>();
                        StringBuilder guess = new StringBuilder();
                        for (ObfuscationDetectorService.DetectionResult res : results) {
                            if (guess.length() > 0) {
                                guess.append(", ");
                            }
                            guess.append(res.getRuleName());
                            sb.append("• ").append(res.getRuleName()).append(": ").append(res.getDescription()).append("\n");
                            sb.append("  Details: ").append(res.getMessage()).append("\n\n");
                            recommendedIds.addAll(res.getRecommendedTransformerIds());
                        }
                        obfuscatorGuess = guess.length() == 0 ? "none detected" : guess.toString();

                        if (!recommendedIds.isEmpty()) {
                            sb.append("Recommended Transformers to apply:\n");
                            for (String id : recommendedIds) {
                                sb.append("  → ").append(id).append("\n");
                            }
                            sb.append("\nWould you like to automatically configure these recommended transformers in the pipeline?");
                            int opt = JOptionPane.showConfirmDialog(DeobfuscatorGUI.this,
                                    sb.toString(), "Obfuscators Detected", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (opt == JOptionPane.YES_OPTION) {
                                activeTransformersModel.clear();
                                for (String id : recommendedIds) {
                                    activeTransformersModel.addElement(TransformerRegistry.findById(id));
                                }
                                tabbedPane.setSelectedIndex(3); // Switch to transformers tab
                            }
                        } else {
                            JOptionPane.showMessageDialog(DeobfuscatorGUI.this, sb.toString(), "Detection Results", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DeobfuscatorGUI.this,
                            "Detection failed: " + ex.getMessage(), "Detection Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // =========================================================================
    // Execution: Start / Stop
    // =========================================================================
    private void startDeobfuscation() {
        String inputPath = inputJarField.getText().trim();
        String outputPath = outputJarField.getText().trim();

        if (inputPath.isEmpty()) {
            notifyUser("Please select an Input JAR file.", "Missing Input", JOptionPane.WARNING_MESSAGE);
            tabbedPane.setSelectedIndex(0);
            fireSettled();
            return;
        }
        if (!new File(inputPath).exists()) {
            notifyUser("Input JAR does not exist: " + inputPath, "File Not Found", JOptionPane.ERROR_MESSAGE);
            tabbedPane.setSelectedIndex(0);
            fireSettled();
            return;
        }
        if (outputPath.isEmpty()) {
            notifyUser("Please select an Output JAR destination.", "Missing Output", JOptionPane.WARNING_MESSAGE);
            tabbedPane.setSelectedIndex(0);
            fireSettled();
            return;
        }
        if (OutputLocations.sameFile(new File(inputPath), new File(outputPath))) {
            notifyUser("Refusing to overwrite the input file. Choose an output path under the out folder.",
                    "Output", JOptionPane.WARNING_MESSAGE);
            fireSettled();
            return;
        }
        File outputParent = new File(outputPath).getParentFile();
        if (outputParent != null && !outputParent.exists() && !outputParent.mkdirs()) {
            notifyUser("Could not create output directory: " + outputParent.getAbsolutePath(),
                    "Output", JOptionPane.ERROR_MESSAGE);
            fireSettled();
            return;
        }
        if (activeTransformersModel.isEmpty()) {
            notifyUser("Please add at least one transformer to the pipeline.", "No Transformers", JOptionPane.WARNING_MESSAGE);
            tabbedPane.setSelectedIndex(3);
            fireSettled();
            return;
        }

        // Warn if runtime is empty
        if (runtimePathField.getText().trim().isEmpty()) {
            if (quietDialogs) {
                performAutoDetectRuntimeQuietly();
            } else {
                int res = JOptionPane.showConfirmDialog(this,
                        "No Java Runtime path (rt.jar / jmods) is set!\n" +
                                "Java Deobfuscator requires runtime classes to resolve type hierarchies.\n" +
                                "Without this, a NoClassInPathException is very likely to occur.\n\n" +
                                "Would you like to Auto-Detect the runtime now?",
                        "Missing Runtime Path", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (res == JOptionPane.YES_OPTION) {
                    performAutoDetectRuntime();
                    if (runtimePathField.getText().trim().isEmpty()) {
                        fireSettled();
                        return;
                    }
                } else if (res == JOptionPane.CANCEL_OPTION) {
                    fireSettled();
                    return;
                }
            }
        }

        Configuration configuration = buildConfiguration();
        if (configuration.getOutput().exists()) {
            File backup = new File(configuration.getOutput().getParentFile(),
                    configuration.getOutput().getName() + ".bak");
            if (!configuration.getOutput().renameTo(backup)) {
                notifyUser("Unable to back up the existing output JAR.", "Output Error", JOptionPane.ERROR_MESSAGE);
                fireSettled();
                return;
            }
        }

        // Switch to console tab
        tabbedPane.setSelectedIndex(4);
        consoleArea.setText("");
        appendConsole("=== Starting Java Deobfuscator ===");
        appendConsole("Input: " + configuration.getInput().getAbsolutePath());
        appendConsole("Output: " + configuration.getOutput().getAbsolutePath());
        if (configuration.getPath() != null) {
            for (File p : configuration.getPath()) {
                appendConsole("Runtime: " + p.getAbsolutePath());
            }
        }
        appendConsole("Transformers: " + configuration.getTransformers().size());
        appendConsole("==================================\n");

        runBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Deobfuscating...");

        redirectSystemStreams();

        runningThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                Deobfuscator deobfuscator = new Deobfuscator(configuration);
                deobfuscator.start();

                long elapsed = System.currentTimeMillis() - start;
                SwingUtilities.invokeLater(() -> {
                    restoreSystemStreams();
                    progressBar.setVisible(false);
                    runBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    boolean changed = updateResultView(configuration.getInput(), configuration.getOutput(), null, true);
                    statusLabel.setText(changed
                            ? "Completed in " + (elapsed / 1000.0) + " s"
                            : "Finished with no bytecode change");
                    appendConsole("\n==================================");
                    appendConsole(changed
                            ? "Engine finished in " + (elapsed / 1000.0) + " seconds and the bytecode changed."
                            : "Engine finished in " + (elapsed / 1000.0) + " seconds but the bytecode matches the input. Not deobfuscated.");
                    appendConsole("Output saved to: " + configuration.getOutput().getAbsolutePath());
                    appendConsole("==================================");
                    notifyUser(changed
                                    ? "The Java engine finished and the bytecode changed.\nSaved to:\n" + configuration.getOutput().getAbsolutePath()
                                    : "The Java engine finished, but the bytecode matches the input. Not deobfuscated.\nOutput:\n" + configuration.getOutput().getAbsolutePath(),
                            "Complete", JOptionPane.INFORMATION_MESSAGE);
                    fireSettled();
                });
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                SwingUtilities.invokeLater(() -> {
                    restoreSystemStreams();
                    progressBar.setVisible(false);
                    runBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    statusLabel.setText("Cancelled");
                    appendConsole("\n[WARNING] Deobfuscation was cancelled by user.");
                    updateResultView(configuration.getInput(), configuration.getOutput(), "Cancelled before completion.", true);
                    fireSettled();
                });
            } catch (NoClassInPathException ex) {
                SwingUtilities.invokeLater(() -> {
                    restoreSystemStreams();
                    progressBar.setVisible(false);
                    runBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    statusLabel.setText("Failed: Class Not Found in Path");
                    appendConsole("\n[ERROR] NoClassInPathException: " + ex.getClassName());
                    appendConsole("The deobfuscator could not locate the class '" + ex.getClassName() + "'.");
                    appendConsole("To resolve this:\n" +
                            " 1. Go to 'Variables & Secrets' tab and ensure Java Runtime (rt.jar / jmods) is set.\n" +
                            " 2. If '" + ex.getClassName() + "' is from a library used by the app, add that library in the 'Classpath & Libs' tab.\n");
                    updateResultView(configuration.getInput(), configuration.getOutput(),
                            "Could not locate class: " + ex.getClassName(), true);
                    notifyUser("Could not locate class: " + ex.getClassName() + "\n\n" +
                                    "Please ensure:\n" +
                                    "1. The Java Runtime (rt.jar / jmods) is set in the 'Variables & Secrets' tab.\n" +
                                    "2. Any third-party dependency JARs are added in the 'Classpath & Libs' tab.",
                            "Class Missing in Path", JOptionPane.ERROR_MESSAGE);
                    fireSettled();
                });
            } catch (PreventableStackOverflowError ex) {
                SwingUtilities.invokeLater(() -> {
                    restoreSystemStreams();
                    progressBar.setVisible(false);
                    runBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    statusLabel.setText("Failed: StackOverflowError");
                    appendConsole("\n[ERROR] PreventableStackOverflowError: Recursion depth exceeded.");
                    appendConsole("Run the app with '-Xss128m' to increase thread stack size.");
                    updateResultView(configuration.getInput(), configuration.getOutput(), "StackOverflowError", true);
                    notifyUser("StackOverflowError occurred during deobfuscation.\n" +
                                    "Please relaunch with '-Xss128m' stack allocation.\n" +
                                    "(Use run.sh or run.bat which configures this automatically).",
                            "Stack Overflow", JOptionPane.ERROR_MESSAGE);
                    fireSettled();
                });
            } catch (Throwable t) {
                SwingUtilities.invokeLater(() -> {
                    restoreSystemStreams();
                    progressBar.setVisible(false);
                    runBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    statusLabel.setText("Failed with error");
                    appendConsole("\n[ERROR] Deobfuscation failed: " + t.getMessage());
                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));
                    appendConsole(sw.toString());
                    updateResultView(configuration.getInput(), configuration.getOutput(), t.getMessage(), true);
                    notifyUser("Deobfuscation failed:\n" + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    fireSettled();
                });
            }
        }, "Deobfuscator-Worker-Thread");
        runningThread.start();
    }

    private void cancelDeobfuscation() {
        if (runningThread != null && runningThread.isAlive()) {
            runningThread.interrupt();
            statusLabel.setText("Cancelling...");
            appendConsole("\n[WARNING] Cancellation requested; waiting for the worker to stop.");
        }
    }

    private void appendConsole(String message) {
        consoleArea.append(message + "\n");
        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
    }

    private void redirectSystemStreams() {
        originalOut = System.out;
        originalErr = System.err;

        OutputStream stream = new OutputStream() {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public synchronized void write(int b) {
                if (b == '\n') {
                    String line = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
                    buffer.reset();
                    SwingUtilities.invokeLater(() -> appendConsole(line));
                } else {
                    buffer.write(b);
                }
            }

            @Override
            public synchronized void flush() {
                if (buffer.size() > 0) {
                    String line = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
                    buffer.reset();
                    SwingUtilities.invokeLater(() -> appendConsole(line));
                }
            }
        };

        PrintStream printStream = new PrintStream(stream, true);
        System.setOut(printStream);
        System.setErr(printStream);
    }

    private void restoreSystemStreams() {
        if (originalOut != null) System.setOut(originalOut);
        if (originalErr != null) System.setErr(originalErr);
    }

    // =========================================================================
    // Configuration Mapping: GUI <-> Configuration
    // =========================================================================
    public Configuration buildConfiguration() {
        Configuration config = new Configuration();
        config.setInput(new File(inputJarField.getText().trim()));
        config.setOutput(new File(outputJarField.getText().trim()));

        // Runtime paths
        List<File> runtimePaths = getRuntimePaths();
        if (!runtimePaths.isEmpty()) {
            config.setPath(runtimePaths);
        }

        // Libraries
        List<File> libraries = getLibraries();
        if (!libraries.isEmpty()) {
            config.setLibraries(libraries);
        }

        // Ignored classes
        List<String> ignored = new ArrayList<>();
        for (int i = 0; i < ignoredClassesModel.getSize(); i++) {
            ignored.add(ignoredClassesModel.getElementAt(i));
        }
        if (!ignored.isEmpty()) {
            config.setIgnoredClasses(ignored);
        }

        // Flags
        config.setVerify(verifyBox.isSelected());
        config.setPatchAsm(patchAsmBox.isSelected());
        config.setSmartRedo(smartRedoBox.isSelected());
        config.setDeleteUselessClasses(deleteUselessBox.isSelected());
        config.setParamorphism(paramorphismBox.isSelected());
        config.setParamorphismV2(paramorphismV2Box.isSelected());

        // Transformers
        String mappingPath = mappingFileField.getText().trim();
        File mappingFile = mappingPath.isEmpty() ? null : new File(mappingPath);

        List<TransformerConfig> transformerConfigs = new ArrayList<>();
        boolean canReuseLoaded = loadedTransformerConfigs != null
                && loadedTransformerConfigs.size() == activeTransformersModel.getSize();
        if (canReuseLoaded) {
            for (int i = 0; i < activeTransformersModel.getSize(); i++) {
                String id = activeTransformersModel.getElementAt(i).getId();
                if (!transformerId(loadedTransformerConfigs.get(i)).equals(id)) {
                    canReuseLoaded = false;
                    break;
                }
            }
        }
        for (int i = 0; i < activeTransformersModel.getSize(); i++) {
            TransformerConfig tc = canReuseLoaded ? loadedTransformerConfigs.get(i)
                    : createTransformerConfig(activeTransformersModel.getElementAt(i).getId(), mappingFile);
            if (tc != null) {
                transformerConfigs.add(tc);
            }
        }
        config.setTransformers(transformerConfigs);

        return config;
    }

    private TransformerConfig createTransformerConfig(String id, File mappingFile) {
        String fullClass = id.startsWith("com.javadeobfuscator") ? id :
                "com.javadeobfuscator.deobfuscator.transformers." + id;
        try {
            Class<?> clazz = Class.forName(fullClass);
            if (!Transformer.class.isAssignableFrom(clazz)) {
                return null;
            }

            @SuppressWarnings("rawtypes")
            Class<? extends Transformer> transClazz = clazz.asSubclass(Transformer.class);

            // Special handling for Radon
            if (clazz == RadonTransformer.class) {
                RadonConfig rc = new RadonConfig();
                rc.setFlowMode((RadonConfig.FlowMode) radonFlowModeCombo.getSelectedItem());
                rc.setNumberMode((RadonConfig.NumberMode) radonNumberModeCombo.getSelectedItem());
                rc.setStringPoolMode((RadonConfig.StringPoolMode) radonStringPoolCombo.getSelectedItem());
                rc.setIndy(radonIndyBox.isSelected());
                rc.setFastIndy(radonFastIndyBox.isSelected());
                rc.setString(radonStringBox.isSelected());
                rc.setTrashClasses(radonTrashBox.isSelected());
                return rc;
            }
            if (clazz == RadonTransformerV2.class) {
                return new RadonV2Config();
            }

            TransformerConfig config = TransformerConfig.configFor(transClazz);
            if (config instanceof AbstractNormalizer.Config && mappingFile != null) {
                ((AbstractNormalizer.Config) config).setMappingFile(mappingFile);
            }
            return config;
        } catch (ClassNotFoundException e) {
            LOGGER.error("Transformer class not found: {}", fullClass, e);
            return null;
        }
    }

    private String transformerId(TransformerConfig config) {
        String name = config.getImplementation().getName();
        String prefix = "com.javadeobfuscator.deobfuscator.transformers.";
        return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
    }

    public void loadFromConfiguration(Configuration config) {
        if (config == null) return;

        if (config.getInput() != null) {
            inputJarField.setText(config.getInput().getAbsolutePath());
        }
        if (config.getOutput() != null) {
            outputJarField.setText(config.getOutput().getAbsolutePath());
        }

        if (config.getPath() != null && !config.getPath().isEmpty()) {
            StringJoiner paths = new StringJoiner(File.pathSeparator);
            for (File path : config.getPath()) {
                paths.add(path.getAbsolutePath());
            }
            runtimePathField.setText(paths.toString());
            updateRuntimeStatus();
        }

        librariesModel.clear();
        if (config.getLibraries() != null) {
            for (File lib : config.getLibraries()) {
                librariesModel.addElement(lib);
            }
        }

        ignoredClassesModel.clear();
        if (config.getIgnoredClasses() != null) {
            for (String ign : config.getIgnoredClasses()) {
                ignoredClassesModel.addElement(ign);
            }
        }

        verifyBox.setSelected(config.isVerify());
        patchAsmBox.setSelected(config.isPatchAsm());
        smartRedoBox.setSelected(config.isSmartRedo());
        deleteUselessBox.setSelected(config.isDeleteUselessClasses());
        paramorphismBox.setSelected(config.isParamorphism());
        paramorphismV2Box.setSelected(config.isParamorphismV2());

        activeTransformersModel.clear();
        loadedTransformerConfigs = config.getTransformers() == null
                ? null : new ArrayList<>(config.getTransformers());
        if (config.getTransformers() != null) {
            for (TransformerConfig tc : config.getTransformers()) {
                if (tc.getImplementation() != null) {
                    TransformerRegistry.TransformerItem item = TransformerRegistry.findById(transformerId(tc));
                    if (item != null) {
                        activeTransformersModel.addElement(item);
                    }
                }
                if (tc instanceof AbstractNormalizer.Config) {
                    File mf = ((AbstractNormalizer.Config) tc).getMappingFile();
                    if (mf != null) {
                        mappingFileField.setText(mf.getAbsolutePath());
                    }
                }
            }
        }
    }

    private List<File> getRuntimePaths() {
        List<File> list = new ArrayList<>();
        String path = runtimePathField.getText().trim();
        if (!path.isEmpty()) {
            for (String entry : path.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                String trimmed = entry.trim();
                if (!trimmed.isEmpty()) {
                    list.add(new File(trimmed));
                }
            }
        }
        return list;
    }

    private List<File> getLibraries() {
        List<File> list = new ArrayList<>();
        for (int i = 0; i < librariesModel.getSize(); i++) {
            list.add(librariesModel.getElementAt(i));
        }
        return list;
    }

    private void resetToDefaults() {
        inputJarField.setText("");
        outputJarField.setText("");
        mappingFileField.setText("");
        librariesModel.clear();
        ignoredClassesModel.clear();
        activeTransformersModel.clear();
        loadedTransformerConfigs = null;
        verifyBox.setSelected(true);
        patchAsmBox.setSelected(false);
        smartRedoBox.setSelected(false);
        deleteUselessBox.setSelected(false);
        paramorphismBox.setSelected(false);
        paramorphismV2Box.setSelected(false);
        performAutoDetectRuntimeQuietly();
    }

    private void openConfigFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Configuration YAML");
        chooser.setFileFilter(new FileNameExtensionFilter("YAML Configuration (*.yaml, *.yml)", "yaml", "yml"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                        .registerModule(new SimpleModule()
                                .addDeserializer(TransformerConfig.class, new TransformerConfigDeserializer(LOGGER))
                                .addSerializer(TransformerConfig.class, new TransformerConfigSerializer()));
                Configuration configuration = mapper.readValue(file, Configuration.class);
                loadFromConfiguration(configuration);
                JOptionPane.showMessageDialog(this, "Configuration loaded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to load config: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveConfigFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Configuration YAML");
        chooser.setSelectedFile(new File("config.yaml"));
        chooser.setFileFilter(new FileNameExtensionFilter("YAML Configuration (*.yaml, *.yml)", "yaml", "yml"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                Configuration config = buildConfiguration();
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                        .registerModule(new SimpleModule().addSerializer(TransformerConfig.class,
                                new TransformerConfigSerializer()));
                mapper.writeValue(file, config);
                JOptionPane.showMessageDialog(this, "Configuration saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save config: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showSecretsGuideDialog() {
        JTextArea area = new JTextArea();
        area.setText(
                "Java Deobfuscator - Secrets and Variables Guide\n" +
                        "================================================\n\n" +
                        "1. Java Runtime Library (rt.jar / jmods):\n" +
                        "   • What it is: The bytecode collection of the Java Standard Library classes.\n" +
                        "   • Why it's needed: Java Deobfuscator performs deep hierarchy analysis and stack frame\n" +
                        "     computation. Without runtime classes, NoClassInPathException will be thrown.\n" +
                        "   • How to get it:\n" +
                        "     - Java 8: Look in your JDK installation for 'jre/lib/rt.jar' or 'lib/rt.jar'.\n" +
                        "       Windows: C:\\Program Files\\Java\\jdk1.8.0_*\\jre\\lib\\rt.jar\n" +
                        "       Linux: /usr/lib/jvm/java-8-openjdk/jre/lib/rt.jar\n" +
                        "     - Java 9+: Point to the 'jmods' folder in your JDK directory ($JAVA_HOME/jmods).\n" +
                        "     - Shortcut: Click the 'Auto-Detect' button on the Variables tab!\n\n" +
                        "2. Remapping / Mapping Files:\n" +
                        "   • What it is: Key-value text files associating obfuscated member names (e.g. 'a', 'b')\n" +
                        "     with original or human-readable names (e.g. 'PlayerEntity', 'calculateScore').\n" +
                        "   • Transformers: Normalizer transformers (ClassNormalizer, MethodNormalizer, etc.)\n" +
                        "     and Minecraft transformers (SRG, MCP, Yarn).\n" +
                        "   • Input field: Set on the 'Variables & Secrets' tab under '2. Remapping & Mapping File'.\n\n" +
                        "3. Radon Obfuscator Parameters:\n" +
                        "   • Flow Mode, Number Mode, String Pool Mode, Indy / Fast Indy flags.\n" +
                        "   • Set on the 'Variables & Secrets' tab under '3. Radon Parameters'.\n\n" +
                        "4. JVM Memory & Stack Size:\n" +
                        "   • Control-flow deobfuscation requires -Xss128m to avoid PreventableStackOverflowError.\n" +
                        "   • Launch with: java -Xss128m -Xmx4G -jar deobfuscator.jar\n"
        );
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(680, 450));
        JOptionPane.showMessageDialog(this, scroll, "Secrets & Variables Guide", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "Java Deobfuscator\n" +
                        "Version 1.0.0 (Upgraded with ASM 9.7.1)\n\n" +
                        "An advanced, extensible Java bytecode deobfuscator supporting:\n" +
                        "• Allatori, Zelix KlassMaster, Stringer, Dash-O, Radon, Smoke, SkidSuite\n" +
                        "• Automated Obfuscation Signature Detection\n" +
                        "• Peephole Bytecode Simplification\n" +
                        "• Modern Java (8, 11, 17, 21+) and ASM 9.7.1 compatibility\n",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        com.javadeobfuscator.deobfuscator.ui.universal.UniversalDeobfuscatorFrame.launch();
    }

    /**
     * Called when the front door routes a Java file here. A raw class file is
     * packed into a new jar under out/ so the existing engine can read it.
     */
    public void acceptRoutedFile(File input, FileTypeDetection detection) {
        quietDialogs = true;
        routedTypeSummary = detection.getType().getDisplayName() + " — " + detection.getSummary();
        obfuscatorGuess = "not scanned";
        appendConsole("Routed file: " + input.getAbsolutePath());
        appendConsole(detection.explain());
        File engineInput = input;
        try {
            if (ClassFilePackager.looksLikeClass(input)) {
                engineInput = ClassFilePackager.packageAsJar(input);
                appendConsole("Packed the class file into " + engineInput.getAbsolutePath() + " without modifying the original.");
            }
        } catch (IOException ex) {
            notifyUser("Could not prepare the Java input: " + ex.getMessage(), "Input", JOptionPane.ERROR_MESSAGE);
            updateResultView(input, null, ex.getMessage(), false);
            fireSettled();
            return;
        }
        inputJarField.setText(engineInput.getAbsolutePath());
        outputJarField.setText(OutputLocations.jarOutput(engineInput).getAbsolutePath());
        if (activeTransformersModel.isEmpty()) {
            applyPreset("Peephole Cleanup & Optimization");
            presetCombo.setSelectedItem("Peephole Cleanup & Optimization");
            obfuscatorGuess = "not scanned; peephole cleanup preset applied";
        }
        performAutoDetectRuntimeQuietly();
        tabbedPane.setSelectedIndex(0);
        startDeobfuscation();
    }

    public void setRunSettledCallback(Runnable callback) {
        this.runSettledCallback = callback;
    }

    public String getResultStatusText() {
        return resultStatusArea == null ? "" : resultStatusArea.getText();
    }

    private JPanel createResultTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        resultStatusArea = new JTextArea(6, 40);
        resultStatusArea.setEditable(false);
        resultStatusArea.setLineWrap(true);
        resultStatusArea.setWrapStyleWord(true);
        resultStatusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultStatusArea.setText("No Java run yet.");
        JScrollPane statusScroll = new JScrollPane(resultStatusArea);
        statusScroll.setBorder(BorderFactory.createTitledBorder("Status"));

        originalViewArea = new JTextArea();
        originalViewArea.setEditable(false);
        originalViewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        transformedViewArea = new JTextArea();
        transformedViewArea.setEditable(false);
        transformedViewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane left = new JScrollPane(originalViewArea);
        left.setBorder(BorderFactory.createTitledBorder("Original bytecode"));
        JScrollPane right = new JScrollPane(transformedViewArea);
        right.setBorder(BorderFactory.createTitledBorder("Result bytecode"));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.5);

        panel.add(statusScroll, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private boolean updateResultView(File input, File output, String failure, boolean toolRan) {
        String original = "";
        String transformed = "";
        boolean changed = false;
        try {
            if (input != null && input.isFile()) {
                original = BytecodeDump.dump(input);
            }
        } catch (IOException ex) {
            original = "Could not read input: " + ex.getMessage();
        }
        boolean wroteOutput = output != null && output.isFile();
        if (failure == null && wroteOutput) {
            try {
                transformed = BytecodeDump.dump(output);
                changed = !BytecodeDump.sameBytecode(original, transformed);
            } catch (IOException ex) {
                failure = ex.getMessage();
                transformed = "Could not read output: " + ex.getMessage();
            }
        } else if (failure != null) {
            transformed = "No deobfuscated view.\n" + failure;
        } else {
            failure = "No output file was written.";
            transformed = failure;
        }
        if (originalViewArea != null) {
            originalViewArea.setText(original);
            originalViewArea.setCaretPosition(0);
            transformedViewArea.setText(transformed);
            transformedViewArea.setCaretPosition(0);
        }
        JobStatus status = new JobStatus(
                routedTypeSummary.isEmpty() ? "Java" : routedTypeSummary,
                obfuscatorGuess,
                "built-in Java deobfuscator (" + describeActiveTools() + ")",
                toolRan,
                toolRan && failure == null,
                changed,
                wroteOutput ? output : null,
                failure == null ? "" : failure
        );
        if (resultStatusArea != null) {
            resultStatusArea.setText(status.toDisplayString());
            resultStatusArea.setCaretPosition(0);
        }
        if (quietDialogs && tabbedPane != null) {
            tabbedPane.setSelectedIndex(5);
        }
        return changed;
    }

    private String describeActiveTools() {
        if (activeTransformersModel == null || activeTransformersModel.isEmpty()) {
            return "no transformers";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < activeTransformersModel.getSize(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(activeTransformersModel.getElementAt(i).getId());
        }
        return sb.toString();
    }

    private void notifyUser(String message, String title, int type) {
        appendConsole("[" + title + "] " + message.replace('\n', ' '));
        if (!quietDialogs) {
            JOptionPane.showMessageDialog(this, message, title, type);
        }
    }

    private void fireSettled() {
        Runnable callback = runSettledCallback;
        quietDialogs = false;
        if (callback != null) {
            callback.run();
        }
    }
}
