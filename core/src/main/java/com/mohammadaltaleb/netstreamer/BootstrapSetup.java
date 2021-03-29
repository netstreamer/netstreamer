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

import com.mohammadaltaleb.netstreamer.config.Transport;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BootstrapSetup {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapSetup.class);

    private final int bossEventLoopGroupThreads;
    private final int workerEventLoopGroupThreads;

    private EventLoopGroup bossEventLoopGroup;
    private EventLoopGroup workerEventLoopGroup;
    private Class<? extends ServerChannel> channelClass;

    BootstrapSetup(Transport transport, int bossEventLoopGroupThreads, int workerEventLoopGroupThreads) {
        this.bossEventLoopGroupThreads = bossEventLoopGroupThreads;
        this.workerEventLoopGroupThreads = workerEventLoopGroupThreads;
        switch (transport) {
            case NIO:
                this.initNio();
                break;
            case EPOLL:
                this.initEPoll();
                break;
            case KQueue:
                this.initKQueue();
                break;
            default:
                throw new IllegalArgumentException("Unsupported Transport value");
        }
    }

    EventLoopGroup getBossGroup() {
        return this.bossEventLoopGroup;
    }

    EventLoopGroup getWorkerGroup() {
        return this.workerEventLoopGroup;
    }

    Class<? extends ServerChannel> getChannelClass() {
        return this.channelClass;
    }

    private void initNio() {
        logger.debug("Setting up a NIO Transport");
        this.bossEventLoopGroup = new NioEventLoopGroup(this.bossEventLoopGroupThreads);
        this.workerEventLoopGroup = new NioEventLoopGroup(this.workerEventLoopGroupThreads);
        this.channelClass = NioServerSocketChannel.class;
    }

    private void initEPoll() {
        logger.debug("Setting up a EPoll Transport");
        this.bossEventLoopGroup = new EpollEventLoopGroup(this.bossEventLoopGroupThreads);
        this.workerEventLoopGroup = new EpollEventLoopGroup(this.workerEventLoopGroupThreads);
        this.channelClass = EpollServerSocketChannel.class;
    }

    private void initKQueue() {
        logger.debug("Setting up a KQueue Transport");
        this.bossEventLoopGroup = new KQueueEventLoopGroup(this.bossEventLoopGroupThreads);
        this.workerEventLoopGroup = new KQueueEventLoopGroup(this.workerEventLoopGroupThreads);
        this.channelClass = KQueueServerSocketChannel.class;
    }
}