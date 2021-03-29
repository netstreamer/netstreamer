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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EventHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(EventHandler.class);

    private final SubscriptionManager subscriptionManager;
    private final ChannelGroup channelGroup;

    EventHandler(SubscriptionManager subscriptionManager, ChannelGroup channelGroup) {
        this.subscriptionManager = subscriptionManager;
        this.channelGroup = channelGroup;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String uid = ctx.channel().id().asLongText();
        this.subscriptionManager.disconnectClient(uid);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            ctx.pipeline().remove(CorsHandler.class);
            ctx.pipeline().remove(HttpRequestHandler.class);
            this.channelGroup.add(ctx.channel());
        } else if (evt instanceof SslHandshakeCompletionEvent) {
            if (!((SslHandshakeCompletionEvent) evt).isSuccess()) {
                logger.error("Ssl Handshake Failure!");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof DecoderException && cause.getCause() instanceof NotSslRecordException) {
            logger.error(cause.getMessage());
            return;
        }
        logger.error(cause.getMessage(), cause);
        String uid = ctx.channel().id().asLongText();
        this.subscriptionManager.disconnectClient(uid);
        if (ctx.channel().isOpen()) {
            ctx.close();
        }
    }
}
