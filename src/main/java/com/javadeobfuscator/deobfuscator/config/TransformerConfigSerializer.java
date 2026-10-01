/*
 * Copyright 2026 GooseyPrime / java-deobfuscator
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.javadeobfuscator.deobfuscator.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class TransformerConfigSerializer extends JsonSerializer<TransformerConfig> {
    private static final String PREFIX = "com.javadeobfuscator.deobfuscator.transformers.";

    @Override
    public void serialize(TransformerConfig value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String id = value.getImplementation().getName();
        if (id.startsWith(PREFIX)) {
            id = id.substring(PREFIX.length());
        }

        ObjectNode properties = new ObjectMapper().valueToTree(value);
        properties.remove("implementation");
        properties.remove("vmModifiers");

        gen.writeStartObject();
        gen.writeFieldName(id);
        gen.writeTree(properties);
        gen.writeEndObject();
    }
}
