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

package com.mohammadaltaleb.netstreamer.example.filestreamer;

import com.mohammadaltaleb.netstreamer.ClientAuthenticationListener;
import com.mohammadaltaleb.netstreamer.Netstreamer;
import com.mohammadaltaleb.netstreamer.StreamerListener;
import com.mohammadaltaleb.netstreamer.client.Client;
import com.mohammadaltaleb.netstreamer.config.NetstreamerConfig;
import com.mohammadaltaleb.netstreamer.payload.Payload;
import com.mohammadaltaleb.netstreamer.payload.PayloadFactory;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.concurrent.atomic.AtomicInteger;

public class FileStreamer implements StreamerListener {
    private static final Logger logger = Logger.getLogger(FileStreamer.class);

    private final AtomicInteger subscriptions = new AtomicInteger();
    private final Netstreamer streamer;
    private final PayloadFactory payloadFactory;
    private final File dataFile;
    private final String subscriptionTopic;

    FileStreamer(int port, boolean isSecure, File dataFile, String subscriptionTopic) {
        NetstreamerConfig netstreamerConfig = null;
        try {
            netstreamerConfig = this.buildStreamerConfig(port, isSecure);
        } catch (Exception e) {
            logger.error("Failed to build file streaming server config", e);
            System.exit(-1);
        }
        this.streamer = new Netstreamer(netstreamerConfig);
        this.streamer.setStreamerListener(this);
        this.payloadFactory = netstreamerConfig.getPayloadFactory();
        this.dataFile = dataFile;
        this.subscriptionTopic = subscriptionTopic;
    }

    @Override
    public void onAuth(Client client, String authParam, ClientAuthenticationListener clientAuthenticationListener) {
        clientAuthenticationListener.accept(client);
    }

    @Override
    public void onSubscribe(Client client, String topic) {
        this.subscriptions.incrementAndGet();
    }

    @Override
    public void onUnsubscribe(Client client, String topic) {
        this.subscriptions.decrementAndGet();
    }

    void start() {
        this.streamer.start();
        this.waitForFirstSubscription();
        logger.info("Start data publishing...");
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(this.dataFile));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Payload payload = this.payloadFactory.emptyPayload();
                payload.addField("line", line);
                payload.addField("time", String.valueOf(System.currentTimeMillis()));
                this.streamer.publish(subscriptionTopic, payload.toString());
                this.sleep(1);
            }
        } catch (IOException e) {
            logger.error("Error while publishing data!", e);
            System.exit(-1);
        }
        logger.info("Finished data publishing");
    }

    private void waitForFirstSubscription() {
        logger.info("Waiting for first subscription...");
        while (this.subscriptions.get() == 0) {
            this.sleep(1000);
        }
        logger.info("Received first subscription. Waiting 5 seconds before start publishing...");
        this.sleep(5000);
    }

    private NetstreamerConfig buildStreamerConfig(int port, boolean isSecure) throws CertificateException, SSLException {
        NetstreamerConfig netstreamerConfig = new NetstreamerConfig(port);
        if (isSecure) {
            SelfSignedCertificate cert = new SelfSignedCertificate();
            SslContextBuilder sslContextBuilder = SslContextBuilder
                    .forServer(cert.certificate(), cert.privateKey())
                    .sslProvider(SslProvider.OPENSSL);
            netstreamerConfig.setSslContext(sslContextBuilder.build());
        }
        return netstreamerConfig;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.error("InterruptException while waiting to publish data!", e);
            System.exit(-1);
        }
    }
}
