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

package com.mohammadaltaleb.netstreamer.client;

import com.mohammadaltaleb.netstreamer.payload.Payload;
import com.mohammadaltaleb.netstreamer.util.ObjectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link com.mohammadaltaleb.netstreamer.Netstreamer} client.
 */
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private final Set<String> topics = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final String uid;
    private final Channel channel;
    private final int maxPayloadDrop;
    private final ClientDisconnectionListener disconnectionListener;

    private long sentPayloads = 0;
    private long droppedPayloads = 0;
    private boolean isConnected = true;

    /**
     * Create a new instance
     *
     * @param uid                   Client's unique identifier
     * @param channel               Client's {@link Channel}
     * @param maxPayloadDrop        Maximum payload drop before disconnecting
     * @param disconnectionListener Client's {@link ClientDisconnectionListener}
     */
    public Client(String uid, Channel channel, int maxPayloadDrop, ClientDisconnectionListener disconnectionListener) {
        ObjectUtil.checkNotNull(uid, "uid");
        ObjectUtil.checkNotNull(channel, "channel");
        ObjectUtil.checkNotNull(disconnectionListener, "disconnectionListener");
        this.uid = uid;
        this.channel = channel;
        this.maxPayloadDrop = maxPayloadDrop;
        this.disconnectionListener = disconnectionListener;
    }

    /**
     * Get the unique identifier of the client
     *
     * @return the unique identifier
     */
    public String getUid() {
        return this.uid;
    }

    /**
     * Disconnect the client and close the socket channel
     */
    public void disconnect() {
        if (!this.isConnected) {
            return;
        }
        isConnected = false;
        channel.close();
        logger.debug("Client {} disconnected", this.uid);
        this.disconnectionListener.onDisconnect(this.uid);
    }

    /**
     * Add a topic to the subscribed topics set
     *
     * @param topic The subscription topic
     */
    public void addSubscription(String topic) {
        ObjectUtil.checkNotNull(topic, "topic");
        this.topics.add(topic);
    }

    /**
     * Remove a topic from the subscribed topics set if exists
     *
     * @param topic The subscribed topic
     */
    public void removeSubscription(String topic) {
        this.topics.remove(topic);
    }

    /**
     * Weather the client is connected
     *
     * @return {@code true} if client is connected
     */
    public boolean isConnected() {
        return this.isConnected && this.channel.isOpen();
    }

    /**
     * Get the subscribed topics set
     *
     * @return The subscribed topics set
     */
    public Set<String> getSubscriptions() {
        return new HashSet<String>(this.topics);
    }

    /**
     * Send a {@link Payload} of a specific topic to the client if the client {@link #isConnected()}
     * and the {@link Channel} is not null and is writable.
     * If the {@link Channel} is not writable, The {@link Payload} will be dropped and {@link #getDroppedPayloadCount()}
     * will be incremented.
     *
     * @param payload The {@link Payload} to be sent
     */
    public void send(Payload payload) {
        ObjectUtil.checkNotNull(payload, "payload");
        if (!isConnected()) {
            logger.debug("Cannot send payload to client {}. Not connected", this.uid);
            return;
        }
        if (!this.channel.isWritable()) {
            logger.debug("Cannot send payload to client {}. Channel is not writable", this.uid);
            logDroppedPayload(payload.toString());
            return;
        }
        ByteBuf byteBuf = Unpooled.copiedBuffer(payload.toString(), CharsetUtil.UTF_8).retain();
        this.channel.writeAndFlush(byteBuf);
        this.sentPayloads++;
    }

    /**
     * Get the sent {@link Payload} count
     *
     * @return Sent {@link Payload} count
     */
    public long getSentPayloadCount() {
        return this.sentPayloads;
    }

    /**
     * Get the dropped {@link Payload} count
     *
     * @return Dropped {@link Payload} count
     */
    public long getDroppedPayloadCount() {
        return this.droppedPayloads;
    }

    /**
     * Get the number of subscribed topics
     *
     * @return Subscribed topics count
     */
    public int getSubscribedTopicCount() {
        return this.topics.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return uid.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client that = (Client) o;
        return uid.equals(that.uid);
    }

    private void logDroppedPayload(String payload) {
        this.droppedPayloads++;

        if (this.droppedPayloads < this.maxPayloadDrop) {
            if (logger.isDebugEnabled()) {
                logger.warn(
                        "Dropping payload #{} for client with uid: {} - ip: {} - payload: {}",
                        this.droppedPayloads, this.uid, this.channel.remoteAddress(), payload
                );
            } else {
                logger.warn(
                        "Dropping payload #{} for client with uid: {} - ip: {}",
                        this.droppedPayloads, this.uid, this.channel.remoteAddress()
                );
            }
        } else if (this.droppedPayloads >= this.maxPayloadDrop) {
            throw new MaxPayloadDropException(String.format(
                    "Client reached max payload drop (%d). Client uid: %s - ip: %s",
                    this.maxPayloadDrop,
                    this.uid,
                    this.channel.remoteAddress()
            ));
        }
    }
}
