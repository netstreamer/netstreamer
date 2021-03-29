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

import com.mohammadaltaleb.netstreamer.client.Client;
import com.mohammadaltaleb.netstreamer.client.ClientDisconnectionListener;
import com.mohammadaltaleb.netstreamer.client.MaxPayloadDropException;
import com.mohammadaltaleb.netstreamer.payload.Payload;
import com.mohammadaltaleb.netstreamer.payload.PayloadFactory;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.mohammadaltaleb.netstreamer.payload.PayloadConstants.*;

public class SubscriptionManager implements ClientDisconnectionListener, ClientAuthenticationListener {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionManager.class);

    private final Queue<Client> unauthenticatedClients = new ConcurrentLinkedQueue<Client>();
    private final Map<String, String> clientAuthenticationParams = new ConcurrentHashMap<String, String>();
    private final Map<String, Client> clients = new ConcurrentHashMap<String, Client>();
    private final Map<String, Set<Client>> topicClientsMap = new ConcurrentHashMap<String, Set<Client>>();
    private final ExecutorService notifyPool = Executors.newFixedThreadPool(2);
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
    private final AtomicLong sendExecutorQueueSize = new AtomicLong(0L);
    private final PayloadFactory payloadFactory;
    private final int maxClientPayloadDrop;

    private StreamerListener streamerListener;

    SubscriptionManager(PayloadFactory payloadFactory, int maxClientPayloadDrop) {
        this.payloadFactory = payloadFactory;
        this.maxClientPayloadDrop = maxClientPayloadDrop;
        ExecutorService authenticationExecutor = Executors.newSingleThreadExecutor();
        authenticationExecutor.execute(new AuthenticationWorker());
    }

    public void connectClient(String uid, String authParam, Channel channel) {
        logger.debug("Creating new client");
        Client client = new Client(uid, channel, maxClientPayloadDrop, this);
        // DO NOT CHANGE THE ORDER OF AUTH PARAM AND CLIENT INSERTION.
        // THE AUTHENTICATION WORKER THREAD POLLS THE CLIENT FIRST AND PARAM SECOND.
        // IF THE ORDER IS CHANGED, THIS COULD CAUSE AN ISSUE WHERE THE CLIENT IS POLLED AND THE PARAM IS NOT INSERTED YET.
        this.clientAuthenticationParams.put(uid, authParam);
        this.unauthenticatedClients.add(client);
    }

    public void disconnectClient(String uid) {
        Client client = this.clients.get(uid);
        if (client != null) {
            client.disconnect();
        }
    }

    public void addSubscription(String uid, String[] topics) {
        if (topics.length == 0) {
            return;
        }
        Client client = this.clients.get(uid);
        if (client == null) {
            logger.debug("No authenticated client with uid {} was found. Ignoring add subscription request", uid);
            return;
        }
        logger.debug("Adding subscriptions for client {}", uid);
        for (String topic : topics) {
            String trimmedTopic = topic.trim();
            addClientToTopicSubscribers(trimmedTopic, client);
            client.addSubscription(trimmedTopic);
            notifyOnSubscribe(trimmedTopic, client);
        }
    }

    public void removeSubscription(String uid, String[] topics) {
        if (topics.length == 0) {
            return;
        }
        Client client = this.clients.get(uid);
        if (client == null) {
            logger.debug("No authenticated client with uid {} was found. Ignoring remove subscription request", uid);
            return;
        }
        logger.debug("Removing subscriptions for client {}", uid);
        for (String topic : topics) {
            String trimmedTopic = topic.trim();
            removeClientFromTopicSubscribers(trimmedTopic, client);
            client.removeSubscription(trimmedTopic);
            notifyOnUnsubscribe(trimmedTopic, client);
        }
    }

    public boolean hasClient(String uid) {
        return this.clients.containsKey(uid);
    }

    public int getClientCount() {
        return this.clients.size();
    }

    public int getSubscribedClientCount(String topic) {
        Set<Client> subscribedClients = this.topicClientsMap.get(topic);
        if (subscribedClients == null) {
            return 0;
        }
        return subscribedClients.size();
    }

    @Override
    public void accept(Client client) {
        if (!client.isConnected()) {
            return;
        }
        this.clients.put(client.getUid(), client);
        Payload payload = this.getStatusPayload(RESPONSE_SUCCESS_STATUS, "Client is successfully connected and authenticated");
        client.send(payload);
    }

    @Override
    public void reject(Client client) {
        Payload payload = this.getStatusPayload(RESPONSE_FAIL_STATUS, "Failed to authenticate client");
        client.send(payload);
        client.disconnect();
    }

    @Override
    public void onDisconnect(String uid) {
        logger.debug("Removing client {} from clients map", uid);
        Client client = this.clients.remove(uid);
        if (client == null) {
            return;
        }
        logger.debug("Removing client {} subscriptions", uid);
        Set<String> clientSubscriptions = client.getSubscriptions();
        for (String topic : clientSubscriptions) {
            removeClientFromTopicSubscribers(topic, client);
            notifyOnUnsubscribe(topic, client);
        }
    }

    void setStreamerListener(StreamerListener streamerListener) {
        this.streamerListener = streamerListener;
    }

    void send(final String topic, final Payload payload) {
        Set<Client> subscribedClients = this.topicClientsMap.get(topic);
        if (subscribedClients == null || subscribedClients.isEmpty()) {
            logger.debug("No subscribers for topic {}", topic);
            return;
        }
        for (final Client client : subscribedClients) {
            long executorQueueSize = sendExecutorQueueSize.incrementAndGet();
            if (executorQueueSize % 10000 == 0) {
                logger.debug(
                        "Number of pending messages to be sent in the queue: {}. number of clients: {}",
                        executorQueueSize,
                        clients.size()
                );
            }
            this.sendExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setName("Sender");
                    try {
                        sendExecutorQueueSize.decrementAndGet();
                        client.send(payload);
                    } catch (MaxPayloadDropException e) {
                        logger.error("Disconnecting client {} because of payload drop", client.getUid(), e);
                        client.disconnect();
                    } catch (Exception e) {
                        logger.error("Error while sending payload to client {}", client.getUid(), e);
                    }
                }
            });
        }
    }

    ClientsReport getClientsReport() {
        int totalClients;
        int connectedCount = 0;
        List<Long> sentPayloadCountList = new ArrayList<Long>();
        List<Long> droppedPayloadCountList = new ArrayList<Long>();
        List<Integer> subscribedTopicCountList = new ArrayList<Integer>();
        totalClients = this.clients.size();
        for (Map.Entry<String, Client> entry : this.clients.entrySet()) {
            Client client = entry.getValue();
            if (client.isConnected()) {
                connectedCount++;
            }
            sentPayloadCountList.add(client.getSentPayloadCount());
            droppedPayloadCountList.add(client.getDroppedPayloadCount());
            subscribedTopicCountList.add(client.getSubscribedTopicCount());
        }
        return new ClientsReport(
                totalClients, connectedCount, sentPayloadCountList, droppedPayloadCountList, subscribedTopicCountList
        );
    }

    private void addClientToTopicSubscribers(String topic, Client client) {
        logger.debug("Adding client {} to topic {} subscribers", client.getUid(), topic);
        Set<Client> clients;
        synchronized (this.topicClientsMap) {
            clients = this.topicClientsMap.get(topic);
            if (clients == null) {
                clients = Collections.newSetFromMap(new ConcurrentHashMap<Client, Boolean>());
                this.topicClientsMap.put(topic, clients);
            }
        }
        clients.add(client);
    }

    private void removeClientFromTopicSubscribers(String topic, Client client) {
        logger.debug("Removing client {} from topic {} subscribers", client.getUid(), topic);
        Set<Client> clients = this.topicClientsMap.get(topic);
        if (clients == null) {
            return;
        }
        clients.remove(client);
    }

    private void notifyOnSubscribe(final String topic, final Client client) {
        if (this.streamerListener == null) {
            return;
        }
        logger.debug("Notifying subscribe listener. Client: {}, Topic: {}", client.getUid(), topic);
        this.notifyPool.execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("Subscribe Notifier");
                try {
                    streamerListener.onSubscribe(client, topic);
                } catch (Exception e) {
                    logger.error("Error while subscribe notifying", e);
                }
            }
        });
    }

    private void notifyOnUnsubscribe(final String topic, final Client client) {
        if (this.streamerListener == null) {
            return;
        }
        logger.debug("Notifying unsubscribe listener. Client: {}, Topic: {}", client.getUid(), topic);
        this.notifyPool.execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("Unsubscribe Notifier");
                try {
                    streamerListener.onUnsubscribe(client, topic);
                } catch (Exception e) {
                    logger.error("Error while unsubscribe notifying", e);
                }
            }
        });
    }

    private Payload getStatusPayload(String status, String message) {
        Payload payload = this.payloadFactory.emptyPayload();
        payload.addField(RESPONSE_EVENT_KEY, RESPONSE_STATUS_EVENT);
        payload.addField(RESPONSE_STATUS_KEY, status);
        payload.addField(RESPONSE_MESSAGE_KEY, message);
        return payload;
    }

    private class AuthenticationWorker implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName("Authentication Worker");
            while (true) {
                try {
                    Client client = unauthenticatedClients.poll();
                    if (client == null) {
                        Thread.sleep(50);
                        continue;
                    }
                    String authParam = clientAuthenticationParams.remove(client.getUid());
                    if (streamerListener == null) {
                        accept(client);
                    } else {
                        streamerListener.onAuth(client, authParam, SubscriptionManager.this);
                    }
                } catch (Exception e) {
                    logger.error("Error while authenticating", e);
                }
            }
        }
    }
}
