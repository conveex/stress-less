# STRESS-LESS — Especificación MQTT
**Versión:** 1.0  
**Fase:** 0 — Contratos  
**Documento:** 2 de 3  
**Depende de:** Documentación base v1.1, Modelo ER v1.0  

---

## 1. Propósito

Este documento define el contrato completo de comunicación MQTT entre los componentes de STRESS-LESS: el ESP32 biométrico, el backend y el ESP32 hub. Incluye el árbol de topics, los schemas de payloads, los niveles de QoS, la política de retención de mensajes y el comportamiento ante errores.

Todo componente que publique o suscriba a MQTT debe respetar exactamente los schemas definidos aquí. Cambios en payloads deben actualizarse en este documento antes de modificar el código.

---

## 2. Broker MQTT

### 2.1 Configuración por ambiente

| Ambiente | Proveedor | Host | Puerto | TLS | Notas |
|---|---|---|---|---|---|
| Prototipo integrado | HiveMQ Cloud gratuito | `<cluster>.s1.eu.hivemq.cloud` | 8883 | Requerido | Credenciales por variable de entorno. |
| Alternativa gratuita | EMQX Cloud | `<cluster>.emqxsl.com` | 8883 | Requerido | Similar a HiveMQ Cloud. |
| Pruebas locales aisladas | Mosquitto local | `localhost` | 1883 | Opcional | Solo para desarrollo sin internet. |
| Producción futura | HiveMQ Enterprise / AWS IoT Core | A definir | 8883 | Requerido | Certificados por dispositivo. |

### 2.2 Credenciales por componente

| Componente | `clientId` | `username` | `password` |
|---|---|---|---|
| ESP32 biométrico (prototipo) | `band-sim-001` | `band-sim-001` | Token configurado en broker para ese clientId |
| ESP32 hub | `hub-001` | `hub-001` | `deviceToken` recibido durante registro |
| Backend (suscriptor/publicador) | `backend-stressless` | Usuario admin del broker | Contraseña admin del broker |

> **Regla de seguridad:** El backend valida que el `bandId` y `hubId` que aparecen dentro del payload correspondan al clientId autenticado que publicó el mensaje. Un cliente no puede publicar en topics de otro dispositivo.

### 2.3 Variables de entorno del backend

```env
MQTT_HOST=<cluster>.s1.eu.hivemq.cloud
MQTT_PORT=8883
MQTT_USERNAME=backend-stressless
MQTT_PASSWORD=<contraseña_admin_broker>
MQTT_CLIENT_ID=backend-stressless
MQTT_TLS=true
```

---

## 3. Árbol de topics

```
stressless/
├── band/
│   └── {bandId}/
│       └── biometrics          ← ESP32 biométrico publica; backend suscribe
│
├── hub/
│   └── {hubId}/
│       ├── status              ← Hub publica; backend suscribe
│       ├── commands            ← Backend publica; hub suscribe
│       ├── events              ← Hub publica; backend suscribe
│       └── devices             ← Hub publica al conectarse; backend suscribe
│
├── room/
│   └── {roomId}/
│       └── state               ← Backend publica; app (futura) suscribe
│
└── user/
    └── {userId}/
        └── stress              ← Backend publica; app (futura) suscribe
```

### 3.1 Dirección de publicación por topic

| Topic | Publicador | Suscriptor(es) | Descripción |
|---|---|---|---|
| `stressless/band/{bandId}/biometrics` | ESP32 biométrico | Backend | Lecturas biométricas crudas. |
| `stressless/hub/{hubId}/status` | Hub | Backend | Estado y heartbeat del hub. |
| `stressless/hub/{hubId}/commands` | Backend | Hub | Comandos ambientales al hub. |
| `stressless/hub/{hubId}/events` | Hub | Backend | Eventos operativos del hub. |
| `stressless/hub/{hubId}/devices` | Hub | Backend | Catálogo de dispositivos registrados. |
| `stressless/room/{roomId}/state` | Backend | App (Fase 5+) | Estado ambiental actual de la habitación. |
| `stressless/user/{userId}/stress` | Backend | App (Fase 5+) | Estado fisiológico actual del usuario. |

