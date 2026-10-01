/*
 * Copyright 2016 Sam Sun <me@samczsun.com>
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.javadeobfuscator.deobfuscator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.javadeobfuscator.deobfuscator.config.Configuration;
import com.javadeobfuscator.deobfuscator.config.TransformerConfig;
import com.javadeobfuscator.deobfuscator.config.TransformerConfigDeserializer;
import com.javadeobfuscator.deobfuscator.exceptions.NoClassInPathException;
import com.javadeobfuscator.deobfuscator.exceptions.PreventableStackOverflowError;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;

public class DeobfuscatorMain {
    public static void main(String[] args) throws ClassNotFoundException {
        boolean isGui = (args.length == 0 || hasGuiFlag(args)) && !hasConfigFlag(args) && !hasHelpFlag(args);
        if (isGui && !GraphicsEnvironment.isHeadless()) {
            com.javadeobfuscator.deobfuscator.ui.DeobfuscatorGUI.main(args);
            return;
        }
        int code = run(args);
        System.exit(code);
    }

    private static boolean hasGuiFlag(String[] args) {
        for (String arg : args) {
            if ("-g".equals(arg) || "--gui".equals(arg)) return true;
        }
        return false;
    }

    private static boolean hasConfigFlag(String[] args) {
        for (String arg : args) {
            if ("-c".equals(arg) || "--config".equals(arg)) return true;
        }
        return false;
    }

    private static boolean hasHelpFlag(String[] args) {
        for (String arg : args) {
            if ("-h".equals(arg) || "--help".equals(arg)) return true;
        }
        return false;
    }

    public static int run(String[] args) throws ClassNotFoundException {
        Logger logger = LoggerFactory.getLogger(DeobfuscatorMain.class);

        Options options = new Options();
        options.addOption("c", "config", true, "The configuration file to use");
        options.addOption("g", "gui", false, "Launch the Graphical User Interface (GUI)");
        options.addOption("h", "help", false, "Print help and usage information");
        options.addOption("v", "version", false, "Print version information");

        if (args.length == 0) {
            if (!GraphicsEnvironment.isHeadless()) {
                com.javadeobfuscator.deobfuscator.ui.DeobfuscatorGUI.main(args);
                return 0;
            } else {
                printHelp(options);
                return 0;
            }
        }

        CommandLineParser cmdlineParser = new DefaultParser();
        CommandLine cmdLine;
        try {
            cmdLine = cmdlineParser.parse(options, args);
        } catch (ParseException e) {
            logger.error("An error occurred while parsing the commandline", e);
            return 1;
        }

        if (cmdLine.hasOption("help")) {
            printHelp(options);
            return 0;
        }

        if (cmdLine.hasOption("version")) {
            System.out.println("Java Deobfuscator v1.0.0 (ASM 9.7.1, Java 8-21+ JMOD Support)");
            return 0;
        }

        if (cmdLine.hasOption("gui") && !cmdLine.hasOption("config")) {
            if (GraphicsEnvironment.isHeadless()) {
                logger.error("Cannot launch GUI: No graphical display detected (headless environment). Use --config for CLI mode.");
                return 1;
            }
            com.javadeobfuscator.deobfuscator.ui.DeobfuscatorGUI.main(args);
            return 0;
        }

        if (!cmdLine.hasOption("config")) {
            logger.error("A config file must be specified (or use --gui / no args to start GUI, or --help for help)");
            printHelp(options);
            return 2;
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                .registerModule(
                        new SimpleModule().addDeserializer(TransformerConfig.class, new TransformerConfigDeserializer(logger))
                );

        Configuration configuration;
        try {
            configuration = mapper.readValue(new File(cmdLine.getOptionValue("config")), Configuration.class);
        } catch (IOException e) {
            logger.error("An error occurred while parsing the configuration file", e);
            return 3;
        }

        if (configuration.getInput() == null) {
            logger.error("An input JAR must be specified");
            return 4;
        }

        if (configuration.isDetect()) {
            return run(configuration);
        }

        if (configuration.getOutput() == null) {
            logger.error("An output JAR must be specified");
            return 5;
        }

        if (configuration.getOutput().exists()) {
            logger.warn("The specified output JAR already exists!");
            File parent = configuration.getOutput().getParentFile();
            if (!configuration.getOutput().renameTo(new File(parent, configuration.getOutput().getName() + ".bak"))) {
                logger.warn("I was unable to back up the previous output JAR");
            }
        }

        if (configuration.getTransformers() == null || configuration.getTransformers().size() == 0) {
            logger.error("At least one transformer must be specified");
            return 6;
        }

        return run(configuration);
    }

    private static int run(Configuration configuration) {
        Deobfuscator deobfuscator = new Deobfuscator(configuration);

        try {
            deobfuscator.start();
            return 0;
        } catch (NoClassInPathException ex) {
            for (int i = 0; i < 5; i++)
                System.out.println();
            System.out.println("** DO NOT OPEN AN ISSUE ON GITHUB **");
            System.out.println("Could not locate a class file.");
            System.out.println("Have you added the necessary files to the -path argument?");
            System.out.println("The error was:");
            ex.printStackTrace(System.out);
            return -2;
        } catch (PreventableStackOverflowError ex) {
            for (int i = 0; i < 5; i++)
                System.out.println();
            System.out.println("** DO NOT OPEN AN ISSUE ON GITHUB **");
            System.out.println("A StackOverflowError occurred during deobfuscation, but it is preventable");
            System.out.println("Try increasing your stack size using the -Xss flag");
            System.out.println("The error was:");
            ex.printStackTrace(System.out);
            return -3;
        } catch (Throwable t) {
            for (int i = 0; i < 5; i++)
                System.out.println();
            System.out.println("Deobfuscation failed. Please open a ticket on GitHub and provide the following error:");
            t.printStackTrace(System.out);
            return -1;
        }
    }

    private static void printHelp(Options options) {
        System.out.println("Java Deobfuscator (with ASM 9.7.1 & Modern JDK Support)");
        System.out.println("An easy-to-use, powerful Java bytecode deobfuscator.\n");
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar deobfuscator.jar [options]", options);
        System.out.println("\nModes of Operation:");
        System.out.println("  1. Graphical User Interface (GUI):");
        System.out.println("     java -jar deobfuscator.jar");
        System.out.println("     java -jar deobfuscator.jar --gui");
        System.out.println("     ./run.sh (Linux/macOS) or run.bat (Windows)");
        System.out.println();
        System.out.println("  2. Command Line Interface (CLI):");
        System.out.println("     java -Xss128m -jar deobfuscator.jar --config config.yaml");
        System.out.println();
        System.out.println("Required Variables & Environment Secrets:");
        System.out.println("  • Java Runtime Library (-path):");
        System.out.println("      Java 8:   rt.jar (e.g. $JAVA_HOME/jre/lib/rt.jar)");
        System.out.println("      Java 9+:  jmods (e.g. $JAVA_HOME/jmods/java.base.jmod)");
        System.out.println("      (The GUI auto-detects this across your installed JVMs!)");
        System.out.println("  • Third-party Dependencies (-libraries):");
        System.out.println("      Additional JAR libraries needed by your target application.");
        System.out.println("  • Mapping Files (for remappers/normalizers):");
        System.out.println("      Dictionaries or ProGuard/SRG mapping files when remapping names.");
    }
}
