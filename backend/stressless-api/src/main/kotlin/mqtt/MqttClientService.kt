package com.stressless.mqtt

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.stressless.config.AppConfig
import com.stressless.dto.MqttHealthResponse
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object MqttClientService {
    private val log = LoggerFactory.getLogger(MqttClientService::class.java)

    private lateinit var client: Mqtt5AsyncClient

    private val connected = AtomicBoolean(false)
    private val messagesReceived = AtomicLong(0)

    @Volatile
    private var lastMessageTopic: String? = null

    @Volatile
    private var lastMessagePayloadPreview: String? = null

    private val subscriptions = mutableListOf<String>()

    fun init() {
        log.info("Initializing MQTT client for host {}:{}", AppConfig.mqttHost, AppConfig.mqttPort)

        val builder = MqttClient.builder()
            .useMqttVersion5()
            .identifier(AppConfig.mqttClientId)
            .serverHost(AppConfig.mqttHost)
            .serverPort(AppConfig.mqttPort)

        if (AppConfig.mqttTls) {
            builder.sslWithDefaultConfig()
        }

        client = builder.buildAsync()

        try {
            client.connectWith()
                .simpleAuth()
                .username(AppConfig.mqttUsername)
                .password(AppConfig.mqttPassword.toByteArray(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .send()
                .get(15, TimeUnit.SECONDS)

            connected.set(true)
            log.info("MQTT connected as clientId={}", AppConfig.mqttClientId)

            subscribeDevelopmentTopics()
        } catch (ex: Exception) {
            connected.set(false)
            log.error("MQTT connection failed: {}", ex.message, ex)
        }
    }

    private fun subscribeDevelopmentTopics() {
        subscribe("stressless/#", MqttQos.AT_LEAST_ONCE)
    }

    fun subscribe(topicFilter: String, qos: MqttQos) {
        if (!::client.isInitialized) {
            error("MQTT client has not been initialized")
        }

        client.subscribeWith()
            .topicFilter(topicFilter)
            .qos(qos)
            .callback { publish ->
                val topic = publish.topic.toString()
                val payload = publish.payload
                    .map { StandardCharsets.UTF_8.decode(it).toString() }
                    .orElse("")

                messagesReceived.incrementAndGet()
                lastMessageTopic = topic
                lastMessagePayloadPreview = payload.take(300)

                log.info("MQTT message received | topic={} | payload={}", topic, payload)
            }
            .send()
            .get(10, TimeUnit.SECONDS)

        subscriptions.add(topicFilter)
        log.info("MQTT subscribed to {}", topicFilter)
    }

    fun publishJson(
        topic: String,
        payload: String,
        qos: MqttQos = MqttQos.AT_LEAST_ONCE,
        retain: Boolean = false
    ) {
        if (!::client.isInitialized) {
            error("MQTT client has not been initialized")
        }

        if (!connected.get()) {
            error("MQTT client is not connected")
        }

        client.publishWith()
            .topic(topic)
            .qos(qos)
            .retain(retain)
            .payload(payload.toByteArray(StandardCharsets.UTF_8))
            .send()
            .get(10, TimeUnit.SECONDS)

        log.info("MQTT message published | topic={} | retain={} | payload={}", topic, retain, payload)
    }

    fun health(): MqttHealthResponse {
        return MqttHealthResponse(
            status = if (connected.get()) "OK" else "ERROR",
            connected = connected.get(),
            host = AppConfig.mqttHost,
            port = AppConfig.mqttPort,
            clientId = AppConfig.mqttClientId,
            subscriptions = subscriptions.toList(),
            messagesReceived = messagesReceived.get(),
            lastMessageTopic = lastMessageTopic,
            lastMessagePayloadPreview = lastMessagePayloadPreview
        )
    }
}