---

## 4. Niveles de QoS

| Topic | QoS | Justificación |
|---|---:|---|
| `biometrics` | **0** | Alta frecuencia (cada 5 s). La pérdida ocasional de una lectura no compromete la ventana temporal. Reducir overhead de confirmación. |
| `hub/status` | **1** | El estado del hub debe llegar al menos una vez para que el backend actualice `last_seen_at`. |
| `hub/commands` | **1** | Los comandos ambientales deben ejecutarse. Al menos una entrega garantizada. |
| `hub/events` | **1** | Los eventos operativos son importantes para auditoría y debugging. |
| `hub/devices` | **1** | El catálogo de dispositivos debe sincronizarse correctamente. |
| `room/state` | **0** | Informativo para la app. La app puede re-solicitar el estado actual via REST si lo pierde. |
| `user/stress` | **0** | Informativo para la app. El estado fisiológico actual también está disponible via REST. |

> **Nota sobre QoS 2:** No se usa en ningún topic. El overhead de QoS 2 no justifica su uso en este sistema dado que el backend ya persiste estados y la app puede hacer polling REST como fallback.

---

## 5. Retención de mensajes (Retained)

| Topic | Retained | Razón |
|---|---|---|
| `hub/status` | **Sí** | El backend y la app deben poder consultar el último estado conocido del hub al reconectarse. |
| `hub/devices` | **Sí** | El catálogo de dispositivos debe estar disponible para nuevos suscriptores sin esperar que el hub republique. |
| `biometrics` | **No** | Dato perecedero; solo tiene sentido en tiempo real. |
| `hub/commands` | **No** | Un comando retenido podría ejecutarse de forma indeseada al reconectar. |
| `hub/events` | **No** | Los eventos son históricos; se persisten en base de datos. |
| `room/state` | **Sí** | La app debe poder mostrar el estado actual al abrir sin esperar el siguiente ciclo. |
| `user/stress` | **Sí** | La app debe poder mostrar el estado fisiológico actual al abrir. |

---

## 6. Last Will and Testament (LWT)

El hub debe configurar LWT al conectarse al broker. Si la conexión se cae sin desconexión limpia, el broker publicará automáticamente el LWT.

### 6.1 Configuración del LWT del hub

| Parámetro | Valor |
|---|---|
| Topic | `stressless/hub/{hubId}/status` |
| QoS | 1 |
| Retained | Sí |
| Payload | Ver sección 7.2 |

### 6.2 Payload del LWT

```json
{
  "hubId": "hub-001",
  "status": "OFFLINE",
  "operational_state": "ERROR",
  "timestamp": null,
  "reason": "LWT"
}
```

> El campo `timestamp` es `null` en el LWT porque es generado por el broker, no por el hub. El backend usará `received_at` como referencia temporal.

---

## 7. Schemas de payloads

Todos los payloads son JSON. Los campos marcados como `requerido` deben estar presentes siempre. Los campos `nullable` pueden estar ausentes o ser `null`.

---

### 7.1 `stressless/band/{bandId}/biometrics`

Publicado por: **ESP32 biométrico**  
Frecuencia: cada **5 segundos**  
QoS: 0  

