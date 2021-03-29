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

package com.mohammadaltaleb.netstreamer.connector.handlers;

import com.mohammadaltaleb.netstreamer.connector.MessageListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;

public class ConnectorChannelInitializer extends ChannelInitializer<Channel> {
    private final HandshakeHandler handshakeHandler;
    private final MessageListener messageListener;

    public ConnectorChannelInitializer(URI uri, MessageListener messageListener) {
        this.handshakeHandler = new HandshakeHandler(WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, false, EmptyHttpHeaders.INSTANCE
        ));
        this.messageListener = messageListener;
    }

    public HandshakeHandler getHandshakeHandler() {
        return this.handshakeHandler;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("HttpClientCodec", new HttpClientCodec());
        pipeline.addLast("HttpObjectAggregator", new HttpObjectAggregator(64 * 1024));
        pipeline.addLast("HandshakeHandler", this.handshakeHandler);
        pipeline.addLast("WebSocketFrameHandler", new WebSocketFrameHandler(this.messageListener));
    }
}
