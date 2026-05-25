package com.stressless.services

import com.stressless.dto.mqtt.BiometricsPayload
import com.stressless.dto.stress.StressClassificationResult

object StressClassifierService {

    fun classify(
        payload: BiometricsPayload,
        baselineBpm: Double,
        baselineGsr: Double,
        baselineMovement: Double
    ): StressClassificationResult {
        val bpmDelta = payload.bpm - baselineBpm
        val gsrDelta = payload.gsr - baselineGsr
        val movement = payload.movement

        val movementLevel = when {
            movement <= 0.30 -> "low"
            movement <= 0.60 -> "medium"
            else -> "high"
        }

        val bpmReason = when {
            bpmDelta >= 25 -> "strongly_above_baseline"
            bpmDelta >= 15 -> "above_baseline"
            bpmDelta <= -10 -> "below_baseline"
            else -> "near_baseline"
        }

        val gsrReason = when {
            gsrDelta >= 250 -> "strongly_above_baseline"
            gsrDelta >= 150 -> "above_baseline"
            gsrDelta <= -100 -> "below_baseline"
            else -> "near_baseline"
        }

        val result = when {
            bpmDelta >= 25 && gsrDelta >= 250 && movement <= 0.30 -> {
                StressClassificationResult(
                    state = "HIGH_STRESS",
                    confidence = 0.82,
                    bpmDelta = bpmDelta,
                    gsrDelta = gsrDelta,
                    movementAtDetection = movement,
                    reasonJson = """
                        {
                          "bpm": "$bpmReason",
                          "gsr": "$gsrReason",
                          "movement": "$movementLevel",
                          "duration": "instant_rule_prototype"
                        }
                    """.trimIndent()
                )
            }

            (bpmDelta >= 15 || gsrDelta >= 150) && movement <= 0.60 -> {
                StressClassificationResult(
                    state = "MODERATE_STRESS",
                    confidence = 0.68,
                    bpmDelta = bpmDelta,
                    gsrDelta = gsrDelta,
                    movementAtDetection = movement,
                    reasonJson = """
                        {
                          "bpm": "$bpmReason",
                          "gsr": "$gsrReason",
                          "movement": "$movementLevel",
                          "duration": "instant_rule_prototype"
                        }
                    """.trimIndent()
                )
            }

            payload.bpm <= baselineBpm - 5 && payload.gsr <= baselineGsr - 50 && movement <= baselineMovement + 0.10 -> {
                StressClassificationResult(
                    state = "RELAXED",
                    confidence = 0.70,
                    bpmDelta = bpmDelta,
                    gsrDelta = gsrDelta,
                    movementAtDetection = movement,
                    reasonJson = """
                        {
                          "bpm": "$bpmReason",
                          "gsr": "$gsrReason",
                          "movement": "$movementLevel",
                          "duration": "instant_rule_prototype"
                        }
                    """.trimIndent()
                )
            }

            else -> {
                StressClassificationResult(
                    state = "NORMAL",
                    confidence = 0.75,
                    bpmDelta = bpmDelta,
                    gsrDelta = gsrDelta,
                    movementAtDetection = movement,
                    reasonJson = """
                        {
                          "bpm": "$bpmReason",
                          "gsr": "$gsrReason",
                          "movement": "$movementLevel",
                          "duration": "instant_rule_prototype"
                        }
                    """.trimIndent()
                )
            }
        }

        return result
    }
}