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

/**
 * An abstract factory to create {@link Payload} objects
 */
public interface PayloadFactory {

    /**
     * Create a new empty {@link Payload}
     *
     * @return A new {@link Payload} object
     */
    Payload emptyPayload();

    /**
     * Create a {@link Payload} object from {@link String} representation
     *
     * @param payloadString The payload string
     * @return A new {@link Payload} object
     * @throws PayloadParsingException if payload parsing failed
     */
    Payload fromString(String payloadString) throws PayloadParsingException;
}
