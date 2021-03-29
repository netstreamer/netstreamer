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

package com.mohammadaltaleb.netstreamer.payload;

/**
 * {@link Payload} key and value constants
 */
public interface PayloadConstants {
    String REQUEST_ACTION_KEY = "action";
    String REQUEST_PARAM_KEY = "param";

    String REQUEST_AUTH_ACTION = "auth";
    String REQUEST_SUBSCRIBE_ACTION = "subscribe";
    String REQUEST_UNSUBSCRIBE_ACTION = "unsubscribe";

    String RESPONSE_EVENT_KEY = "event";
    String RESPONSE_STATUS_KEY = "status";
    String RESPONSE_MESSAGE_KEY = "message";
    String RESPONSE_TOPIC_KEY = "topic";
    String RESPONSE_UPDATE_KEY = "update";

    String RESPONSE_STATUS_EVENT = "status";
    String RESPONSE_UPDATE_EVENT = "update";
    String RESPONSE_SUCCESS_STATUS = "success";
    String RESPONSE_FAIL_STATUS = "fail";
}
