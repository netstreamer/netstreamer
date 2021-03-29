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

package com.mohammadaltaleb.netstreamer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Reporter {
    private static final Logger logger = LoggerFactory.getLogger(Reporter.class);

    private final SubscriptionManager subscriptionManager;

    private boolean isStarted = false;

    Reporter(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    void start() {
        if (!logger.isDebugEnabled()) {
            return;
        }
        if (this.isStarted) {
            logger.debug("Reporter already started!");
            return;
        }
        logger.debug("Starting reporter");
        this.isStarted = true;
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(new LogReporter(), 1, 1, TimeUnit.MINUTES);
    }

    private class LogReporter implements Runnable {
        @Override
        public void run() {
            try {
                logger.debug(subscriptionManager.getClientsReport().toString());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
