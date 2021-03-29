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
 * Methods of this interface will be invoked to accept or reject a {@link Client} during the authentication process
 */
public interface ClientAuthenticationListener {

    /**
     * Called to accept a {@link Client} if it is authenticated
     *
     * @param client The {@link Client} to accept
     */
    void accept(Client client);

    /**
     * Called to reject a {@link Client} if it is not authenticated
     *
     * @param client The {@link Client} to reject
     */
    void reject(Client client);
}
