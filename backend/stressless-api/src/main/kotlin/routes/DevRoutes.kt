package com.stressless.routes

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.stressless.mqtt.MqttClientService
import com.stressless.repositories.DbHealthRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.time.Instant

fun Route.devRoutes() {
    get("/api/v1/dev/db-health") {
        call.respond(DbHealthRepository.check())
    }

    get("/api/v1/dev/mqtt-health") {
        call.respond(MqttClientService.health())
    }

    post("/api/v1/dev/mqtt-publish-test") {
        val payload = """
            {
              "commandId": "c1b2c3d4-0000-0000-0000-000000000777",
              "hubId": "hub-001",
              "source": "AUTOMATION",
              "triggeredByState": "HIGH_STRESS",
              "actions": [
                {
                  "deviceKey": "led-rgb-001",
                  "action": "SET_BRIGHTNESS",
                  "value": 25
                },
                {
                  "deviceKey": "led-rgb-001",
                  "action": "SET_COLOR_HEX",
                  "value": "#5064C8"
                },
                {
                  "deviceKey": "display-001",
                  "action": "SHOW_MESSAGE",
                  "value": "Respira profundo"
                }
              ],
              "timestamp": "${Instant.now()}"
            }
        """.trimIndent()

        MqttClientService.publishJson(
            topic = "stressless/hub/hub-001/commands",
            payload = payload,
            qos = MqttQos.AT_LEAST_ONCE,
            retain = false
        )

        call.respond(
            HttpStatusCode.Accepted,
            mapOf(
                "status" to "PUBLISHED",
                "topic" to "stressless/hub/hub-001/commands",
                "timestamp" to Instant.now().toString()
            )
        )
    }
}