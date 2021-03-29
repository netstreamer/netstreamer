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

package com.mohammadaltaleb.netstreamer.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mohammadaltaleb.netstreamer.connector.handlers.ConnectorChannelInitializer;
import com.mohammadaltaleb.netstreamer.connector.handlers.SecureConnectorChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;

/**
 * A simple utility to build a Netstreamer client.
 * <br><br>
 * This connector allows you to establish a WebSocket connection with a Netstreamer
 * and receive messages by subscribing to topics. It assumes the server accepts messages in JSON format.
 * <br><br>
 * <pre>
 *     {@link MessageListener} messageListener = ...;
 *     {@link String} topic = ...;
 *     {@link NetstreamerConnector} connector = new {@link NetstreamerConnector}("wss://127.0.0.1/", messageListener);
 *     if (connector.connect()) {
 *         connector.subscribe(topic);
 *     }
 * </pre>
 */
public class NetstreamerConnector {
    private static final Logger logger = LoggerFactory.getLogger(NetstreamerConnector.class);
    private static final String WEB_SOCKET_PROTOCOL = "ws";
    private static final String SECURE_WEB_SOCKET_PROTOCOL = "wss";
    private static final String ACTION_KEY = "action";
    private static final String PARAM_KEY = "param";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final URI uri;
    private final MessageListener messageListener;

    private Channel channel;
    private boolean connected = false;

    /**
     * Create a new instance
     *
     * @param uriString       The uri of the Netstreamer
     * @param messageListener The {@link MessageListener} to notify on new messages
     */
    public NetstreamerConnector(String uriString, MessageListener messageListener) {
        this.uri = this.getUriFromString(uriString);
        this.messageListener = messageListener;
    }

    /**
     * Open a WebSocket with Netstreamer
     */
    public void connect() throws SSLException, InterruptedException {
        if (this.connected) {
            logger.info("Already connected!");
            return;
        }
        logger.info(String.format("Connecting to %s...", this.uri.toString()));
        String scheme = this.uri.getScheme();
        ConnectorChannelInitializer channelInitializer = SECURE_WEB_SOCKET_PROTOCOL.equals(scheme) ?
                new SecureConnectorChannelInitializer(this.uri, this.messageListener, this.getSslContext()) :
                new ConnectorChannelInitializer(this.uri, this.messageListener);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup()).channel(NioSocketChannel.class).handler(channelInitializer);

        int port = this.uri.getPort() == -1 ? this.getDefaultPort(scheme) : this.uri.getPort();
        this.channel = bootstrap.connect(this.uri.getHost(), port).sync().channel();
        channelInitializer.getHandshakeHandler().handshakeFuture().sync();
        this.connected = true;
    }

    /**
     * Disconnect from Netstreamer
     */
    public void disconnect() {
        this.channel.writeAndFlush(new CloseWebSocketFrame());
        this.channel.close();
    }

    /**
     * Send an authentication request
     *
     * @param authParam The authentication parameter
     */
    public void authenticate(String authParam) {
        logger.info(String.format("Sending authentication request..."));
        this.send(this.getJsonPayload("auth", authParam));
    }

    /**
     * Subscribe to a topic
     *
     * @param topic The subscription topic
     */
    public void subscribe(String topic) {
        logger.info(String.format("Subscribe to %s...", topic));
        this.send(getJsonPayload("subscribe", topic));
    }

    /**
     * Unsubscribe from a topic
     *
     * @param topic The topic to unsubscribe from
     */
    public void unsubscribe(String topic) {
        logger.info(String.format("Unsubscribe from %s...", topic));
        this.send(getJsonPayload("unsubscribe", topic));
    }

    private void send(String text) {
        this.channel.writeAndFlush(new TextWebSocketFrame(text));
    }

    private SslContext getSslContext() throws SSLException {
        SslContextBuilder builder = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .sslProvider(SslProvider.OPENSSL);
        return builder.build();
    }

    private URI getUriFromString(String urlString) {
        URI uri = URI.create(urlString);
        if (!WEB_SOCKET_PROTOCOL.equals(uri.getScheme()) && !SECURE_WEB_SOCKET_PROTOCOL.equals(uri.getScheme())) {
            throw new IllegalArgumentException(String.format(
                    "Illegal Protocol [%s]! Protocol should either be [ws] or [wss]", uri.getScheme()
            ));
        }
        return uri;
    }

    private int getDefaultPort(String scheme) {
        return SECURE_WEB_SOCKET_PROTOCOL.equals(scheme) ? 443 : 80;
    }

    private String getJsonPayload(String action, String param) {
        ObjectNode objectNode = this.objectMapper.createObjectNode();
        objectNode.put(ACTION_KEY, action);
        objectNode.put(PARAM_KEY, param);
        return objectNode.toString();
    }
}
