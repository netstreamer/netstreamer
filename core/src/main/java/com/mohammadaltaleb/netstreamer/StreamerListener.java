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

import com.mohammadaltaleb.netstreamer.client.Client;

/**
 * A streamer listener is used to request client authentication and will be notified on each subscription adding or removing
 */
public interface StreamerListener {

    /**
     * Called on {@link Client} authentication request.
     * Clients with authentication request are queued and sent to this method using a dedicated thread one by one.
     * To accept a client, {@link ClientAuthenticationListener#accept(Client)} should be called.
     * To reject a client, {@link ClientAuthenticationListener#reject(Client)} should be called.
     * If neither accept nor reject are called, the client will be ignored, but the channel associated with it will not be closed.
     *
     * @param client                       The connected {@link Client}
     * @param authParam                    The authentication parameter sent with the client's "auth" request
     * @param clientAuthenticationListener The authentication listener that should be notified to accept or reject a client
     */
    void onAuth(Client client, String authParam, ClientAuthenticationListener clientAuthenticationListener);

    /**
     * Called when a {@link Client} subscribes to a topic
     *
     * @param topic  The subscription topic
     * @param client The {@link Client}
     */
    void onSubscribe(Client client, String topic);

    /**
     * Called when a {@link Client} unsubscribes from a topic
     *
     * @param topic  The topic unsubscribed from
     * @param client The {@link Client}
     */
    void onUnsubscribe(Client client, String topic);
}
