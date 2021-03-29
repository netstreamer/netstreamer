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

package com.mohammadaltaleb.netstreamer.config;

import com.mohammadaltaleb.netstreamer.payload.JsonPayloadFactory;
import com.mohammadaltaleb.netstreamer.payload.PayloadFactory;
import com.mohammadaltaleb.netstreamer.util.ObjectUtil;
import io.netty.handler.ssl.SslContext;

/**
 * This class contains the configurations of the {@link com.mohammadaltaleb.netstreamer.Netstreamer}
 * and is required to initialize the streamer.
 */
public class NetstreamerConfig {
    private final String host;
    private final int port;

    private String webSocketPath = "/";
    private int clientHighWatermark = 2 * 1024 * 1024;
    private int clientLowWatermark = clientHighWatermark - (32 * 1024);
    private int bossEventLoopGroupThreads = 1;
    private int workerEventLoopGroupThreads = 0;
    private int maxClientPayloadDrop = 100;
    private int maxFrameSize = 64 * 1024;
    private String[] allowedOrigins = {"*"};
    private Transport transport = Transport.NIO;
    private SslContext sslContext = null;
    private PayloadFactory payloadFactory = new JsonPayloadFactory();

    /**
     * Create a new instance
     *
     * @param port The port {@link com.mohammadaltaleb.netstreamer.Netstreamer} should bind to
     * @see #NetstreamerConfig(String, int)
     */
    public NetstreamerConfig(int port) {
        this("localhost", port);
    }

    /**
     * Create a new instance
     *
     * @param host The host name of the {@link com.mohammadaltaleb.netstreamer.Netstreamer}
     * @param port The port {@link com.mohammadaltaleb.netstreamer.Netstreamer} should bind to
     */
    public NetstreamerConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Get the web socket path of the streamer
     *
     * @return The web socket path
     */
    public String getWebSocketPath() {
        return webSocketPath;
    }

    /**
     * Set the web socket path of the streamer
     *
     * @param webSocketPath The web socket path
     * @return The {@link NetstreamerConfig} instance
     */
    public NetstreamerConfig setWebSocketPath(String webSocketPath) {
        ObjectUtil.checkNotNull(webSocketPath, "webSocketPath");
        if (webSocketPath.isEmpty()) {
            webSocketPath = "/";
        }
        this.webSocketPath = webSocketPath;
        return this;
    }

    /**
     * Get netty {@link io.netty.channel.Channel}'s high watermark
     *
     * @return netty {@link io.netty.channel.Channel}'s high watermark
     */
    public int getClientHighWatermark() {
        return clientHighWatermark;
    }

    /**
     * Set netty {@link io.netty.channel.Channel}'s high watermark
     *
     * @param clientHighWatermark netty {@link io.netty.channel.Channel}'s high watermark
     * @return The {@link NetstreamerConfig} instance
     */
    public NetstreamerConfig setClientHighWatermark(int clientHighWatermark) {
        this.clientHighWatermark = clientHighWatermark;
        return this;
    }

    /**
     * Get netty {@link io.netty.channel.Channel}'s low watermark
     *
     * @return netty {@link io.netty.channel.Channel}'s low watermark
     */
    public int getClientLowWatermark() {
        return clientLowWatermark;
    }

    /**
     * Set netty {@link io.netty.channel.Channel}'s low watermark
     *
     * @param clientLowWatermark netty {@link io.netty.channel.Channel}'s low watermark
     * @return The {@link NetstreamerConfig} instance
     */
    public NetstreamerConfig setClientLowWatermark(int clientLowWatermark) {
        this.clientLowWatermark = clientLowWatermark;
        return this;
    }

    /**
     * Get netty {@link io.netty.bootstrap.ServerBootstrap}'s parent event loop threads
     *
     * @return netty {@link io.netty.bootstrap.ServerBootstrap}'s parent event loop threads
     */
    public int getBossEventLoopGroupThreads() {
        return bossEventLoopGroupThreads;
    }

    /**
     * Set netty {@link io.netty.bootstrap.ServerBootstrap}'s parent event loop threads
     *
     * @param bossEventLoopGroupThreads netty {@link io.netty.bootstrap.ServerBootstrap}'s parent event loop threads
     * @return The {@link NetstreamerConfig} instance
     */
    public NetstreamerConfig setBossEventLoopGroupThreads(int bossEventLoopGroupThreads) {
        this.bossEventLoopGroupThreads = bossEventLoopGroupThreads;
        return this;
    }

    /**
     * Get netty {@link io.netty.bootstrap.ServerBootstrap}'s child event loop threads
     *
     * @return netty {@link io.netty.bootstrap.ServerBootstrap}'s child event loop threads
     */
    public int getWorkerEventLoopGroupThreads() {
        return workerEventLoopGroupThreads;
    }

