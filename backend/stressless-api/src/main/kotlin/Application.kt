package com.stressless

import com.stressless.db.DatabaseFactory
import com.stressless.mqtt.MqttClientService
import com.stressless.plugins.configureCors
import com.stressless.plugins.configureMonitoring
import com.stressless.plugins.configureRouting
import com.stressless.plugins.configureSerialization
import com.stressless.plugins.configureStatusPages
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
    MqttClientService.init()

    configureMonitoring()
    configureCors()
    configureSerialization()
    configureStatusPages()
    configureRouting()
}