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
import com.mohammadaltaleb.netstreamer.payload.Payload;
import com.mohammadaltaleb.netstreamer.payload.PayloadFactory;
import com.mohammadaltaleb.netstreamer.payload.PayloadParsingException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mohammadaltaleb.netstreamer.payload.PayloadConstants.*;

class StreamerRequestHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(StreamerRequestHandler.class);

    private final SubscriptionManager subscriptionManager;
    private final PayloadFactory payloadFactory;

    StreamerRequestHandler(SubscriptionManager subscriptionManager, PayloadFactory payloadFactory) {
        this.subscriptionManager = subscriptionManager;
        this.payloadFactory = payloadFactory;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        String message = textWebSocketFrame.text();
        Payload payload;
        try {
            payload = this.payloadFactory.fromString(message);
        } catch (PayloadParsingException e) {
            logger.error("Failed to parse payload", e);
            return;
        }
        Channel channel = channelHandlerContext.channel();
        String uid = channel.id().asLongText();
        boolean isAuthenticated = this.subscriptionManager.hasClient(uid);

        logger.debug("Validating request payload message {}", message);
        if (!isValidPayload(payload, isAuthenticated)) {
            logger.error("Invalid request payload");
            return;
        }

        String action = payload.getField(REQUEST_ACTION_KEY);
        String param = payload.getField(REQUEST_PARAM_KEY);
        if (REQUEST_AUTH_ACTION.equals(action)) {
            connect(uid, param, channel);
        } else if (REQUEST_SUBSCRIBE_ACTION.equals(action)) {
            subscribe(uid, param.split(","));
        } else if (REQUEST_UNSUBSCRIBE_ACTION.equals(action)) {
            unsubscribe(uid, param.split(","));
        }
    }

    private boolean isValidPayload(Payload payload, boolean isAuthenticated) {
        if (!payload.hasField(REQUEST_ACTION_KEY)) {
            logger.debug("Request payload does not contain '{}' key", REQUEST_ACTION_KEY);
            return false;
        }
        if (!payload.hasField(REQUEST_PARAM_KEY)) {
            logger.debug("Request payload does not contain '{}' key", REQUEST_PARAM_KEY);
            return false;
        }
        String action = payload.getField(REQUEST_ACTION_KEY);
        if (!REQUEST_AUTH_ACTION.equals(action) && !REQUEST_SUBSCRIBE_ACTION.equals(action) && !REQUEST_UNSUBSCRIBE_ACTION.equals(action)) {
            logger.debug("Invalid request payload action '{}'", action);
            return false;
        }
        if ((REQUEST_SUBSCRIBE_ACTION.equals(action) || REQUEST_UNSUBSCRIBE_ACTION.equals(action)) && !isAuthenticated) {
            logger.debug("Client sent '{}' request but is not authenticated", action);
            return false;
        }
        return true;
    }

    private void connect(String uid, String authParam, Channel channel) {
        try {
            this.subscriptionManager.connectClient(uid, authParam, channel);
        } catch (Exception e) {
            logger.error("Error connecting client with uid {}. IP: {}. Closing channel...", uid, channel.remoteAddress(), e);
            channel.close();
        }
    }

    private void subscribe(String uid, String[] topics) {
        try {
            this.subscriptionManager.addSubscription(uid, topics);
        } catch (Exception e) {
            logger.error("Error adding {} subscriptions for client: {}", topics.length, uid, e);
        }
    }

    private void unsubscribe(String uid, String[] topics) {
        try {
            this.subscriptionManager.removeSubscription(uid, topics);
        } catch (Exception e) {
            logger.error("Error removing {} subscriptions for client: {}", topics.length, uid, e);
        }
    }
}
