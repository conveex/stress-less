# STRESS-LESS — Pruebas MQTT manuales

## Broker

Ambiente: Development  
Broker: HiveMQ Cloud  
Puerto: 8883  
TLS: Sí

## Credenciales de prueba

Solicitarlas a un desarrollador

Usuarios esperados:

- backend-stressless
- band-sim-001
- hub-001

## Topics principales

| Topic | QoS | Retained | Publicador esperado | Descripción |
|---|---:|---|---|---|
| stressless/hub/hub-001/status | 1 | Sí | Hub | Estado del hub |
| stressless/hub/hub-001/devices | 1 | Sí | Hub | Catálogo de dispositivos |
| stressless/band/band-sim-001/biometrics | 0 | No | Band | Lecturas biométricas |
| stressless/hub/hub-001/commands | 1 | No | Backend | Comandos hacia hub |
| stressless/hub/hub-001/events | 1 | No | Hub | Eventos y ACK |
| stressless/user/a1b2c3d4-0000-0000-0000-000000000001/stress | 0 | Sí | Backend | Estado fisiológico actual |
| stressless/room/a1b2c3d4-0000-0000-0000-000000000002/state | 0 | Sí | Backend | Estado actual de habitación |

## Orden de prueba manual

1. Conectar MQTT Explorer usando usuario `backend-stressless`.
2. Publicar `hub_status_active.json` en `stressless/hub/hub-001/status` con QoS 1 y retained true.
3. Publicar `hub_devices.json` en `stressless/hub/hub-001/devices` con QoS 1 y retained true.
4. Publicar `biometrics_normal.json` en `stressless/band/band-sim-001/biometrics` con QoS 0 y retained false.
5. Publicar `biometrics_high_stress.json` en `stressless/band/band-sim-001/biometrics` con QoS 0 y retained false.
6. Publicar `command_high_stress_profile.json` en `stressless/hub/hub-001/commands` con QoS 1 y retained false.
7. Publicar `hub_command_ack.json` en `stressless/hub/hub-001/events` con QoS 1 y retained false.
8. Publicar `user_stress_high.json` en `stressless/user/a1b2c3d4-0000-0000-0000-000000000001/stress` con QoS 0 y retained true.
9. Publicar `room_state_high_stress.json` en `stressless/room/a1b2c3d4-0000-0000-0000-000000000002/state` con QoS 0 y retained true.

## Resultado esperado

MQTT Explorer debe mostrar el árbol:

```text
stressless/
├── hub/
│   └── hub-001/
│       ├── status
│       ├── devices
│       ├── commands
│       └── events
├── band/
│   └── band-sim-001/
│       └── biometrics
├── user/
│   └── a1b2c3d4-0000-0000-0000-000000000001/
│       └── stress
└── room/
    └── a1b2c3d4-0000-0000-0000-000000000002/
        └── state
```