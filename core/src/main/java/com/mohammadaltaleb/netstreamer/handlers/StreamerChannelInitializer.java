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

package com.mohammadaltaleb.netstreamer.handlers;

import com.mohammadaltaleb.netstreamer.SubscriptionManager;
import com.mohammadaltaleb.netstreamer.payload.PayloadFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamerChannelInitializer extends ChannelInitializer<Channel> {
    private static final Logger logger = LoggerFactory.getLogger(StreamerChannelInitializer.class);

    private final String webSocketPath;
    private final int maxFrameSize;
    private final ChannelGroup channelGroup;
    private final SubscriptionManager subscriptionManager;
    private final CorsConfig corsConfig;
    private final PayloadFactory payloadFactory;

    public StreamerChannelInitializer(String webSocketPath, int maxFrameSize, String[] allowedOrigins,
                                      ChannelGroup channelGroup, SubscriptionManager subscriptionManager,
                                      PayloadFactory payloadFactory) {
        this.webSocketPath = webSocketPath;
        this.maxFrameSize = maxFrameSize;
        this.channelGroup = channelGroup;
        this.subscriptionManager = subscriptionManager;
        this.corsConfig = this.getCorsConfigBuilder(allowedOrigins).build();
        this.payloadFactory = payloadFactory;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        logger.debug("Initializing channel {}", channel.remoteAddress());
        ChannelPipeline pipeline = channel.pipeline();

        // inbound
        pipeline.addLast("HttpServerCodec", new HttpServerCodec());
        pipeline.addLast("HttpObjectAggregator", new HttpObjectAggregator(64 * 1024));
        pipeline.addLast("CorsHandler", new CorsHandler(this.corsConfig));
        pipeline.addLast("HttpRequestHandler", new HttpRequestHandler(webSocketPath));
        pipeline.addLast("WebSocketServerProtocolHandler", new WebSocketServerProtocolHandler(webSocketPath, null, false, maxFrameSize));
        pipeline.addLast("StreamerRequestHandler", new StreamerRequestHandler(subscriptionManager, payloadFactory));
        pipeline.addLast("EventHandler", new EventHandler(subscriptionManager, channelGroup));

        // outbound
        pipeline.addLast("ByteBufToTextWebSocketFrameEncoder", new ByteBufToTextWebSocketFrameEncoder());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage(), cause);
        ctx.close();
    }

    private CorsConfigBuilder getCorsConfigBuilder(String[] allowedOrigins) {
        CorsConfigBuilder corsConfigBuilder;
        if (allowedOrigins.length == 1 && "*".equals(allowedOrigins[0])) {
            corsConfigBuilder = CorsConfigBuilder.forAnyOrigin();
        } else {
            corsConfigBuilder = CorsConfigBuilder.forOrigins(allowedOrigins);
        }
        return corsConfigBuilder.allowedRequestMethods(HttpMethod.GET).shortCircuit();
    }
}
