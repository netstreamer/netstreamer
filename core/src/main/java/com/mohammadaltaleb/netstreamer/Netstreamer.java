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

import com.mohammadaltaleb.netstreamer.config.NetstreamerConfig;
import com.mohammadaltaleb.netstreamer.handlers.SecureStreamerChannelInitializer;
import com.mohammadaltaleb.netstreamer.handlers.StreamerChannelInitializer;
import com.mohammadaltaleb.netstreamer.payload.Payload;
import com.mohammadaltaleb.netstreamer.util.ObjectUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static com.mohammadaltaleb.netstreamer.payload.PayloadConstants.*;

/**
 * General-purpose WebSocket streamer.
 */
public class Netstreamer {
    private static final Logger logger = LoggerFactory.getLogger(Netstreamer.class);

    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    private final BootstrapSetup bootstrapSetup;
    private final InetSocketAddress address;
    private final NetstreamerConfig config;
    private final SubscriptionManager subscriptionManager;
    private final Reporter reporter;

    private boolean isStarted = false;
    private Channel channel;

    /**
     * Create a new instance
     *
     * @param config The streamer's configurations
     */
    public Netstreamer(NetstreamerConfig config) {
        ObjectUtil.checkNotNull(config, "config");
        this.logConfig(config);
        this.address = new InetSocketAddress(config.getHost(), config.getPort());
        this.bootstrapSetup = new BootstrapSetup(config.getTransport(), config.getBossEventLoopGroupThreads(), config.getWorkerEventLoopGroupThreads());
        this.config = config;
        this.subscriptionManager = new SubscriptionManager(config.getPayloadFactory(), config.getMaxClientPayloadDrop());
        this.reporter = new Reporter(this.subscriptionManager);
    }

    /**
     * Start the streamer
     */
    public void start() {
        logger.debug("Starting netstreamer...");
        if (this.isStarted) {
            logger.warn("Netstreamer is already started");
            return;
        }
        this.reporter.start();

        String webSocketPath = this.config.getWebSocketPath();
        boolean isSecure = this.config.getSslContext() != null;
        StreamerChannelInitializer channelInitializer;
        if (isSecure) {
            channelInitializer = new SecureStreamerChannelInitializer(
                    webSocketPath, this.config.getMaxFrameSize(), this.config.getAllowedOrigins(), this.channelGroup,
                    this.subscriptionManager, this.config.getPayloadFactory(), this.config.getSslContext()
            );
        } else {
            channelInitializer = new StreamerChannelInitializer(
                    webSocketPath, this.config.getMaxFrameSize(), this.config.getAllowedOrigins(), this.channelGroup,
                    this.subscriptionManager, this.config.getPayloadFactory()
            );
        }
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.bootstrapSetup.getBossGroup(), this.bootstrapSetup.getWorkerGroup())
                .channel(this.bootstrapSetup.getChannelClass())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(config.getClientLowWatermark(), config.getClientHighWatermark()))
                .childHandler(channelInitializer);

        try {
            this.channel = bootstrap.bind(this.address).sync().channel();
            this.isStarted = true;
            String protocol = isSecure ? "wss" : "ws";
            logger.info("Netstreamer bound to address successfully. ({}://{}:{}{})", protocol, config.getHost(), config.getPort(), webSocketPath);
        } catch (Exception e) {
            logger.error("Server binding failed!", e);
            this.stop();
        }
    }

    /**
     * Stop the streamer
     */
    public void stop() {
        logger.debug("Stopping streamer...");
        if (this.channel != null) {
            this.channel.close();
        }
        this.channelGroup.close();
        this.bootstrapSetup.getBossGroup().shutdownGracefully();
        this.bootstrapSetup.getWorkerGroup().shutdownGracefully();
    }

    /**
     * Publish an update to {@link com.mohammadaltaleb.netstreamer.client.Client}s with a specific topic subscription
     *
     * @param topic  The {@link Payload}'s topic
     * @param update The {@link Payload}'s update
     */
    public void publish(String topic, String update) {
        ObjectUtil.checkNotNull(topic, "topic");
        ObjectUtil.checkNotNull(update, "update");
        if (logger.isDebugEnabled()) {
            logger.debug("Publishing update to topic {}. update: {}", topic, update);
        }
        Payload payload = this.config.getPayloadFactory().emptyPayload();
        payload.addField(RESPONSE_EVENT_KEY, RESPONSE_UPDATE_EVENT);
        payload.addField(RESPONSE_TOPIC_KEY, topic);
        payload.addField(RESPONSE_UPDATE_KEY, update);
        this.subscriptionManager.send(topic, payload);
    }

    /**
     * Is the streamer started
     *
     * @return {@code true} is the streamer is started
     */
    public boolean isStarted() {
        return this.isStarted;
    }

    /**
     * Add a {@link StreamerListener} which will be notified for client authentication and on each subscribe and unsubscribe
     *
     * @param streamerListener The {@link StreamerListener}
     */
    public void setStreamerListener(StreamerListener streamerListener) {
        ObjectUtil.checkNotNull(streamerListener, "streamerListener");
        this.subscriptionManager.setStreamerListener(streamerListener);
    }

    /**
     * Get the number of connected authenticated clients
     *
     * @return The number of connected authenticated clients
     */
    public int getClientCount() {
        return this.subscriptionManager.getClientCount();
    }

    /**
     * Get the number of clients subscribed to a certain topic
     *
     * @param topic The subscription topic
     * @return The number of clients subscribed to the topic
     */
    public int getTopicSubscribedClientCount(String topic) {
        ObjectUtil.checkNotNull(topic, "topic");
        return this.subscriptionManager.getSubscribedClientCount(topic);
    }

    private void logConfig(NetstreamerConfig config) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug("Initializing netstreamer with config:");
        logger.debug("clientHighWatermark: {}", config.getClientHighWatermark());
        logger.debug("clientLowWatermark: {}", config.getClientLowWatermark());
        logger.debug("bossEventLoopGroupThreads: {}", config.getBossEventLoopGroupThreads());
        logger.debug("workerEventLoopGroupThreads: {}", config.getWorkerEventLoopGroupThreads());
        logger.debug("transport: {}", config.getTransport().toString());
        logger.debug("host: {}", config.getHost());
        logger.debug("port: {}", config.getPort());
    }
}
