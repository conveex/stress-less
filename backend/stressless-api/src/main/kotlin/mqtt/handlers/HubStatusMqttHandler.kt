package com.stressless.mqtt.handlers

import com.stressless.dto.mqtt.HubStatusPayload
import com.stressless.repositories.HubRepository
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

object HubStatusMqttHandler {
    private val log = LoggerFactory.getLogger(HubStatusMqttHandler::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun handle(topic: String, payloadText: String) {
        if (!isHubStatusTopic(topic)) {
            return
        }

        try {
            log.info("Processing hub status MQTT message from topic={}", topic)

            val payload = json.decodeFromString<HubStatusPayload>(payloadText)

            val updatedRows = HubRepository.updateHubStatus(payload)

            if (updatedRows == 0) {
                log.warn(
                    "Hub status received but no hub was updated. hubId={} topic={}",
                    payload.hubId,
                    topic
                )
            } else {
                log.info(
                    "Hub status updated. hubId={} status={} operationalState={} updatedRows={}",
                    payload.hubId,
                    payload.status,
                    payload.operationalState,
                    updatedRows
                )
            }
        } catch (ex: Exception) {
            log.error(
                "Error processing hub status MQTT message. topic={} payload={}",
                topic,
                payloadText,
                ex
            )
        }
    }

    private fun isHubStatusTopic(topic: String): Boolean {
        return topic.matches(Regex("^stressless/hub/[^/]+/status$"))
    }
}