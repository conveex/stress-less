# STRESS-LESS — Especificación API REST
**Versión:** 1.0  
**Fase:** 0 — Contratos  
**Documento:** 3 de 3  
**Depende de:** Documentación base v1.1, Modelo ER v1.0, Especificación MQTT v1.0  

---

## 1. Propósito

Este documento define el contrato completo de la API REST del backend de STRESS-LESS. Incluye todos los endpoints, métodos, headers de autenticación, request bodies, response bodies y códigos de error.

La app Android y cualquier cliente que consuma esta API deben respetar exactamente los contratos definidos aquí. El backend debe implementar exactamente estas respuestas.

---

## 2. Convenciones generales

### 2.1 URL base

```
https://<backend-host>/api/v1
```

Para el prototipo en Render o Railway:
```
https://stressless-api.onrender.com/api/v1
```

### 2.2 Formato

- Todos los requests y responses usan `Content-Type: application/json`.
- Los timestamps están en formato ISO 8601 UTC: `"2026-05-14T12:30:00Z"`.
- Los IDs son UUID v4 como strings: `"a1b2c3d4-0000-0000-0000-000000000001"`.
- Los campos opcionales ausentes se omiten del response (no se devuelven como `null` a menos que se especifique).

### 2.3 Autenticación

Todos los endpoints excepto `/auth/register` y `/auth/login` requieren header:

```
Authorization: Bearer <jwt_token>
```

El JWT tiene expiración de **24 horas**. El cliente debe renovarlo con `/auth/refresh`.

### 2.4 Códigos de estado HTTP

| Código | Significado |
|---|---|
| 200 | OK — Operación exitosa. |
| 201 | Created — Recurso creado. |
| 204 | No Content — Operación exitosa sin body. |
| 400 | Bad Request — Payload inválido o falta campo requerido. |
| 401 | Unauthorized — Token ausente, inválido o expirado. |
| 403 | Forbidden — Token válido pero sin permisos para el recurso. |
| 404 | Not Found — Recurso no encontrado. |
| 409 | Conflict — Conflicto de estado (ej. pulsera ya activa). |
| 422 | Unprocessable Entity — Datos semánticamente inválidos. |
| 500 | Internal Server Error — Error inesperado del servidor. |

### 2.5 Formato de error estándar

```json
{
  "error": "BAND_ALREADY_ACTIVE",
  "message": "El usuario ya tiene una pulsera activa. Desactívala antes de activar otra.",
  "timestamp": "2026-05-14T12:30:00Z"
}
```

| Campo | Descripción |
|---|---|
| `error` | Código de error en UPPER_SNAKE_CASE. Usado por la app para lógica de manejo. |
| `message` | Descripción legible para debugging. No mostrar al usuario final directamente. |
| `timestamp` | Momento del error. |

---

## 3. Autenticación — `/auth`

---

### POST `/auth/register`

Registra un nuevo usuario.

**Request:**
```json
{
  "name": "María García",
  "email": "maria@ejemplo.com",
  "password": "contraseña_segura_123"
}
```

**Response 201:**
```json
{
  "userId": "a1b2c3d4-0000-0000-0000-000000000001",
  "name": "María García",
  "email": "maria@ejemplo.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2026-05-15T12:30:00Z"
}
```

**Errores:**
- `400 EMAIL_INVALID` — Formato de email inválido.
- `400 PASSWORD_TOO_SHORT` — Contraseña menor a 8 caracteres.
- `409 EMAIL_ALREADY_REGISTERED` — Email ya registrado.

---

### POST `/auth/login`

Inicia sesión y obtiene JWT.

**Request:**
```json
{
  "email": "maria@ejemplo.com",
  "password": "contraseña_segura_123"
}
```

**Response 200:**
```json
{
  "userId": "a1b2c3d4-0000-0000-0000-000000000001",
  "name": "María García",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2026-05-15T12:30:00Z"
}
```

**Errores:**
- `401 INVALID_CREDENTIALS` — Email o contraseña incorrectos.
- `403 ACCOUNT_DELETED` — La cuenta fue eliminada.

---

### POST `/auth/refresh`

Renueva el JWT antes de que expire.

