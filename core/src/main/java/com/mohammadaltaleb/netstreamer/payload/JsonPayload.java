/*
 * Copyright 2021 The Netstreamer Authors.
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

package com.mohammadaltaleb.netstreamer.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mohammadaltaleb.netstreamer.util.ObjectUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A JSON implementation for {@link Payload} using Jackson library.
 */
public class JsonPayload implements Payload {
    private final ObjectNode json;
    private final Map<String, String> fields;

    /**
     * Create a new instance
     */
    public JsonPayload() {
        this.json = new ObjectMapper().createObjectNode();
        this.fields = new HashMap<String, String>();
    }

    /**
     * Create a new instance
     *
     * @param payloadString A {@link String} representation of the payload
     */
    public JsonPayload(String payloadString) throws PayloadParsingException {
        ObjectUtil.checkNotNull(payloadString, "payloadString");
        try {
            this.json = (ObjectNode) new ObjectMapper().readTree(payloadString);
        } catch (JsonProcessingException e) {
            throw new PayloadParsingException(payloadString, e);
        }
        this.fields = new HashMap<String, String>();

        Iterator<Map.Entry<String, JsonNode>> iterator = this.json.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            this.fields.put(entry.getKey(), entry.getValue().asText());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addField(String key, String value) {
        ObjectUtil.checkNotNull(key, "key");
        ObjectUtil.checkNotNull(value, "value");
        this.fields.put(key, value);
        this.json.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getField(String key) {
        return this.fields.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasField(String key) {
        return this.fields.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getFields() {
        return new HashMap<String, String>(this.fields);
    }

    public String toString() {
        return this.json.toString();
    }
}
