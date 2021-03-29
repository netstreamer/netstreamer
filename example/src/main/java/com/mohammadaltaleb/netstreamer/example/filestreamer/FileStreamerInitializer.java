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

import org.apache.log4j.Logger;

public class FileStreamerInitializer {
    private static final Logger logger = Logger.getLogger(FileStreamerInitializer.class);
    private static final String TOPIC = "PUB.FILE";

    public static void main(String[] args) {
        int size = getIntProperty("size", 1, 1000);
        int port = getIntProperty("port", 1, 65535);
        boolean isSecure = System.getProperties().containsKey("isSecure") && Boolean.getBoolean("isSecure");

        logger.info(String.format("Preparing publisher to stream %dMB...", size));

        StreamerDataFileManager streamerDataFileManager = new StreamerDataFileManager(size);
        streamerDataFileManager.generateDataFile();

        FileStreamer fileStreamer = new FileStreamer(port, isSecure, streamerDataFileManager.getDataFile(), TOPIC);
        fileStreamer.start();
    }

    private static int getIntProperty(String propertyKey, int minValue, int maxValue) {
        String propertyStringValue = System.getProperty(propertyKey);
        if (propertyStringValue == null) {
            logger.error(String.format("%s is not provided as a system property (-D%s)!", propertyKey, propertyKey));
            System.exit(-1);
        }

        int propertyIntValue = 0;
        try {
            propertyIntValue = Integer.parseInt(propertyStringValue);
        } catch (NumberFormatException e) {
            logger.error(String.format("Provided %s is not a valid integer!", propertyKey), e);
            System.exit(-1);
        }

        if (propertyIntValue < minValue || propertyIntValue > maxValue) {
            logger.error(String.format("%s must be in range [%d, %d]!", propertyKey, minValue, maxValue));
            System.exit(-1);
        }

        return propertyIntValue;
    }
}
