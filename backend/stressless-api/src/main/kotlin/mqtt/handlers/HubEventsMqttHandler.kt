package com.stressless.mqtt.handlers

import com.stressless.dto.mqtt.HubEventPayload
import com.stressless.repositories.CommandRepository
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

object HubEventsMqttHandler {
    private val log = LoggerFactory.getLogger(HubEventsMqttHandler::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun handle(topic: String, payloadText: String) {
        if (!isHubEventsTopic(topic)) {
            return
        }

        try {
            log.info("Processing hub event MQTT message from topic={}", topic)

            val payload = json.decodeFromString<HubEventPayload>(payloadText)

            when (payload.eventType) {
                "COMMAND_ACK" -> handleCommandAck(payload)
                "COMMAND_FAILED" -> handleCommandFailed(payload)
                else -> {
                    log.info(
                        "Hub event received but no persistence action configured. eventType={} hubId={}",
                        payload.eventType,
                        payload.hubId
                    )
                }
            }
        } catch (ex: Exception) {
            log.error("Error processing hub event MQTT message. topic={} payload={}", topic, payloadText, ex)
        }
    }

    private fun isHubEventsTopic(topic: String): Boolean {
        return topic.matches(Regex("^stressless/hub/[^/]+/events$"))
    }

    private fun handleCommandAck(payload: HubEventPayload) {
        val commandId = payload.commandId

        if (commandId.isNullOrBlank()) {
            log.warn("COMMAND_ACK ignored because commandId is missing. hubId={}", payload.hubId)
            return
        }

        val updatedRows = CommandRepository.acknowledgeCommandByPayloadCommandId(commandId)

        if (updatedRows == 0) {
            log.warn(
                "COMMAND_ACK received but no SENT command was updated. commandId={} hubId={}",
                commandId,
                payload.hubId
            )
        } else {
            log.info(
                "Command acknowledged successfully. commandId={} hubId={} updatedRows={}",
                commandId,
                payload.hubId,
                updatedRows
            )
        }
    }

    private fun handleCommandFailed(payload: HubEventPayload) {
        val commandId = payload.commandId

        if (commandId.isNullOrBlank()) {
            log.warn("COMMAND_FAILED ignored because commandId is missing. hubId={}", payload.hubId)
            return
        }

        val updatedRows = CommandRepository.failCommandByPayloadCommandId(commandId)

        if (updatedRows == 0) {
            log.warn(
                "COMMAND_FAILED received but no SENT command was updated. commandId={} hubId={}",
                commandId,
                payload.hubId
            )
        } else {
            log.info(
                "Command marked as failed. commandId={} hubId={} updatedRows={}",
                commandId,
                payload.hubId,
                updatedRows
            )
        }
    }
}