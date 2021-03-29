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

import java.util.Collections;
import java.util.List;

class ClientsReport {
    private final double meanPayloadSent;
    private final double meanPayloadDropped;
    private final double meanSubscribedTopics;
    private final long medianPayloadSent;
    private final long medianPayloadDropped;
    private final int medianSubscribedTopics;
    private final long p95PayloadSent;
    private final long p95PayloadDropped;
    private final int p95SubscribedTopics;
    private final int totalClients;
    private final int connectedClients;
    private final int clientsWithDropCount;

    public ClientsReport(int totalClients, int connectedClients, List<Long> sentPayloadCountList,
                         List<Long> droppedPayloadCountList, List<Integer> subscribedTopicsCountList) {
        this.totalClients = totalClients;
        this.connectedClients = connectedClients;
        if (totalClients == 0) {
            this.meanPayloadSent = 0D;
            this.meanPayloadDropped = 0D;
            this.meanSubscribedTopics = 0;

            this.medianPayloadSent = 0L;
            this.medianPayloadDropped = 0L;
            this.medianSubscribedTopics = 0;

            this.p95PayloadSent = 0L;
            this.p95PayloadDropped = 0L;
            this.p95SubscribedTopics = 0;

            this.clientsWithDropCount = 0;
        } else {
            long totalPayloadSent = 0L;
            long totalPayloadDropped = 0L;
            long totalSubscribedTopics = 0L;
            int clientsWithDropCount = 0;
            for (int i = 0; i < totalClients; i++) {
                totalPayloadSent += sentPayloadCountList.get(i);
                totalPayloadDropped += droppedPayloadCountList.get(i);
                totalSubscribedTopics += subscribedTopicsCountList.get(i);
                if (droppedPayloadCountList.get(i) > 0) {
                    clientsWithDropCount++;
                }
            }
            Collections.sort(sentPayloadCountList);
            Collections.sort(droppedPayloadCountList);
            Collections.sort(subscribedTopicsCountList);
            int p50Index = (int) Math.floor(0.50 * connectedClients);
            int p95Index = (int) Math.floor(0.95 * connectedClients);

            this.meanPayloadSent = connectedClients == 0 ? 0D : (double) totalPayloadSent / connectedClients;
            this.meanPayloadDropped = connectedClients == 0 ? 0D : (double) totalPayloadDropped / connectedClients;
            this.meanSubscribedTopics = connectedClients == 0 ? 0D : (double) totalSubscribedTopics / connectedClients;

            this.medianPayloadSent = sentPayloadCountList.get(p50Index);
            this.medianPayloadDropped = droppedPayloadCountList.get(p50Index);
            this.medianSubscribedTopics = subscribedTopicsCountList.get(p50Index);

            this.p95PayloadSent = sentPayloadCountList.get(p95Index);
            this.p95PayloadDropped = droppedPayloadCountList.get(p95Index);
            this.p95SubscribedTopics = subscribedTopicsCountList.get(p95Index);

            this.clientsWithDropCount = clientsWithDropCount;
        }
    }

    @Override
    public String toString() {
        return "\n--------------------------------------------\n" +
                "Clients Report:\n" +
                String.format("Total Clients: %s\n", totalClients) +
                String.format("Connected Clients: %s\n", connectedClients) +
                String.format("Clients with Dropped Payloads: %s\n", clientsWithDropCount) +
                String.format("Mean Payload Sent: %s\n", meanPayloadSent) +
                String.format("Mean Payload Dropped: %s\n", meanPayloadDropped) +
                String.format("Mean Subscribed Topics: %s\n", meanSubscribedTopics) +
                String.format("Median Payload Sent: %s\n", medianPayloadSent) +
                String.format("Median Payload Dropped: %s\n", medianPayloadDropped) +
                String.format("Median Subscribed Topics: %s\n", medianSubscribedTopics) +
                String.format("P95 Payload Sent: %s\n", p95PayloadSent) +
                String.format("P95 Payload Dropped: %s\n", p95PayloadDropped) +
                String.format("P95 Subscribed Topics: %s", p95SubscribedTopics) +
                "\n--------------------------------------------\n";
    }
}