```json
{
  "bandId": "band-sim-001",
  "hubId": "hub-001",
  "bpm": 98.5,
  "gsr": 720.0,
  "movement": 0.08,
  "battery": 85,
  "source": "SIMULATED",
  "timestamp": "2026-05-14T12:30:00Z"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `bandId` | string | ✅ | Identificador de la pulsera. Debe coincidir con `band_id` en BD. |
| `hubId` | string | ✅ | Hub asociado. El backend valida la relación band-hub. |
| `bpm` | number | ✅ | Frecuencia cardiaca. Rango válido: 30–250. |
| `gsr` | number | ✅ | Valor de GSR (ADC crudo o procesado). Rango válido: 0–4095 (ADC 12-bit). |
| `movement` | number | ✅ | Estimación de movimiento. Rango: 0.000–1.000. |
| `battery` | integer | nullable | Nivel de batería 0–100. Puede omitirse en prototipo simulado. |
| `source` | string | ✅ | `"REAL"` o `"SIMULATED"`. |
| `timestamp` | string (ISO 8601) | ✅ | Timestamp UTC. Si el ESP32 no tiene NTP, usar `"1970-01-01T00:00:00Z"` y el backend usará `received_at`. |

**Validaciones del backend al recibir:**
1. `bandId` existe en tabla `bands` con `is_active = true`.
2. `hubId` corresponde al hub asociado al usuario de esa pulsera.
3. `bpm` está en rango 30–250; si no, descartar con log.
4. `movement` está en rango 0–1; si no, clampear.
5. Si `timestamp` es epoch (1970), usar `received_at` como timestamp efectivo.

---

### 7.2 `stressless/hub/{hubId}/status`

Publicado por: **Hub**  
Frecuencia: al conectarse, al cambiar estado y cada **30 segundos** (heartbeat)  
QoS: 1, Retained: Sí  

```json
{
  "hubId": "hub-001",
  "status": "ACTIVE",
  "operational_state": "ACTIVE",
  "firmware_version": "0.1.0",
  "ip_address": "192.168.1.105",
  "free_heap": 142000,
  "uptime_seconds": 3600,
  "timestamp": "2026-05-14T12:30:00Z"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `hubId` | string | ✅ | Identificador del hub. |
| `status` | string | ✅ | `"ACTIVE"`, `"OFFLINE"`, `"ERROR"`, `"PENDING"`. |
| `operational_state` | string | ✅ | Estado operativo: `ACTIVE`, `PAUSED`, `MANUAL`, `NO_DATA_MODE`, `EXIT_MODE`, `ERROR`, `LOCAL_ONLY`. |
| `firmware_version` | string | nullable | Versión del firmware. |
| `ip_address` | string | nullable | IP local del hub. |
| `free_heap` | integer | nullable | Bytes de heap libre del ESP32. Útil para debugging. |
| `uptime_seconds` | integer | nullable | Segundos desde el último reinicio. |
| `timestamp` | string (ISO 8601) | ✅ | Timestamp UTC del hub. |

**Acciones del backend al recibir:**
- Actualizar `hubs.status`, `hubs.operational_state`, `hubs.last_seen_at`, `hubs.firmware_version`, `hubs.ip_address`.
- Si `status = "OFFLINE"` o `"ERROR"`, registrar en `system_events`.

---

### 7.3 `stressless/hub/{hubId}/commands`

Publicado por: **Backend**  
QoS: 1  
Retained: No  

El backend puede enviar un comando a un solo dispositivo o un lote de comandos en una sola publicación.

**Comando individual:**

```json
{
  "commandId": "c1b2c3d4-0000-0000-0000-000000000099",
  "hubId": "hub-001",
  "source": "AUTOMATION",
  "actions": [
    {
      "deviceKey": "led-rgb-001",
      "action": "SET_BRIGHTNESS",
      "value": 30
    }
  ],
  "timestamp": "2026-05-14T12:35:00Z"
}
```

**Comando de perfil completo (múltiples acciones):**

```json
{
  "commandId": "c1b2c3d4-0000-0000-0000-000000000100",
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
      "action": "SET_COLOR_RGB",
      "value": { "r": 80, "g": 100, "b": 200 }
    },
    {
      "deviceKey": "fan-001",
      "action": "SET_SPEED",
      "value": "LOW"
    },
    {
      "deviceKey": "display-001",
      "action": "SHOW_MESSAGE",
      "value": "Respira profundo"
    }
  ],
  "timestamp": "2026-05-14T12:35:00Z"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `commandId` | string (UUID) | ✅ | ID único del comando. Usado para ACK y deduplicación. |
| `hubId` | string | ✅ | Hub destinatario. |
| `source` | string | ✅ | `"AUTOMATION"`, `"MANUAL_APP"`, `"SAFETY_PROFILE"`, `"EXIT_MODE"`. |
| `triggeredByState` | string | nullable | Estado fisiológico que originó el comando. |
| `actions` | array | ✅ | Lista de acciones. Al menos una acción requerida. |
| `actions[].deviceKey` | string | ✅ | Clave del dispositivo en el hub (ej. `"led-rgb-001"`). |
| `actions[].action` | string | ✅ | Acción del catálogo: `TURN_ON`, `TURN_OFF`, `SET_BRIGHTNESS`, `SET_COLOR_RGB`, `SET_COLOR_HEX`, `SET_SPEED`, `SET_TEMPERATURE`, `SET_MODE`, `SET_VOLUME`, `SHOW_MESSAGE`, `SET_POSITION`. |
| `actions[].value` | any | nullable | Valor de la acción. Tipo depende de la acción (ver tabla en sección 8). |
| `timestamp` | string (ISO 8601) | ✅ | Timestamp de generación del comando. |

**Comportamiento del hub al recibir:**
1. Parsear el JSON y extraer `actions`.
2. Ejecutar cada acción en orden según `order_index` implícito del array.
3. Publicar ACK en `hub/events` con el `commandId` (ver sección 7.4).
4. Si un dispositivo no existe o no responde, continuar con las demás acciones y reportar el error en el ACK.

---

### 7.4 `stressless/hub/{hubId}/events`

Publicado por: **Hub**  
QoS: 1  
Retained: No  

```json
{
  "hubId": "hub-001",
  "eventType": "COMMAND_ACK",
  "severity": "INFO",
  "commandId": "c1b2c3d4-0000-0000-0000-000000000100",
  "description": "Comandos ejecutados correctamente",
  "metadata": {
    "executed": ["led-rgb-001", "fan-001", "display-001"],
    "failed": []
  },
  "timestamp": "2026-05-14T12:35:01Z"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `hubId` | string | ✅ | Identificador del hub. |
| `eventType` | string | ✅ | Tipo de evento (ver catálogo abajo). |
| `severity` | string | ✅ | `"INFO"`, `"WARN"`, `"ERROR"`. |
| `commandId` | string (UUID) | nullable | Solo en eventos `COMMAND_ACK` o `COMMAND_FAILED`. |
| `description` | string | nullable | Descripción legible. |
| `metadata` | object | nullable | Datos adicionales según `eventType`. |
| `timestamp` | string (ISO 8601) | ✅ | Timestamp del hub. |

**Catálogo de eventType del hub:**

| eventType | Descripción | metadata típico |
|---|---|---|
| `COMMAND_ACK` | Comando recibido y ejecutado. | `{ "executed": [...], "failed": [] }` |
| `COMMAND_FAILED` | Error al ejecutar uno o más dispositivos. | `{ "executed": [...], "failed": [...], "error": "..." }` |
| `WIFI_DISCONNECTED` | Pérdida de WiFi detectada. | `{ "ssid": "..." }` |
| `WIFI_RECONNECTED` | Reconexión a WiFi exitosa. | `{ "ssid": "...", "ip": "..." }` |
| `MQTT_RECONNECTED` | Reconexión a broker exitosa. | `{ "broker": "..." }` |
| `MODE_CHANGED` | Cambio de estado operativo. | `{ "from": "ACTIVE", "to": "MANUAL" }` |
| `DEVICE_ERROR` | Un dispositivo no respondió. | `{ "deviceKey": "...", "error": "..." }` |
| `STARTUP` | Hub reiniciado o iniciado. | `{ "firmware_version": "...", "reason": "POWER_ON" }` |
| `EXIT_MODE_ACTIVATED` | Modo salida activado (botón físico o timeout). | `{}` |

---

### 7.5 `stressless/hub/{hubId}/devices`

Publicado por: **Hub** (al conectarse y bajo solicitud)  
QoS: 1  
Retained: Sí  

```json
{
  "hubId": "hub-001",
  "devices": [
    {
      "deviceKey": "led-rgb-001",
      "name": "LED RGB",
      "type": "LIGHT",
      "capabilities": ["ON_OFF", "BRIGHTNESS", "COLOR"],
      "currentState": { "on": true, "brightness": 80, "color": "#FFFFFF" }
    },
    {
      "deviceKey": "fan-001",
      "name": "Ventilador",
      "type": "FAN",
      "capabilities": ["ON_OFF", "SPEED"],
      "currentState": { "on": false }
    },
    {
      "deviceKey": "display-001",
      "name": "LCD 16x2",
      "type": "DISPLAY",
      "capabilities": ["ON_OFF", "MESSAGE"],
      "currentState": { "on": true, "message": "Iniciando..." }
    },
    {
      "deviceKey": "buzzer-001",
      "name": "Buzzer audio",
      "type": "AUDIO",
      "capabilities": ["ON_OFF", "VOLUME"],
      "currentState": { "on": false }
    }
  ],
  "timestamp": "2026-05-14T12:30:00Z"
}
```

**Acciones del backend al recibir:**
- Comparar con los dispositivos registrados en BD para ese `hubId`.
- Si hay dispositivos nuevos, registrarlos en `devices` y `device_capabilities`.
- Actualizar `current_state` de los dispositivos existentes.

---

### 7.6 `stressless/room/{roomId}/state`

Publicado por: **Backend**  
QoS: 0, Retained: Sí  

```json
{
  "roomId": "a1b2c3d4-0000-0000-0000-000000000002",
  "hubId": "hub-001",
  "operationalState": "ACTIVE",
  "activeProfile": "Calma profunda",
  "devices": [
    { "deviceKey": "led-rgb-001", "on": true, "brightness": 25, "color": "#5064C8" },
    { "deviceKey": "fan-001", "on": true, "speed": "LOW" },
    { "deviceKey": "display-001", "on": true, "message": "Respira profundo" },
    { "deviceKey": "buzzer-001", "on": false }
  ],
  "timestamp": "2026-05-14T12:35:02Z"
}
```

---

### 7.7 `stressless/user/{userId}/stress`

Publicado por: **Backend**  
QoS: 0, Retained: Sí  

```json
{
  "userId": "a1b2c3d4-0000-0000-0000-000000000001",
  "detectedState": "MODERATE_STRESS",
  "confidence": 0.76,
  "reason": {
    "bpm": "above_baseline",
    "gsr": "above_baseline",
    "movement": "low",
    "duration": "sustained"
  },
  "bpmCurrent": 98.5,
  "gsrCurrent": 720.0,
  "bpmBaseline": 70.0,
  "gsrBaseline": 500.0,
  "bandStatus": "CONNECTED",
  "bandBattery": 85,
  "timestamp": "2026-05-14T12:35:00Z"
}
```

---

## 8. Referencia de valores por acción

| Acción | Tipo de `value` | Formato / Rango | Ejemplo |
|---|---|---|---|
| `TURN_ON` | boolean | `true` | `true` |
| `TURN_OFF` | boolean | `false` | `false` |
| `SET_BRIGHTNESS` | integer | 0–100 | `30` |
| `SET_COLOR_RGB` | object | `{"r":0-255,"g":0-255,"b":0-255}` | `{"r":80,"g":100,"b":200}` |
| `SET_COLOR_HEX` | string | `#RRGGBB` | `"#5064C8"` |
| `SET_SPEED` | string o integer | `"LOW"`, `"MEDIUM"`, `"HIGH"` o 0–100 | `"LOW"` |
| `SET_TEMPERATURE` | number | Celsius (15.0–30.0 típico) | `22.5` |
| `SET_MODE` | string | Catálogo por dispositivo | `"ECO"`, `"SLEEP"`, `"RELAX"` |
| `SET_VOLUME` | integer | 0–100 | `25` |
| `SHOW_MESSAGE` | string | Máximo 32 chars (LCD 16x2: 2 líneas × 16) | `"Respira profundo"` |
| `SET_POSITION` | integer | 0–100 (0 = cerrado, 100 = abierto) | `20` |

---

## 9. Flujo completo del prototipo

```
ESP32 biométrico                 Broker MQTT              Backend               ESP32 Hub
      │                               │                       │                      │
      │──── PUBLISH biometrics ──────►│                       │                      │
      │     QoS 0, cada 5s            │                       │                      │
      │                               │──── DELIVER ─────────►│                      │
      │                               │                       │ Validar bandId/hubId  │
      │                               │                       │ Clasificar estado     │
      │                               │                       │ Guardar BD            │
      │                               │                       │ Decidir si aplica     │
      │                               │                       │ perfil ambiental      │
      │                               │◄─── PUBLISH commands ─│                      │
      │                               │     QoS 1             │                      │
      │                               │──── DELIVER ─────────────────────────────────►│
      │                               │                       │                      │ Ejecutar acciones
      │                               │                       │                      │ LED + LCD + Fan
      │                               │◄─── PUBLISH events ───────────────────────── │
      │                               │     COMMAND_ACK       │                      │
      │                               │──── DELIVER ─────────►│                      │
      │                               │                       │ Actualizar command    │
      │                               │                       │ status = ACKNOWLEDGED │
```

---

## 10. Comportamiento ante errores de conexión

### 10.1 ESP32 biométrico pierde WiFi

- Intentar reconexión cada 5 segundos con backoff exponencial (máx. 60 s).
- Mientras no hay conexión, continuar generando lecturas en buffer local (máx. 60 lecturas = 5 min).
- Al reconectar, publicar lecturas buffereadas en ráfaga (con `timestamp` original de cada una).
- El backend descarta lecturas con `timestamp` más viejo que la ventana de relevancia actual (definido como 5 minutos).

### 10.2 Hub pierde conexión MQTT

- Intentar reconexión MQTT cada 10 segundos.
- Si la reconexión falla más de 3 veces, entrar en `LOCAL_ONLY`.
- En `LOCAL_ONLY`: mantener último estado seguro, mostrar "Sin conexión" en LCD, dejar actuadores sin cambios.
- Al reconectar, publicar `hub/status` y `hub/devices` inmediatamente.
- Publicar evento `MQTT_RECONNECTED` en `hub/events`.

### 10.3 Backend pierde conexión con broker

- El cliente MQTT del backend debe configurar reconnect automático.
- Si la reconexión tarda más de 30 segundos, loguear `ERROR` en consola del backend.
- El backend no debe perder mensajes de `commands` pendientes — mantener cola local en memoria.

---

## 11. Pruebas de integración MQTT

### 11.1 Herramientas recomendadas

- **MQTT Explorer** (GUI): visualización del árbol de topics y payloads en tiempo real.
- **mosquitto_pub / mosquitto_sub** (CLI): publicar mensajes de prueba desde terminal.
- **Postman** (MQTT beta): útil para pruebas desde el mismo entorno que la API REST.

### 11.2 Secuencia de prueba mínima del prototipo

```bash
# Paso 1: Verificar que el hub se conecta y publica status
# → Abrir MQTT Explorer, conectarse al broker
# → Verificar que stressless/hub/hub-001/status llega con status=ACTIVE

# Paso 2: Simular datos biométricos desde terminal
mosquitto_pub -h <broker_host> -p 8883 --cafile ca.crt \
  -u "band-sim-001" -P "<password>" \
  -t "stressless/band/band-sim-001/biometrics" \
  -m '{"bandId":"band-sim-001","hubId":"hub-001","bpm":110.0,"gsr":850.0,"movement":0.05,"source":"SIMULATED","timestamp":"2026-05-14T12:30:00Z"}'

# Paso 3: Verificar en MQTT Explorer que el backend recibió, clasificó y publicó en:
# stressless/hub/hub-001/commands   ← debe llegar comando de perfil
# stressless/user/{userId}/stress   ← debe llegar estado HIGH_STRESS o MODERATE_STRESS

# Paso 4: Verificar que el hub ejecutó el comando
# → LED RGB debe cambiar brillo/color
# → LCD debe mostrar mensaje
# → stressless/hub/hub-001/events debe tener COMMAND_ACK
```

---

*Fin del documento — Especificación MQTT STRESS-LESS v1.0*
