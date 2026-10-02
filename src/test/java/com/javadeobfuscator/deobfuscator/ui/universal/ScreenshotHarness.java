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

import com.javadeobfuscator.deobfuscator.samples.MadeUpSampleGenerator;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * Captures the universal window under a display (xvfb is enough).
 * Usage: java ... ScreenshotHarness /path/to/dir
 */
public final class ScreenshotHarness {
    private ScreenshotHarness() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: ScreenshotHarness <output-directory>");
        }
        File outputDir = new File(args[0]);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IllegalStateException("Could not create " + outputDir);
        }
        File samples = new File("samples/test-resources");
        MadeUpSampleGenerator.writeAll(samples);

        final UniversalDeobfuscatorFrame[] holder = new UniversalDeobfuscatorFrame[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }
                holder[0] = new UniversalDeobfuscatorFrame();
                holder[0].setLocation(0, 0);
                holder[0].setVisible(true);
            }
        });
        UniversalDeobfuscatorFrame frame = holder[0];
        Thread.sleep(800);
        capture(frame, new File(outputDir, "01-front.png"));

        for (int i = 0; i < frame.getTabCount(); i++) {
            final int index = i;
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    frame.selectTab(index);
                }
            });
            Thread.sleep(400);
            capture(frame, new File(outputDir, String.format("%02d-%s.png", i + 2, slug(frame.getTabTitle(i)))));
        }

        File jar = new File(samples, "made-up-obfuscated.jar");
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.openFile(jar);
            }
        });
        waitForResult(frame, 180000);
        Thread.sleep(500);
        capture(frame, new File(outputDir, "08-java-result.png"));

        openAndCapture(frame, new File(samples, "made-up-obfuscated.js"), new File(outputDir, "09-javascript-routed.png"));
        openAndCapture(frame, new File(samples, "made-up-dotnet.exe"), new File(outputDir, "10-dotnet-routed.png"));
        openAndCapture(frame, new File(samples, "made-up-android.apk"), new File(outputDir, "11-android-routed.png"));
        openAndCapture(frame, new File(samples, "made-up-unknown.bin"), new File(outputDir, "12-unknown-routed.png"));
        openAndCapture(frame, new File(samples, "made-up-native.exe"), new File(outputDir, "13-native-pe-unknown.png"));

        System.out.println("Java result status:");
        System.out.println(frame.getJavaPanel().getResultStatusText());
        System.exit(0);
    }

    private static void openAndCapture(final UniversalDeobfuscatorFrame frame, final File file, File image) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.openFile(file);
            }
        });
        Thread.sleep(700);
        capture(frame, image);
    }

    private static void waitForResult(UniversalDeobfuscatorFrame frame, long timeoutMs) throws InterruptedException, InvocationTargetException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            final String[] text = new String[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    text[0] = frame.getJavaPanel().getResultStatusText();
                }
            });
            if (text[0] != null && text[0].contains("Result:")) {
                return;
            }
            Thread.sleep(400);
        }
        throw new IllegalStateException("Timed out waiting for the Java result view.");
    }

    private static void capture(JFrame frame, File file) throws AWTException, java.io.IOException {
        Robot robot = new Robot();
        robot.waitForIdle();
        Rectangle bounds = new Rectangle(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight());
        BufferedImage image = robot.createScreenCapture(bounds);
        ImageIO.write(image, "png", file);
        System.out.println("Wrote " + file.getAbsolutePath());
    }

    private static String slug(String title) {
        return title.toLowerCase().replace('.', ' ').trim().replace(' ', '-');
    }
}
