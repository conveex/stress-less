# STRESS-LESS

STRESS-LESS es un proyecto IoT orientado a adaptar una habitación inteligente de acuerdo con señales fisiológicas asociadas al estrés del usuario.

El sistema integra una pulsera biométrica o ESP32 biométrico, un hub ESP32 de habitación, un backend cloud, una base de datos PostgreSQL, comunicación MQTT y una app Android.

## Objetivo del MVP

Construir un prototipo funcional donde:

1. Un ESP32 biométrico simulado publique lecturas por MQTT.
2. El backend reciba y valide los datos.
3. El backend clasifique el estado fisiológico usando reglas iniciales.
4. El backend guarde lecturas y estados en PostgreSQL.
5. El backend publique comandos MQTT al hub.
6. El ESP32 hub ejecute acciones en actuadores como LED RGB, LCD, ventilador o buzzer.
7. La app Android visualice estado actual, controles y configuración básica.

## Stack inicial

| Área | Tecnología |
|---|---|
| Backend | Kotlin + Ktor |
| Base de datos | PostgreSQL |
| App Android | Kotlin + Jetpack Compose |
| Firmware | ESP32 + Arduino IDE |
| Broker MQTT | HiveMQ Cloud o EMQX Cloud |
| Pruebas API | Postman |
| Gestión BD | DBeaver |
| Versionamiento | Git + GitHub |