    /**
     * Set netty {@link io.netty.bootstrap.ServerBootstrap}'s child event loop threads
     *
     * @param workerEventLoopGroupThreads netty {@link io.netty.bootstrap.ServerBootstrap}'s child event loop threads
     * @return The {@link NetstreamerConfig} instance
     */
    public NetstreamerConfig setWorkerEventLoopGroupThreads(int workerEventLoopGroupThreads) {
        this.workerEventLoopGroupThreads = workerEventLoopGroupThreads;
        return this;
    }

    /**
     * Get maximum payload drop for {@link com.mohammadaltaleb.netstreamer.client.Client}
     *
     * @return Maximum payload drop for {@link com.mohammadaltaleb.netstreamer.client.Client}
     */
    public int getMaxClientPayloadDrop() {
        return maxClientPayloadDrop;
    }

    /**
     * Set maximum payload drop for {@link com.mohammadaltaleb.netstreamer.client.Client}.
     * After this threshold, the {@link com.mohammadaltaleb.netstreamer.client.Client} will be disconnected
     *
     * @param maxClientPayloadDrop Maximum payload drop for {@link com.mohammadaltaleb.netstreamer.client.Client}
     * @return The {@link NetstreamerConfig} instance
     */
    public NetstreamerConfig setMaxClientPayloadDrop(int maxClientPayloadDrop) {
        this.maxClientPayloadDrop = maxClientPayloadDrop;
        return this;
    }

    /**
     * Get maximum web socket frame size
     *
     * @return Maximum web socket frame size
     */
    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    /**
     * Set maximum web socket frame size
     *
     * @param maxFrameSize maximum web socket frame size
     * @return The {@link NetstreamerConfig} instance
     */
    public NetstreamerConfig setMaxFrameSize(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    /**
     * Get {@link io.netty.handler.codec.http.cors.CorsHandler}'s allowed origins
     *
     * @return {@link io.netty.handler.codec.http.cors.CorsHandler}'s allowed origins
     */
    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * Set {@link io.netty.handler.codec.http.cors.CorsHandler}'s allowed origins
     *
     * @param origins {@link io.netty.handler.codec.http.cors.CorsHandler}'s allowed origins
     * @return The {@link NetstreamerConfig} instance
     */
    public NetstreamerConfig setAllowedOrigins(String... origins) {
        this.allowedOrigins = origins;
        return this;
    }

    /**
     * Get netty {@link io.netty.channel.EventLoopGroup}s and {@link io.netty.channel.socket.ServerSocketChannel}'s transport type
     *
     * @return Transport type
     */
    public Transport getTransport() {
        return transport;
    }

    /**
     * Set netty {@link io.netty.channel.EventLoopGroup}s and {@link io.netty.channel.socket.ServerSocketChannel}'s transport type
     *
     * @param transport Transport type
     * @return The {@link NetstreamerConfig} instance
     */
    public NetstreamerConfig setTransport(Transport transport) {
        ObjectUtil.checkNotNull(transport, "transport");
        this.transport = transport;
        return this;
    }

    /**
     * Get {@link io.netty.handler.ssl.SslHandler}'s {@link SslContext}. {@code null} if TLS/SSL is not enabled
     *
     * @return {@link io.netty.handler.ssl.SslHandler}'s {@link SslContext}
     */
    public SslContext getSslContext() {
        return sslContext;
    }

    /**
     * Set {@link io.netty.handler.ssl.SslHandler}'s {@link SslContext}. {@code null} to disable TLS/SSL
     *
     * @param sslContext {@link io.netty.handler.ssl.SslHandler}'s {@link SslContext}
     * @return The {@link NetstreamerConfig} instance
     */
    public NetstreamerConfig setSslContext(SslContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    /**
     * Get the {@link PayloadFactory} used to create and parse {@link com.mohammadaltaleb.netstreamer.payload.Payload} objects
     *
     * @return The {@link PayloadFactory}
     */
    public PayloadFactory getPayloadFactory() {
        return payloadFactory;
    }

    /**
     * Set the {@link PayloadFactory} to be used when creating and parsing {@link com.mohammadaltaleb.netstreamer.payload.Payload} objects
     *
     * @param payloadFactory The {@link PayloadFactory} to use
     * @return The {@link NetstreamerConfig} instance
     */
    public NetstreamerConfig setPayloadFactory(PayloadFactory payloadFactory) {
        this.payloadFactory = payloadFactory;
        return this;
    }

    /**
     * Get {@link com.mohammadaltaleb.netstreamer.Netstreamer}'s host name
     *
     * @return {@link com.mohammadaltaleb.netstreamer.Netstreamer}'s host name
     */
    public String getHost() {
        return host;
    }

    /**
     * Get {@link com.mohammadaltaleb.netstreamer.Netstreamer}'s port
     *
     * @return {@link com.mohammadaltaleb.netstreamer.Netstreamer}'s port
     */
    public int getPort() {
        return port;
    }
}
