package com.sungrowpower;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe;
import com.sungrowpower.utils.DecryptUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service("subscriptionContainerClass")
public class SubscriptionContainerClass {
    @Value("${mqtt.client.id:sungrow-mqtt-client-test}")
    private String clientId;

    @Value("${sungrow.mqtt.host}")
    private String brokerHost;

    @Value("${sungrow.mqtt.port}")
    private int brokerPort;

    @Value("${sungrow.mqtt.username:}")
    private String username;

    @Value("${sungrow.mqtt.password:}")
    private String password;

    @Value("${sungrow.mqtt.rsa:}")
    private String rsa;

    @Value("${sungrow.mqtt.topics}")
    private String topics;

    private static final Logger log = LoggerFactory.getLogger(SubscriptionContainerClass.class);
    private Mqtt3AsyncClient mqttClient;
    private final Map<String, Consumer<Mqtt3Publish>> topicHandlers = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("init subscription container class");

        registerTopicHandlers();

        createAndConnectClient();
    }

    @PreDestroy
    public void destroy() {
        log.info("destroy subscription container class");
        if (mqttClient != null) {
            try {
                mqttClient.disconnect()
                        .whenComplete((voidValue, throwable) -> {
                            if (throwable != null) {
                                log.info("mqtt connection closed");
                            } else {
                                log.error("mqtt connection failed: {}", throwable.getMessage());
                            }
                        })
                        .join();
            } catch (Exception e) {
                log.error("shutdown subscription container class exception: {}", e.getMessage());
            }
        }
    }

    private void registerTopicHandlers() {
        List<String> topicList = Arrays.asList(topics.split(","));
        for (String topic : topicList) {
            topicHandlers.put(topic, publish -> {
                String payload = DecryptUtil.publicDecrypt(getPayloadString(publish), rsa);
                log.info("data: {}", payload);
            });
        }
    }

    private void createAndConnectClient() {
        mqttClient = Mqtt3Client.builder()
                .identifier(clientId)
                .serverHost(brokerHost)
                .serverPort(brokerPort)
                .automaticReconnect()
                .applyAutomaticReconnect()
                .simpleAuth()
                .username(username)
                .password(password.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .buildAsync();

        CompletableFuture<Void> connectFuture = mqttClient.connect()
                .thenAccept(connAck -> {
                    log.info("Successfully connected to the MQTT server: {}:{}", brokerHost, brokerPort);
                    subscribeToAllTopics();
                })
                .exceptionally(throwable -> {
                    log.error("MQTT connection failed: {}", throwable.getMessage(), throwable);
                    return null;
                });
        connectFuture.join();
    }


    private void subscribeToAllTopics() {
        String[] topics = topicHandlers.keySet().stream()
                .filter(topic -> !"default".equals(topic))
                .toArray(String[]::new);

        if (topics.length == 0) {
            log.warn("No subscription topics are set up.");
            return;
        }

        log.info("start subscribe {} topics", topics.length);

        for (String topic : topics) {
            subscribeToTopic(topic);
        }
    }

    private void subscribeToTopic(String topic) {
        Consumer<Mqtt3Publish> handler = topicHandlers.get(topic);
        if (handler == null) {
            handler = topicHandlers.get("default");
        }

        final Consumer<Mqtt3Publish> finalHandler = handler;

        mqttClient.subscribeWith()
                .topicFilter(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(publish -> {
                    try {
                        finalHandler.accept(publish);
                    } catch (Exception e) {
                        log.error("process msg exception [{}]: {}", topic, e.getMessage(), e);
                    }
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable == null) {
                        log.info("✅ sub success: {}", topic);
                    } else {
                        log.error("❌ sub fail: {} - {}", topic, throwable.getMessage());
                    }
                });
    }

    private String getPayloadString(Mqtt3Publish publish) {
        return new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
    }
}
