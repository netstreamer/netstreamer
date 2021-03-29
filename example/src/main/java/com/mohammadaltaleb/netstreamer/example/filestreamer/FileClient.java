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

package com.mohammadaltaleb.netstreamer.example.filestreamer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mohammadaltaleb.netstreamer.connector.MessageListener;
import com.mohammadaltaleb.netstreamer.connector.NetstreamerConnector;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import static com.mohammadaltaleb.netstreamer.payload.PayloadConstants.*;

public class FileClient implements MessageListener {
    private static final Logger logger = Logger.getLogger(FileClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BufferedWriter bufferedWriter;
    private final String subscriptionTopic;
    private final NetstreamerConnector connector;

    FileClient(String uriString, String dataFilePath, String subscriptionTopic) {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(dataFilePath));
        } catch (IOException e) {
            logger.error("Failed to initialize file client BufferedWriter", e);
            System.exit(-1);
        }
        this.bufferedWriter = bufferedWriter;
        this.subscriptionTopic = subscriptionTopic;
        this.connector = new NetstreamerConnector(uriString, this);
    }

    @Override
    public void onMessage(String message) {
        ObjectNode objectNode;
        try {
            objectNode = (ObjectNode) objectMapper.readTree(message);
        } catch (JsonProcessingException e) {
            logger.error(String.format("Failed to parse JSON. Original message: %s", message), e);
            return;
        }
        if (!objectNode.has(RESPONSE_EVENT_KEY)) {
            logger.error(String.format("Message does not contain an event field %s", message));
            return;
        }
        String event = objectNode.get(RESPONSE_EVENT_KEY).asText();
        if (RESPONSE_STATUS_EVENT.equals(event)) {
            String status = objectNode.get(RESPONSE_STATUS_KEY).asText();
            String statusMessage = objectNode.get(RESPONSE_MESSAGE_KEY).asText();
            if (RESPONSE_SUCCESS_STATUS.equals(status)) {
                this.connector.subscribe(subscriptionTopic);
            } else {
                logger.error(String.format("Failed status message. status: %s. status message: %s", status, statusMessage));
            }
            return;
        }
        if (RESPONSE_UPDATE_EVENT.equals(event)) {
            String topic = objectNode.get(RESPONSE_TOPIC_KEY).asText();
            if (!this.subscriptionTopic.equals(topic)) {
                return;
            }
            String updateString = objectNode.get(RESPONSE_UPDATE_KEY).asText();
            ObjectNode update;
            try {
                update = (ObjectNode) objectMapper.readTree(updateString);
            } catch (JsonProcessingException e) {
                logger.error(String.format("Failed to parse update JSON. Update string: %s", updateString), e);
                return;
            }
            this.handleUpdate(update);
        }
    }

    void start() {
        if (this.connect()) {
            this.connector.authenticate(UUID.randomUUID().toString());
        }
    }

    private boolean connect() {
        try {
            this.connector.connect();
            return true;
        } catch (Exception e) {
            logger.error("Failed to connect to netstreamer", e);
            return false;
        }
    }

    private void handleUpdate(ObjectNode update) {
        if (!update.has("line") || !update.has("time")) {
            return;
        }

        String line = update.get("line").asText();
        long time = update.get("time").asLong();

        try {
            this.bufferedWriter.write(String.format("%s\n", line));
            if (line.equals("LAST LINE")) {
                logger.info(String.format("Received last line. Time difference: %dms", System.currentTimeMillis() - time));
                this.bufferedWriter.close();
            }
        } catch (IOException e) {
            logger.error("Error writing line to data file");
        }
    }
}
