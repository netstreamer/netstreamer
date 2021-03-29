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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class StreamerDataFileManager {
    private static final Logger logger = Logger.getLogger(StreamerDataFileManager.class);
    private final static String DATA_FILE_PATH = System.getProperty("user.home") + "/filestreamer-output/publisher/data.txt";

    private final File dataFile;
    private final int bytesToWrite;

    private int writtenBytes = 0;

    StreamerDataFileManager(int size) {
        this.bytesToWrite = size * 1024 * 1024;
        this.dataFile = new File(DATA_FILE_PATH);
    }

    void generateDataFile() {
        logger.info(String.format("Generating data file in %s...", DATA_FILE_PATH));
        this.clearDataFile();
        BufferedWriter bufferedWriter = this.getBufferedWriter();
        Random random = new Random();
        int minLineLength = 100;
        int maxLineLength = 400;
        try {
            while (this.writtenBytes < this.bytesToWrite) {
                int lineLength = random.nextInt((maxLineLength - minLineLength) + 1) + minLineLength;
                this.writtenBytes += lineLength;

                bufferedWriter.write(String.format("%s\n", this.getRandomString(lineLength)));
            }
            bufferedWriter.write("LAST LINE\n");
            bufferedWriter.close();
        } catch (IOException e) {
            logger.error("Failed while generating data file!", e);
            System.exit(-1);
        }
        logger.info("Done generating data file");
    }

    File getDataFile() {
        return this.dataFile;
    }

    private void clearDataFile() {
        if (this.dataFile.exists()) {
            logger.info("Deleting old data file");
            this.dataFile.delete();
        } else if (!this.dataFile.getParentFile().exists()) {
            logger.info("Creating data file directories");
            boolean created = this.dataFile.getParentFile().mkdirs();
            if (!created) {
                logger.error("Could not create directories!");
                System.exit(-1);
            }
        }
    }

    private BufferedWriter getBufferedWriter() {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(this.dataFile));
        } catch (IOException e) {
            logger.error("Could not create buffered writer!", e);
            System.exit(-1);
        }
        return bufferedWriter;
    }

    private String getRandomString(int stringLength) {
        int aIndex = 97; // letter 'a'
        int zIndex = 122; // letter 'z'
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder(stringLength);
        for (int i = 0; i < stringLength; i++) {
            int characterIndex = random.nextInt((zIndex - aIndex) + 1) + aIndex;
            stringBuilder.append((char) characterIndex);
        }
        return stringBuilder.toString();
    }
}