**Headers:** `Authorization: Bearer <token>`

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2026-05-16T12:30:00Z"
}
```

**Errores:**
- `401 TOKEN_EXPIRED` — Token expirado; el usuario debe hacer login nuevamente.
- `401 TOKEN_INVALID` — Token malformado.

---

## 4. Usuario — `/users`

---

### GET `/users/me`

Obtiene el perfil del usuario autenticado.

**Response 200:**
```json
{
  "userId": "a1b2c3d4-0000-0000-0000-000000000001",
  "name": "María García",
  "email": "maria@ejemplo.com",
  "isCalibrated": false,
  "baselineBpm": 70.0,
  "baselineGsr": 500.0,
  "disconnectionPolicy": "KEEP_LAST",
  "dataRetentionConsent": true,
  "createdAt": "2026-05-14T10:00:00Z"
}
```

---

### PATCH `/users/me`

Actualiza campos editables del perfil.

**Request (todos los campos son opcionales):**
```json
{
  "name": "María G.",
  "disconnectionPolicy": "RETURN_NORMAL",
  "dataRetentionConsent": true
}
```

**Response 200:** Igual a `GET /users/me` con los datos actualizados.

**Errores:**
- `422 INVALID_DISCONNECTION_POLICY` — Valor no permitido.

---

### DELETE `/users/me`

Elimina la cuenta del usuario. Aplica soft delete y programa purga de datos biométricos.

**Request:**
```json
{
  "confirmPassword": "contraseña_segura_123"
}
```

**Response 204:** Sin body.

**Errores:**
- `401 INVALID_CREDENTIALS` — Contraseña de confirmación incorrecta.

---

## 5. Calibración — `/users/me/calibration`

---

### GET `/users/me/calibration`

Obtiene el estado de calibración del usuario.

**Response 200:**
```json
{
  "isCalibrated": true,
  "baselineBpm": 68.5,
  "baselineGsr": 482.0,
  "baselineMovement": 0.08,
  "calibratedAt": "2026-05-14T11:00:00Z"
}
```

---

### POST `/users/me/calibration/start`

Inicia una sesión de calibración. El backend comenzará a recolectar lecturas de la pulsera activa durante los próximos 5 minutos.

**Request:** Sin body.

**Response 200:**
```json
{
  "calibrationSessionId": "cal-session-001",
  "startedAt": "2026-05-14T11:00:00Z",
  "durationSeconds": 300,
  "instructions": "Permanece en reposo durante 5 minutos. Evita movimiento y actividad intensa."
}
```

**Errores:**
- `409 NO_ACTIVE_BAND` — No hay pulsera activa registrada.
- `409 CALIBRATION_IN_PROGRESS` — Ya hay una sesión de calibración activa.

---

### POST `/users/me/calibration/complete`

El backend calcula y guarda la línea base con las lecturas recolectadas durante la sesión.

**Request:** Sin body.

**Response 200:**
```json
{
  "isCalibrated": true,
  "baselineBpm": 68.5,
  "baselineGsr": 482.0,
  "baselineMovement": 0.08,
  "readingsUsed": 58,
  "readingsDiscarded": 2,
  "calibratedAt": "2026-05-14T11:05:00Z"
}
```

**Errores:**
- `409 INSUFFICIENT_READINGS` — No hubo suficientes lecturas válidas (mínimo 30).
- `409 NO_CALIBRATION_SESSION` — No hay sesión de calibración activa.

---

## 6. Pulseras — `/bands`

---

### GET `/bands`

Lista las pulseras del usuario autenticado.

**Response 200:**
```json
{
  "bands": [
    {
      "bandId": "a1b2c3d4-0000-0000-0000-000000000004",
      "bandLogicalId": "band-sim-001",
      "serialNumber": null,
      "isActive": true,
      "status": "CONNECTED",
      "batteryLevel": 85,
      "lastSeenAt": "2026-05-14T12:29:55Z",
      "createdAt": "2026-05-14T10:00:00Z"
    }
  ]
}
```

---

### POST `/bands`

Registra una nueva pulsera para el usuario.

**Request:**
```json
{
  "bandLogicalId": "band-sim-001",
  "serialNumber": "SL-2024-001"
}
```

**Response 201:**
```json
{
  "bandId": "a1b2c3d4-0000-0000-0000-000000000004",
  "bandLogicalId": "band-sim-001",
  "isActive": false,
  "status": "REGISTERED",
  "createdAt": "2026-05-14T10:00:00Z"
}
```

**Errores:**
- `409 BAND_ID_ALREADY_REGISTERED` — El `bandLogicalId` ya está asignado a otro usuario.

---

### PATCH `/bands/{bandId}/activate`

Activa una pulsera. Desactiva automáticamente la que estaba activa.

**Request:** Sin body.

**Response 200:**
```json
{
  "bandId": "a1b2c3d4-0000-0000-0000-000000000004",
  "bandLogicalId": "band-sim-001",
  "isActive": true,
  "previouslyActiveBandId": null
}
```

**Errores:**
- `404 BAND_NOT_FOUND` — Pulsera no encontrada o no pertenece al usuario.

---

### DELETE `/bands/{bandId}`

Desvincula y elimina una pulsera del usuario.

**Response 204:** Sin body.

**Errores:**
- `404 BAND_NOT_FOUND`
- `409 BAND_IS_ACTIVE` — No se puede eliminar la pulsera activa. Activa otra primero o desactívala.

---

## 7. Habitación — `/rooms`

---

### GET `/rooms/primary`

Obtiene la habitación principal del usuario con su hub y dispositivos.

**Response 200:**
```json
{
  "roomId": "a1b2c3d4-0000-0000-0000-000000000002",
  "name": "Habitación principal",
  "hub": {
    "hubId": "a1b2c3d4-0000-0000-0000-000000000003",
    "hubLogicalId": "hub-001",
    "status": "ACTIVE",
    "operationalState": "ACTIVE",
    "firmwareVersion": "0.1.0",
    "lastSeenAt": "2026-05-14T12:29:55Z"
  },
  "devices": [
    {
      "deviceId": "a1b2c3d4-0000-0000-0000-000000000010",
      "deviceKey": "led-rgb-001",
      "name": "LED RGB",
      "type": "LIGHT",
      "enabled": true,
      "capabilities": ["ON_OFF", "BRIGHTNESS", "COLOR"],
      "currentState": { "on": true, "brightness": 80, "color": "#FFFFFF" }
    },
    {
      "deviceId": "a1b2c3d4-0000-0000-0000-000000000011",
      "deviceKey": "fan-001",
      "name": "Ventilador",
      "type": "FAN",
      "enabled": true,
      "capabilities": ["ON_OFF", "SPEED"],
      "currentState": { "on": false }
    }
  ]
}
```

**Errores:**
- `404 ROOM_NOT_FOUND` — El usuario no tiene habitación configurada.

---

## 8. Hubs — `/hubs`

---

### POST `/hubs/register`

Registra un nuevo hub y genera su token de dispositivo para autenticación MQTT. La app llama a este endpoint antes de enviar la configuración por BLE al hub.

**Request:**
```json
{
  "hubLogicalId": "hub-001",
  "roomId": "a1b2c3d4-0000-0000-0000-000000000002"
}
```

**Response 201:**
```json
{
  "hubId": "a1b2c3d4-0000-0000-0000-000000000003",
  "hubLogicalId": "hub-001",
  "deviceToken": "sl-hub-tok-c8f2a1b4e9d3...",
  "mqttHost": "cluster.s1.eu.hivemq.cloud",
  "mqttPort": 8883,
  "mqttTls": true,
  "tokenExpiresAt": "2026-05-14T13:30:00Z"
}
```

> El `deviceToken` es el único momento en que se devuelve en texto claro. El backend guarda solo su hash. La app lo envía al hub por BLE inmediatamente.

**Errores:**
- `404 ROOM_NOT_FOUND` — La habitación no existe o no pertenece al usuario.
- `409 HUB_ALREADY_REGISTERED` — La habitación ya tiene un hub registrado.

---

### GET `/hubs/{hubId}/status`

Obtiene el estado actual del hub.

**Response 200:**
```json
{
  "hubId": "a1b2c3d4-0000-0000-0000-000000000003",
  "hubLogicalId": "hub-001",
  "status": "ACTIVE",
  "operationalState": "ACTIVE",
  "firmwareVersion": "0.1.0",
  "lastSeenAt": "2026-05-14T12:29:55Z",
  "ipAddress": "192.168.1.105"
}
```

**Errores:**
- `403 FORBIDDEN` — El hub no pertenece al usuario autenticado.
- `404 HUB_NOT_FOUND`

---

### POST `/hubs/{hubId}/operational-state`

Cambia el estado operativo del hub desde la app (pausa, manual, salida).

**Request:**
```json
{
  "state": "PAUSED"
}
```

Estados permitidos desde la app: `ACTIVE`, `PAUSED`, `MANUAL`, `EXIT_MODE`.

**Response 200:**
```json
{
  "hubId": "a1b2c3d4-0000-0000-0000-000000000003",
  "previousState": "ACTIVE",
  "newState": "PAUSED",
  "changedAt": "2026-05-14T12:35:00Z"
}
```

**Errores:**
- `422 INVALID_STATE` — Estado no permitido.
- `403 FORBIDDEN`

---

### POST `/hubs/{hubId}/commands`

Envía un comando manual desde la app directamente al hub vía MQTT.

**Request:**
```json
{
  "actions": [
    {
      "deviceKey": "led-rgb-001",
      "action": "SET_BRIGHTNESS",
      "value": 50
    },
    {
      "deviceKey": "fan-001",
      "action": "TURN_OFF",
      "value": false
    }
  ]
}
```

**Response 200:**
```json
{
  "commandId": "c1b2c3d4-0000-0000-0000-000000000099",
  "status": "SENT",
  "sentAt": "2026-05-14T12:35:00Z"
}
```

**Errores:**
- `404 DEVICE_NOT_FOUND` — Un `deviceKey` no existe para ese hub.
- `422 INVALID_ACTION` — Acción no permitida para el tipo de dispositivo.
- `409 HUB_OFFLINE` — El hub está offline; el comando no puede enviarse.

---

## 9. Estado fisiológico — `/stress`

---

### GET `/stress/current`

Obtiene el estado fisiológico actual estimado del usuario.

**Response 200:**
```json
{
  "userId": "a1b2c3d4-0000-0000-0000-000000000001",
  "detectedState": "MODERATE_STRESS",
  "confidence": 0.76,
  "bpmCurrent": 98.5,
  "gsrCurrent": 720.0,
  "bpmBaseline": 70.0,
  "gsrBaseline": 500.0,
  "reason": {
    "bpm": "above_baseline",
    "gsr": "above_baseline",
    "movement": "low",
    "duration": "sustained"
  },
  "bandStatus": "CONNECTED",
  "bandBattery": 85,
  "detectedAt": "2026-05-14T12:35:00Z"
}
```

Si no hay datos recientes:
```json
{
  "userId": "a1b2c3d4-0000-0000-0000-000000000001",
  "detectedState": "NO_DATA",
  "confidence": 0.0,
  "bandStatus": "DISCONNECTED",
  "bandBattery": null,
  "detectedAt": null
}
```

---

### GET `/stress/history`

Obtiene el historial de estados fisiológicos. Devuelve estados agrupados por día usando `aggregated_daily_stats`.

**Query params:**

| Parámetro | Tipo | Default | Descripción |
|---|---|---|---|
| `days` | integer | 7 | Número de días hacia atrás. Máx. 90. |
| `page` | integer | 1 | Página de resultados. |
| `pageSize` | integer | 30 | Tamaño de página. Máx. 90. |

**Response 200:**
```json
{
  "userId": "a1b2c3d4-0000-0000-0000-000000000001",
  "period": {
    "from": "2026-05-07",
    "to": "2026-05-14"
  },
  "summary": {
    "avgHighStressMinutesPerDay": 42,
    "avgRecoverySeconds": 380,
    "totalProfileActivations": 14
  },
  "days": [
    {
      "date": "2026-05-14",
      "avgBpm": 74.2,
      "avgGsr": 520.0,
      "highStressMinutes": 35,
      "moderateStressMinutes": 60,
      "normalMinutes": 280,
      "relaxedMinutes": 25,
      "noDataMinutes": 40,
      "profileActivations": 3,
      "recoveryAvgSeconds": 320
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 30,
    "total": 7
  }
}
```

---

### GET `/stress/recent-events`

Obtiene los últimos N eventos de cambio de estado fisiológico (no agrupados).

**Query params:**

| Parámetro | Tipo | Default | Descripción |
|---|---|---|---|
| `limit` | integer | 20 | Número de eventos. Máx. 100. |

**Response 200:**
```json
{
  "events": [
    {
      "stateId": "...",
      "state": "HIGH_STRESS",
      "confidence": 0.82,
      "profileApplied": "Calma profunda",
      "detectedAt": "2026-05-14T12:35:00Z",
      "resolvedAt": "2026-05-14T12:52:00Z",
      "durationMinutes": 17
    }
  ]
}
```

---

## 10. Perfiles ambientales — `/profiles`

---

### GET `/profiles`

Lista los perfiles ambientales del usuario.

**Response 200:**
```json
{
  "profiles": [
    {
      "profileId": "...",
      "name": "Calma profunda",
      "targetState": "HIGH_STRESS",
      "isActive": true,
      "useAutomaticFallback": true,
      "actionsCount": 4,
      "createdAt": "2026-05-14T10:00:00Z"
    }
  ]
}
```

---

### GET `/profiles/{profileId}`

Obtiene un perfil con todas sus acciones.

**Response 200:**
```json
{
  "profileId": "...",
  "name": "Calma profunda",
  "targetState": "HIGH_STRESS",
  "isActive": true,
  "useAutomaticFallback": true,
  "actions": [
    {
      "actionId": "...",
      "deviceKey": "led-rgb-001",
      "deviceName": "LED RGB",
      "action": "SET_BRIGHTNESS",
      "value": 25,
      "orderIndex": 0
    },
    {
      "actionId": "...",
      "deviceKey": "led-rgb-001",
      "deviceName": "LED RGB",
      "action": "SET_COLOR_RGB",
      "value": { "r": 80, "g": 100, "b": 200 },
      "orderIndex": 1
    },
    {
      "actionId": "...",
      "deviceKey": "fan-001",
      "deviceName": "Ventilador",
      "action": "SET_SPEED",
      "value": "LOW",
      "orderIndex": 2
    },
    {
      "actionId": "...",
      "deviceKey": "display-001",
      "deviceName": "LCD 16x2",
      "action": "SHOW_MESSAGE",
      "value": "Respira profundo",
      "orderIndex": 3
    }
  ],
  "createdAt": "2026-05-14T10:00:00Z",
  "updatedAt": "2026-05-14T10:00:00Z"
}
```

---

### POST `/profiles`

Crea un nuevo perfil ambiental.

**Request:**
```json
{
  "name": "Calma profunda",
  "targetState": "HIGH_STRESS",
  "isActive": true,
  "useAutomaticFallback": true,
  "actions": [
    {
      "deviceKey": "led-rgb-001",
      "action": "SET_BRIGHTNESS",
      "value": 25,
      "orderIndex": 0
    },
    {
      "deviceKey": "fan-001",
      "action": "SET_SPEED",
      "value": "LOW",
      "orderIndex": 1
    }
  ]
}
```

**Response 201:** Igual a `GET /profiles/{profileId}`.

**Errores:**
- `404 DEVICE_NOT_FOUND` — Un `deviceKey` no existe para el hub del usuario.
- `422 INVALID_TARGET_STATE` — Estado no permitido para perfiles.
- `422 INVALID_ACTION_FOR_DEVICE` — La acción no es compatible con el tipo de dispositivo.

---

### PUT `/profiles/{profileId}`

Reemplaza completamente un perfil (acciones incluidas).

**Request:** Igual a `POST /profiles`.  
**Response 200:** Igual a `GET /profiles/{profileId}`.

---

### PATCH `/profiles/{profileId}`

Actualiza campos del perfil sin reemplazar las acciones.

**Request:**
```json
{
  "name": "Relajación suave",
  "isActive": false
}
```

**Response 200:** Igual a `GET /profiles/{profileId}`.

---

### DELETE `/profiles/{profileId}`

Elimina un perfil y todas sus acciones.

**Response 204:** Sin body.

---

## 11. Privacidad y datos — `/privacy`

---

### GET `/privacy/data-summary`

Resumen de los datos almacenados del usuario.

**Response 200:**
```json
{
  "biometricEventsCount": 8640,
  "biometricEventsOldestDate": "2026-04-14",
  "detectedStatesCount": 312,
  "aggregatedDaysCount": 30,
  "systemEventsCount": 45,
  "retentionPolicy": {
    "biometricEvents": "30 días",
    "detectedStates": "90 días",
    "aggregatedStats": "Mientras la cuenta esté activa",
    "calibrationData": "Mientras la cuenta esté activa"
  }
}
```

---

### DELETE `/privacy/biometric-history`

Elimina todo el historial biométrico del usuario (biometric_events, detected_states, aggregated_daily_stats), manteniendo la cuenta y configuración activa.

**Request:**
```json
{
  "confirmPassword": "contraseña_segura_123"
}
```

**Response 200:**
```json
{
  "deleted": {
    "biometricEvents": 8640,
    "detectedStates": 312,
    "aggregatedStats": 30
  },
  "deletedAt": "2026-05-14T12:40:00Z"
}
```

---

### GET `/privacy/export`

Genera y devuelve todos los datos del usuario en formato JSON para portabilidad.

**Response 200:**
```json
{
  "exportedAt": "2026-05-14T12:40:00Z",
  "user": { ... },
  "bands": [ ... ],
  "rooms": [ ... ],
  "hubs": [ ... ],
  "devices": [ ... ],
  "profiles": [ ... ],
  "biometricEvents": [ ... ],
  "detectedStates": [ ... ],
  "aggregatedStats": [ ... ]
}
```

> Para implementaciones con muchos datos, este endpoint puede devolver un enlace de descarga en lugar del JSON inline.

---

## 12. Resumen de endpoints

| Método | Endpoint | Auth | Descripción |
|---|---|---|---|
| POST | `/auth/register` | ❌ | Registrar usuario. |
| POST | `/auth/login` | ❌ | Iniciar sesión. |
| POST | `/auth/refresh` | ✅ | Renovar JWT. |
| GET | `/users/me` | ✅ | Perfil del usuario. |
| PATCH | `/users/me` | ✅ | Actualizar perfil. |
| DELETE | `/users/me` | ✅ | Eliminar cuenta. |
| GET | `/users/me/calibration` | ✅ | Estado de calibración. |
| POST | `/users/me/calibration/start` | ✅ | Iniciar calibración. |
| POST | `/users/me/calibration/complete` | ✅ | Guardar línea base. |
| GET | `/bands` | ✅ | Listar pulseras. |
| POST | `/bands` | ✅ | Registrar pulsera. |
| PATCH | `/bands/{bandId}/activate` | ✅ | Activar pulsera. |
| DELETE | `/bands/{bandId}` | ✅ | Desvincular pulsera. |
| GET | `/rooms/primary` | ✅ | Habitación principal con dispositivos. |
| POST | `/hubs/register` | ✅ | Registrar hub y generar token. |
| GET | `/hubs/{hubId}/status` | ✅ | Estado del hub. |
| POST | `/hubs/{hubId}/operational-state` | ✅ | Cambiar estado operativo. |
| POST | `/hubs/{hubId}/commands` | ✅ | Enviar comando manual. |
| GET | `/stress/current` | ✅ | Estado fisiológico actual. |
| GET | `/stress/history` | ✅ | Historial agrupado por día. |
| GET | `/stress/recent-events` | ✅ | Últimos eventos de estado. |
| GET | `/profiles` | ✅ | Listar perfiles. |
| GET | `/profiles/{profileId}` | ✅ | Detalle de perfil. |
| POST | `/profiles` | ✅ | Crear perfil. |
| PUT | `/profiles/{profileId}` | ✅ | Reemplazar perfil. |
| PATCH | `/profiles/{profileId}` | ✅ | Actualizar perfil. |
| DELETE | `/profiles/{profileId}` | ✅ | Eliminar perfil. |
| GET | `/privacy/data-summary` | ✅ | Resumen de datos almacenados. |
| DELETE | `/privacy/biometric-history` | ✅ | Eliminar historial biométrico. |
| GET | `/privacy/export` | ✅ | Exportar todos los datos. |

---

## 13. Notas de implementación para Ktor

- Usar `ContentNegotiation` con `kotlinx.serialization` o `Jackson` para serialización JSON.
- Usar `Authentication` plugin de Ktor con `jwt` para validar tokens en cada ruta protegida.
- Estructurar rutas con `routing { authenticate("jwt") { ... } }` para agrupar los endpoints protegidos.
- Los errores deben devolverse con el formato estándar de la sección 2.5. Usar `StatusPages` plugin para manejo centralizado de excepciones.
- Los IDs en las rutas (ej. `{bandId}`, `{hubId}`) son UUIDs; validar formato antes de hacer la consulta a BD para devolver 400 en lugar de 500 si el formato es inválido.
- Para `/hubs/{hubId}/commands`, el backend publica en MQTT de forma asíncrona y devuelve `202 Accepted` si se prefiere no bloquear la respuesta HTTP hasta el ACK del hub.

---

*Fin del documento — Especificación API REST STRESS-LESS v1.0*
