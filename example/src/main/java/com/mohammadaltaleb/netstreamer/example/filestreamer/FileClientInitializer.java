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

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileClientInitializer {
    private static final Logger logger = Logger.getLogger(FileClientInitializer.class);
    private static final String TOPIC = "PUB.FILE";
    private static final String DEFAULT_URI = "ws://127.0.0.1/";

    public static void main(String[] args) {
        int clientsCount = Integer.getInteger("count", 1);
        final String uriString = System.getProperty("uri", DEFAULT_URI);

        String homePath = System.getProperty("user.home");
        String dataFilesDirectoryPath = String.format("%s/filestreamer-output/client", homePath);
        clearDataFilesDirectory(dataFilesDirectoryPath);

        ExecutorService executorService = Executors.newFixedThreadPool(clientsCount);
        for (int i = 0; i < clientsCount; i++) {
            final String dataFilePath = String.format("%s/data%d.txt", dataFilesDirectoryPath, i + 1);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    FileClient fileClient = new FileClient(uriString, dataFilePath, TOPIC);
                    fileClient.start();
                }
            });
        }
    }

    private static void clearDataFilesDirectory(String directoryPath) {
        File directoryFile = new File(directoryPath);
        if (directoryFile.exists()) {
            logger.info(String.format("Clearing data files directory %s", directoryPath));
            for (String childPath : directoryFile.list()) {
                File childFile = new File(directoryPath, childPath);
                childFile.delete();
            }
        } else {
            logger.info(String.format("Creating data files directory %s", directoryPath));
            boolean created = directoryFile.mkdirs();
            if (!created) {
                logger.error("Could not create directory!");
                System.exit(-1);
            }
        }
    }
}
