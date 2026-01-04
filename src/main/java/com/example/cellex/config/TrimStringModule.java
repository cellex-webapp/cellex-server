package com.example.cellex.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

public class TrimStringModule extends SimpleModule {

    public TrimStringModule() {
        super("TrimStringModule", Version.unknownVersion());
        addDeserializer(String.class, new StdDeserializer<String>(String.class) {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                if (value == null) return null;
                String propName = p.getCurrentName();
                if (propName != null) {
                    String lower = propName.toLowerCase();
                    // Track fields that had leading/trailing spaces
                    if (!value.equals(value.trim())) {
                        TrimContext.mark(propName);
                    }
                    // Do not trim password-like fields to preserve intentional whitespace
                    if (lower.contains("password")) {
                        return value;
                    }
                }
                return value.trim();
            }
        });
    }
}
