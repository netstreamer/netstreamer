# Netstreamer

Netstreamer is a general-purpose event streaming library built on top of the asynchronous event-driven network application framework Netty, and could be used to implement internet facing message brokers and publisher-subscriber systems.

The streamer uses a simple communication sub-protocol operating on the lower level WebSocket protocol to facilitate client authentication and topic subscription.


## Requirements

* Java 1.6+


## The communication protocol

Netstreamer uses WebSocket protocol for communication.
Clients send instructions in the form of actions, and the streamers emits events to notify or provide updates to the clients.

Note: The payload accepted and sent by Netstreamer is in JSON format by default,
but it can be changed by implementing the PayloadFactory and providing the implementation to NetstreamerConfig.

A client opens a WebSocket connection and sends an authentication action request
```json
{"action":"auth","param":"[ANY AUTHENTICATION PARAM]"}
```

The streamer will optionally use the param to authenticate the client and decide whether to accept or reject the request.

If the authentication process is successful, the client will receive a success status event
```json
{"event":"status","status":"success","message":"Client is successfully connected and authenticated"}
```

If the authentication fails, the client will receive a fail status event
```json
{"event":"status","status":"fail","message":"Failed to authenticate client"}
```

To subscribe to a topic or a group of topics, the client must send a subscribe action
```json
{"action":"subscribe","param":"[TOPIC1],[TOPIC2]"}
```

The streamer will publish topic update events to the subscribed clients
```json
{"event":"update","topic":"[TOPIC1]","update":"[UPDATE]"}
{"event":"update","topic":"[TOPIC2]","update":"[UPDATE]"}
```

To stop receiving topic updates, the client must send an unsubscribe action
```json
{"action":"unsubscribe","param":"[TOPIC1],[TOPIC2]"}
```


## Project Structure

The project contains the following maven modules
* **core**: has the Netstreamer implementation and other utils to help configure and set up the streamer.
* **connector**: has a really simple Netstreamer java connector implementation.
  It could be used for testing the streamer and understanding how should the protocol be used,
  but should not be relied on in real-world applications.
* **example**: contains example applications.


## Documentation

* [Javadoc](https://www.javadoc.io/doc/com.mohammadaltaleb.netstreamer)


## Binaries

Binaries and dependency information for Maven, Gradle and others can be found at [http://search.maven.org](https://search.maven.org/search?q=com.mohammadaltaleb.netstreamer).

### Maven
```xml
<dependency>
    <groupId>com.mohammadaltaleb.netstreamer</groupId>
    <artifactId>netstreamer-core</artifactId>
    <version>x.y.z</version>
</dependency>
```

### Gradle
```groovy
implementation group: 'com.mohammadaltaleb.netstreamer', name: 'netstreamer-core', version: 'x.y.z'
```


## Usage

### Creating a streamer

Netstreamer follows a convention over configuration approach.
It takes a configuration object which is a container of configuration values and only requires a port number for the streamer to bind to.
Other properties have default values.

The following example will create a new Netstreamer instance listening on port 8080,
and it will accept all incoming requests
```java
NetstreamerConfig netstreamerConfig = new NetstreamerConfig(8080);
Netstreamer netstreamer = new Netstreamer(netstreamerConfig);
netstreamer.start();
```

To be able to authenticate clients and receive subscription and unsubscription notifications,
a StreamerListener could be passed to the streamer
```java
netstreamer.setStreamerListener(new StreamerListener() {
    @Override
    public void onAuth(Client client, String authParam, ClientAuthenticationListener clientAuthenticationListener) {
        clientAuthenticationListener.accept(client);
        // or clientAuthenticationListener.reject(client);
    }

    @Override
    public void onSubscribe(Client client, String topic) {
    }

    @Override
    public void onUnsubscribe(Client client, String topic) {
    }
});
```

To enable Transport Layer Security (TLS), an SslContext should be passed to the configuration object
```java
SelfSignedCertificate cert = new SelfSignedCertificate();
SslContextBuilder sslContextBuilder = SslContextBuilder
        .forServer(cert.certificate(), cert.privateKey())
        .sslProvider(SslProvider.OPENSSL);
netstreamerConfig.setSslContext(sslContextBuilder.build());
```

Multiple configurations (like the host, WebSocket path, max frame size, allowed origins, etc.) could be easily set.
Check the NetstreamerConfig class properties to see the options.

### Connection, Authentication and Subscription

The following example shows how to use the simple connector found in the connector module to communicate with Netstreamer.
The use of the connector is not required (and not recommended). You can open a WebSocket connection and send actions using any library of your choice.
```java
String uriString = "ws://127.0.0.1:8080/";
NetstreamerConnector connector = new NetstreamerConnector(uriString, new MessageListener() {
    @Override
    public void onMessage(String message) {
        ObjectNode objectNode;
        try {
            objectNode = (ObjectNode) objectMapper.readTree(message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }
        String event = objectNode.get("event").asText();
        if ("status".equals(event)) {
            String status = objectNode.get("status").asText();
            String statusMessage = objectNode.get("message").asText();
            if ("success".equals(status)) {
                connector.subscribe("SOME.TOPIC");
            } else {
                System.err.println(String.format("Failed status message. status: %s. status message: %s", status, statusMessage));
            }
        } else if ("update".equals(event)) {
            String topic = objectNode.get("topic").asText();
            String updateString = objectNode.get("update").asText();
            System.out.println(String.format("Topic: %s. Update: %s", topic, updateString));
        }
    }
});
connector.connect();
connector.authenticate(UUID.randomUUID().toString());
```