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

import java.util.Map;

/**
 * Payload to be sent to {@link com.mohammadaltaleb.netstreamer.client.Client}.
 * A JSON implementation using Jackson library is provided in {@link JsonPayload}, and you can use your own implementation.
 */
public interface Payload {

    /**
     * Add a field to the payload
     *
     * @param key   Field's key
     * @param value Field's value
     */
    void addField(String key, String value);

    /**
     * Return the payload field's value given its key. If the field does not exist, The result will be {@code null}
     *
     * @param key The key of the field
     * @return The value of the field
     */
    String getField(String key);

    /**
     * Weather a field with a given key exists in the payload
     *
     * @param key the key of the field
     * @return {@code true} if a field with the given key exits
     */
    boolean hasField(String key);

    /**
     * Get a copy of payload fields as a {@link Map}
     *
     * @return {@link Map} containing a copy of payload fields
     */
    Map<String, String> getFields();
}
