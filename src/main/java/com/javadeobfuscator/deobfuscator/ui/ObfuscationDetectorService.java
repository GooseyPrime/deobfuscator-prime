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

import com.javadeobfuscator.deobfuscator.Deobfuscator;
import com.javadeobfuscator.deobfuscator.config.Configuration;
import com.javadeobfuscator.deobfuscator.rules.Rule;
import com.javadeobfuscator.deobfuscator.rules.Rules;
import com.javadeobfuscator.deobfuscator.transformers.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class ObfuscationDetectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObfuscationDetectorService.class);

    public static class DetectionResult {
        private final String ruleName;
        private final String description;
        private final String message;
        private final List<String> recommendedTransformerIds;

        public DetectionResult(String ruleName, String description, String message, List<String> recommendedTransformerIds) {
            this.ruleName = ruleName;
            this.description = description;
            this.message = message;
            this.recommendedTransformerIds = recommendedTransformerIds;
        }

        public String getRuleName() {
            return ruleName;
        }

        public String getDescription() {
            return description;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getRecommendedTransformerIds() {
            return recommendedTransformerIds;
        }
    }

    public static List<DetectionResult> detect(File inputJar, List<File> runtimePaths, List<File> libraries) throws Throwable {
        Configuration configuration = new Configuration();
        configuration.setInput(inputJar);
        configuration.setPath(runtimePaths != null ? runtimePaths : Collections.emptyList());
        configuration.setLibraries(libraries != null ? libraries : Collections.emptyList());
        configuration.setDetect(true);

        Deobfuscator deobfuscator = new Deobfuscator(configuration);
        // Load classpath and input
        deobfuscator.start();

        List<DetectionResult> results = new ArrayList<>();
        for (Rule rule : Rules.RULES) {
            try {
                String message = rule.test(deobfuscator);
                if (message != null) {
                    List<String> transformerIds = new ArrayList<>();
                    Collection<Class<? extends Transformer<?>>> rec = rule.getRecommendTransformers();
                    if (rec != null) {
                        for (Class<? extends Transformer<?>> clazz : rec) {
                            String name = clazz.getName();
                            String prefix = "com.javadeobfuscator.deobfuscator.transformers.";
                            if (name.startsWith(prefix)) {
                                transformerIds.add(name.substring(prefix.length()));
                            } else {
                                transformerIds.add(name);
                            }
                        }
                    }
                    results.add(new DetectionResult(rule.getClass().getSimpleName(), rule.getDescription(), message, transformerIds));
                }
            } catch (Exception ex) {
                LOGGER.warn("Detector rule {} failed", rule.getClass().getSimpleName(), ex);
            }
        }
        return results;
    }
}
