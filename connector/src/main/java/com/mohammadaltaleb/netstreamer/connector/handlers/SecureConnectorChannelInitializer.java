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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import java.net.URI;

public class SecureConnectorChannelInitializer extends ConnectorChannelInitializer {
    private final SslContext sslContext;

    public SecureConnectorChannelInitializer(URI uri, MessageListener messageListener, SslContext sslContext) {
        super(uri, messageListener);
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        super.initChannel(channel);

        SSLEngine engine = this.sslContext.newEngine(channel.alloc());
        channel.pipeline().addFirst("SslHandler", new SslHandler(engine));
    }
}
