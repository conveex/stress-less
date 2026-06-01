package com.stressless.mqtt.handlers

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.stressless.dto.mqtt.BiometricsPayload
import com.stressless.mqtt.MqttClientService
import com.stressless.repositories.BiometricsRepository
import com.stressless.services.StressClassifierService
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

object BiometricsMqttHandler {
    private val log = LoggerFactory.getLogger(BiometricsMqttHandler::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun handle(topic: String, payloadText: String) {
        if (!isBiometricsTopic(topic)) {
            return
        }

        try {
            log.info("Processing biometrics MQTT message from topic={}", topic)

            val payload = json.decodeFromString<BiometricsPayload>(payloadText)

            validatePayloadOrThrow(payload)

            val context = BiometricsRepository.findContext(payload)

            if (context == null) {
                log.warn(
                    "Biometrics ignored. No valid band/hub context found. bandId={} hubId={}",
                    payload.bandId,
                    payload.hubId
                )
                return
            }

            val biometricEventId = BiometricsRepository.saveBiometricEvent(payload, context)
            BiometricsRepository.updateBandLastSeen(payload)

            val classification = StressClassifierService.classify(
                payload = payload,
                baselineBpm = context.baselineBpm,
                baselineGsr = context.baselineGsr,
                baselineMovement = context.baselineMovement
            )

            val activeProfileId = BiometricsRepository.findActiveProfileId(
                userId = context.userId,
                state = classification.state
            )

            val detectedStateId = BiometricsRepository.saveDetectedState(
                context = context,
                classification = classification,
                profileApplied = activeProfileId
            )

            log.info(
                "Biometrics processed. eventId={} stateId={} state={} confidence={}",
                biometricEventId,
                detectedStateId,
                classification.state,
                classification.confidence
            )

            publishUserStressState(
                context = context,
                payload = payload,
                classification = classification
            )

            if (shouldPublishCommand(classification.state, classification.confidence, activeProfileId)) {
                val lastAppliedState = BiometricsRepository.findLastAutomationCommandState(context)

                if (lastAppliedState == classification.state) {
                    log.info(
                        "Automation command skipped. State already applied. state={} userId={} hubId={}",
                        classification.state,
                        context.userId,
                        payload.hubId
                    )
                } else {
                    publishCommandIfProfileExists(
                        context = context,
                        payload = payload,
                        classificationState = classification.state,
                        activeProfileId = activeProfileId,
                        detectedStateId = detectedStateId
                    )
                }
            }
        } catch (ex: Exception) {
            log.error("Error processing biometrics MQTT message. topic={} payload={}", topic, payloadText, ex)
        }
    }

    private fun isBiometricsTopic(topic: String): Boolean {
        return topic.matches(Regex("^stressless/band/[^/]+/biometrics$"))
    }

    private fun validatePayloadOrThrow(payload: BiometricsPayload) {
        require(payload.bandId.isNotBlank()) { "bandId is required" }
        require(payload.hubId.isNotBlank()) { "hubId is required" }
        require(payload.bpm in 30.0..250.0) { "bpm out of range: ${payload.bpm}" }
        require(payload.gsr in 0.0..4095.0) { "gsr out of range: ${payload.gsr}" }
        require(payload.movement in 0.0..1.0) { "movement out of range: ${payload.movement}" }
        require(payload.source == "SIMULATED" || payload.source == "REAL") { "source must be REAL or SIMULATED" }

        payload.battery?.let {
            require(it in 0..100) { "battery out of range: $it" }
        }
    }

    private fun shouldPublishCommand(
        state: String,
        confidence: Double,
        activeProfileId: UUID?
    ): Boolean {
        if (confidence < 0.60) {
            return false
        }

        if (activeProfileId == null) {
            return false
        }

        return state in setOf(
            "HIGH_STRESS",
            "MODERATE_STRESS",
            "NORMAL",
            "RELAXED",
            "MODERATE_RELAXED"
        )
    }

    private fun publishCommandIfProfileExists(
        context: BiometricsRepository.BandHubContext,
        payload: BiometricsPayload,
        classificationState: String,
        activeProfileId: UUID?,
        detectedStateId: UUID
    ) {
        if (activeProfileId == null) {
            log.info("No active profile for state={}. Command will not be published.", classificationState)
            return
        }

        val actionsJson = BiometricsRepository.loadProfileCommandActions(activeProfileId)

        val commandId = UUID.randomUUID()

        val commandPayload = """
            {
              "commandId": "$commandId",
              "hubId": "${payload.hubId}",
              "source": "AUTOMATION",
              "triggeredByState": "$classificationState",
              "actions": $actionsJson,
              "timestamp": "${Instant.now()}"
            }
        """.trimIndent()

        BiometricsRepository.saveCommand(
            context = context,
            source = "AUTOMATION",
            detectedStateId = detectedStateId,
            payload = commandPayload
        )

        MqttClientService.publishJson(
            topic = "stressless/hub/${payload.hubId}/commands",
            payload = commandPayload,
            qos = MqttQos.AT_LEAST_ONCE,
            retain = false
        )

        log.info(
            "Automation command published. commandId={} hubId={} state={} profileId={}",
            commandId,
            payload.hubId,
            classificationState,
            activeProfileId
        )
    }

    private fun publishUserStressState(
        context: BiometricsRepository.BandHubContext,
        payload: BiometricsPayload,
        classification: com.stressless.dto.stress.StressClassificationResult
    ) {
        val stressPayload = """
            {
              "userId": "${context.userId}",
              "detectedState": "${classification.state}",
              "confidence": ${classification.confidence},
              "reason": ${classification.reasonJson},
              "bpmCurrent": ${payload.bpm},
              "gsrCurrent": ${payload.gsr},
              "bpmBaseline": ${context.baselineBpm},
              "gsrBaseline": ${context.baselineGsr},
              "bandStatus": "CONNECTED",
              "bandBattery": ${payload.battery ?: "null"},
              "timestamp": "${Instant.now()}"
            }
        """.trimIndent()

        MqttClientService.publishJson(
            topic = "stressless/user/${context.userId}/stress",
            payload = stressPayload,
            qos = MqttQos.AT_MOST_ONCE,
            retain = true
        )
    }
}