# STRESS-LESS — Modelo Entidad-Relación y Base de Datos
**Versión:** 1.0  
**Fase:** 0 — Contratos  
**Documento:** 1 de 3  
**Depende de:** Documentación base v1.1  

---

## 1. Propósito

Este documento define el modelo de datos completo del MVP de STRESS-LESS. Incluye todas las entidades, sus campos con tipos precisos, restricciones, índices, relaciones y el DDL listo para ejecutar en PostgreSQL.

Este documento es el **contrato de datos** del proyecto. Backend, app y firmware deben respetar los nombres de campos y tipos definidos aquí. Cualquier cambio al modelo debe reflejarse en este documento antes de modificar el código.

---

## 2. Decisiones de diseño del modelo

| Decisión | Razón |
|---|---|
| IDs tipo `UUID` en todas las tablas | Evita colisiones al integrar datos de múltiples fuentes (hub, app, backend). |
| `created_at` en todas las tablas | Auditoría mínima sin overhead. |
| `updated_at` solo donde el registro muta | No todas las tablas necesitan tracking de actualización. |
| Datos biométricos en tabla separada | Volumen alto, política de retención diferente al resto. |
| Perfiles y acciones separadas | Un perfil puede tener N acciones sobre N dispositivos. |
| `device_capabilities` como tabla separada | Un dispositivo puede tener múltiples capacidades; evita columnas booleanas repetidas. |
| `detected_states` separado de `biometric_events` | El estado estimado es un dato derivado con ciclo de vida propio. |
| `aggregated_daily_stats` precalculada | El historial de la app consulta agregados, no eventos crudos. |
| `system_events` separado | Log operativo con propósito distinto a datos biométricos. |
| Campos `operational_state` en `hubs` | El hub reporta su propio estado operativo; el backend lo registra. |

---

## 3. Diagrama de relaciones (texto)

```
users
 ├── 1:1  rooms (is_primary = true, MVP)
 ├── 1:N  bands (is_active = true solo una)
 ├── 1:N  environment_profiles
 └── 1:N  aggregated_daily_stats

rooms
 └── 1:1  hubs

hubs
 ├── 1:N  devices
 └── 1:N  commands

devices
 └── 1:N  device_capabilities

bands
 └── 1:N  biometric_events

biometric_events
 └── (origen de) detected_states (via user_id + timestamp)

environment_profiles
 └── 1:N  profile_actions

profile_actions
 └── N:1  devices (referencia opcional)

system_events
 └── referencia a hub_id o user_id (nullable)
```

---

## 4. Entidades y campos

---

### 4.1 `users`

Usuarios del sistema. Un usuario representa una cuenta con su línea base fisiológica y preferencias de privacidad.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único. |
| `name` | VARCHAR(100) | NOT NULL | Nombre del usuario. |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | Email de autenticación. |
| `password_hash` | VARCHAR(255) | NOT NULL | Hash bcrypt de la contraseña. |
| `baseline_bpm` | DECIMAL(5,1) | NOT NULL, DEFAULT 70.0 | BPM de reposo calibrado o por defecto. |
| `baseline_gsr` | DECIMAL(8,2) | NOT NULL, DEFAULT 500.0 | GSR de reposo calibrado o por defecto. |
| `baseline_movement` | DECIMAL(4,3) | NOT NULL, DEFAULT 0.100 | Movimiento de reposo calibrado o por defecto. |
| `baseline_updated_at` | TIMESTAMPTZ | NULLABLE | Última vez que se actualizó la línea base. NULL = no calibrado. |
| `is_calibrated` | BOOLEAN | NOT NULL, DEFAULT false | Indica si el usuario completó la calibración inicial. |
| `disconnection_policy` | VARCHAR(30) | NOT NULL, DEFAULT 'KEEP_LAST' | Política ante desconexión de pulsera: `KEEP_LAST`, `RETURN_NORMAL`, `EXIT_MODE`, `DO_NOTHING`. |
| `data_retention_consent` | BOOLEAN | NOT NULL, DEFAULT false | Consentimiento explícito de retención de datos biométricos. |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Fecha de registro. |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Última actualización del perfil. |
| `deleted_at` | TIMESTAMPTZ | NULLABLE | Soft delete. NULL = cuenta activa. |

**Restricciones adicionales:**
- `email` debe estar en formato válido (validación a nivel aplicación).
- `disconnection_policy` acepta solo los valores del enum definido.
- Si `deleted_at` no es NULL, el usuario se considera eliminado; sus datos biométricos deben purgarse.

---

### 4.2 `bands`

Pulseras biométricas registradas. Un usuario puede tener varias, pero solo una activa.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único interno. |
| `user_id` | UUID | NOT NULL, FK → users.id | Usuario propietario. |
| `band_id` | VARCHAR(100) | NOT NULL, UNIQUE | Identificador lógico de la pulsera (`bandId` en MQTT). |
| `serial_number` | VARCHAR(100) | NULLABLE | Número de serie físico si aplica. |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT false | Solo una por usuario puede ser true. |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'REGISTERED' | Estado: `REGISTERED`, `CONNECTED`, `DISCONNECTED`, `ERROR`. |
| `battery_level` | INTEGER | NULLABLE, CHECK 0-100 | Último nivel de batería reportado. |
| `last_seen_at` | TIMESTAMPTZ | NULLABLE | Última vez que envió datos. |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Fecha de registro. |

**Restricciones adicionales:**
- Solo una `band` con `is_active = true` por `user_id` (constraint parcial único).
- `band_id` es el identificador que usa el firmware y los topics MQTT.

---

### 4.3 `rooms`

Habitaciones del usuario. En MVP, un usuario tiene exactamente una habitación principal.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único. |
| `user_id` | UUID | NOT NULL, FK → users.id | Usuario propietario. |
| `name` | VARCHAR(100) | NOT NULL, DEFAULT 'Mi habitación' | Nombre descriptivo. |
| `is_primary` | BOOLEAN | NOT NULL, DEFAULT true | En MVP siempre true. |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Fecha de creación. |

**Restricciones adicionales:**
- Solo una `room` con `is_primary = true` por `user_id` (constraint parcial único, MVP).

---

### 4.4 `hubs`

Hubs instalados en habitaciones. Una habitación tiene exactamente un hub.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único interno. |
| `room_id` | UUID | NOT NULL, UNIQUE, FK → rooms.id | Habitación a la que pertenece (UNIQUE = 1 hub por room). |
| `hub_id` | VARCHAR(100) | NOT NULL, UNIQUE | Identificador lógico del hub (usado en MQTT y credenciales). |
| `device_token_hash` | VARCHAR(255) | NULLABLE | Hash del token de autenticación MQTT. NULL = no configurado. |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | Estado: `PENDING`, `ACTIVE`, `OFFLINE`, `ERROR`. |
| `operational_state` | VARCHAR(20) | NOT NULL, DEFAULT 'NO_DATA_MODE' | Estado operativo: `ACTIVE`, `PAUSED`, `MANUAL`, `NO_DATA_MODE`, `EXIT_MODE`, `ERROR`, `LOCAL_ONLY`. |
| `firmware_version` | VARCHAR(30) | NULLABLE | Versión del firmware instalado. |
| `last_seen_at` | TIMESTAMPTZ | NULLABLE | Última vez que publicó en MQTT (status o heartbeat). |
| `ip_address` | VARCHAR(45) | NULLABLE | IP local reportada por el hub. |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Fecha de registro. |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Última actualización. |

---

### 4.5 `devices`

Dispositivos ambientales registrados bajo un hub.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único. |
| `hub_id` | UUID | NOT NULL, FK → hubs.id | Hub al que pertenece. |
| `device_key` | VARCHAR(100) | NOT NULL | Clave lógica del dispositivo dentro del hub (ej. `light-001`). UNIQUE por hub. |
| `name` | VARCHAR(100) | NOT NULL | Nombre descriptivo (ej. "Luz principal"). |
| `type` | VARCHAR(30) | NOT NULL | Tipo: `LIGHT`, `FAN`, `CLIMATE`, `AUDIO`, `DISPLAY`, `TV`, `CURTAIN`, `GENERIC_SWITCH`, `AROMA`. |
| `enabled` | BOOLEAN | NOT NULL, DEFAULT true | Si el dispositivo está habilitado para automatización. |
| `current_state` | JSONB | NULLABLE | Estado actual conocido del dispositivo (ej. `{"on": true, "brightness": 40}`). |
| `metadata` | JSONB | NULLABLE | Datos adicionales específicos del tipo de dispositivo. |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Fecha de registro. |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Última actualización. |

**Restricciones adicionales:**
- `(hub_id, device_key)` debe ser UNIQUE.

---

### 4.6 `device_capabilities`

Capacidades declaradas de cada dispositivo.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único. |
| `device_id` | UUID | NOT NULL, FK → devices.id | Dispositivo al que pertenece. |
| `capability` | VARCHAR(30) | NOT NULL | Capacidad: `ON_OFF`, `BRIGHTNESS`, `COLOR`, `TEMPERATURE`, `SPEED`, `VOLUME`, `MESSAGE`, `MODE`, `POSITION`. |
| `min_value` | DECIMAL(10,2) | NULLABLE | Valor mínimo si aplica (ej. 0 para brillo). |
| `max_value` | DECIMAL(10,2) | NULLABLE | Valor máximo si aplica (ej. 100 para brillo). |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Fecha de registro. |

**Restricciones adicionales:**
- `(device_id, capability)` debe ser UNIQUE.

---

### 4.7 `biometric_events`

Lecturas biométricas crudas. Tabla de alto volumen con retención de 30 días.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único. |
| `band_id` | UUID | NOT NULL, FK → bands.id | Pulsera que generó el dato. |
| `hub_id` | UUID | NOT NULL, FK → hubs.id | Hub que recibió o retransmitió el dato. |
| `user_id` | UUID | NOT NULL, FK → users.id | Usuario asociado (desnormalizado para consultas directas). |
| `bpm` | DECIMAL(5,1) | NOT NULL | Frecuencia cardiaca en BPM. |
| `gsr` | DECIMAL(10,2) | NOT NULL | Respuesta galvánica de la piel (valor bruto del ADC o procesado). |
| `movement` | DECIMAL(5,3) | NOT NULL | Nivel de movimiento estimado (0.000 a 1.000). |
| `battery` | INTEGER | NULLABLE, CHECK 0-100 | Nivel de batería de la pulsera en el momento de la lectura. |
| `source` | VARCHAR(20) | NOT NULL, DEFAULT 'REAL' | Origen: `REAL`, `SIMULATED`. |
| `timestamp` | TIMESTAMPTZ | NOT NULL | Timestamp generado en el dispositivo (puede ser aproximado). |
| `received_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Timestamp de recepción en el backend. |

**Índices recomendados:**
- `(user_id, timestamp DESC)` — consultas de historial por usuario.
- `(band_id, timestamp DESC)` — consultas por pulsera.
- `(received_at)` — limpieza periódica por retención.

**Política de retención:** 30 días. Los registros con `received_at < NOW() - INTERVAL '30 days'` pueden purgarse.

---

### 4.8 `detected_states`

Estados fisiológicos estimados por el backend. Retención de 90 días.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único. |
| `user_id` | UUID | NOT NULL, FK → users.id | Usuario. |
| `hub_id` | UUID | NOT NULL, FK → hubs.id | Hub activo cuando se detectó el estado. |
| `state` | VARCHAR(30) | NOT NULL | Estado: `HIGH_STRESS`, `MODERATE_STRESS`, `NORMAL`, `MODERATE_RELAXED`, `RELAXED`, `NO_DATA`, `LOW_CONFIDENCE`. |
| `confidence` | DECIMAL(4,3) | NOT NULL, CHECK 0-1 | Nivel de confianza de la estimación (0.000 a 1.000). |
| `bpm_delta` | DECIMAL(5,1) | NULLABLE | Diferencia entre BPM actual y línea base en el momento de detección. |
| `gsr_delta` | DECIMAL(10,2) | NULLABLE | Diferencia entre GSR actual y línea base. |
| `movement_at_detection` | DECIMAL(5,3) | NULLABLE | Movimiento registrado en el momento de la detección. |
| `reason` | JSONB | NULLABLE | Razón estructurada de la detección (ej. `{"bpm":"above_baseline","gsr":"above_baseline"}`). |
| `profile_applied` | UUID | NULLABLE, FK → environment_profiles.id | Perfil ambiental aplicado como consecuencia. NULL si no se aplicó ninguno. |
| `detected_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Momento de la detección. |
| `resolved_at` | TIMESTAMPTZ | NULLABLE | Momento en que el estado fue reemplazado por otro. |

**Índices recomendados:**
- `(user_id, detected_at DESC)` — historial de estados.
- `(state, detected_at)` — estadísticas por tipo de estado.

**Política de retención:** 90 días.

---

### 4.9 `environment_profiles`

Perfiles ambientales configurados por el usuario para cada estado fisiológico.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único. |
| `user_id` | UUID | NOT NULL, FK → users.id | Usuario propietario del perfil. |
| `name` | VARCHAR(100) | NOT NULL | Nombre descriptivo (ej. "Calma profunda"). |
| `target_state` | VARCHAR(30) | NOT NULL | Estado fisiológico al que responde: `HIGH_STRESS`, `MODERATE_STRESS`, `NORMAL`, `MODERATE_RELAXED`, `RELAXED`. |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT true | Si este perfil está habilitado para aplicarse automáticamente. |
| `use_automatic_fallback` | BOOLEAN | NOT NULL, DEFAULT true | Si se aplica el modo automático para dispositivos no incluidos en las acciones del perfil. |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Fecha de creación. |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Última modificación. |

**Restricciones adicionales:**
- Un usuario puede tener múltiples perfiles para el mismo `target_state`, pero solo uno con `is_active = true` por estado a la vez. (Validación a nivel aplicación en MVP; constraint en v2).

---

### 4.10 `profile_actions`

Acciones individuales dentro de un perfil ambiental.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único. |
| `profile_id` | UUID | NOT NULL, FK → environment_profiles.id | Perfil al que pertenece esta acción. |
| `device_id` | UUID | NOT NULL, FK → devices.id | Dispositivo sobre el que actúa. |
| `action` | VARCHAR(30) | NOT NULL | Acción a ejecutar: `TURN_ON`, `TURN_OFF`, `SET_BRIGHTNESS`, `SET_COLOR_RGB`, `SET_COLOR_HEX`, `SET_SPEED`, `SET_TEMPERATURE`, `SET_MODE`, `SET_VOLUME`, `SHOW_MESSAGE`, `SET_POSITION`. |
| `value` | JSONB | NULLABLE | Valor de la acción. El formato depende de `action` (ej. `35` para brillo, `{"r":80,"g":120,"b":255}` para color). |
| `order_index` | INTEGER | NOT NULL, DEFAULT 0 | Orden de ejecución dentro del perfil. |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Fecha de creación. |

---

### 4.11 `commands`

Comandos ambientales enviados por el backend al hub vía MQTT.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único. |
| `hub_id` | UUID | NOT NULL, FK → hubs.id | Hub destinatario. |
| `room_id` | UUID | NOT NULL, FK → rooms.id | Habitación asociada. |
| `user_id` | UUID | NOT NULL, FK → users.id | Usuario que origina el comando (directo o por automatización). |
| `source` | VARCHAR(20) | NOT NULL | Origen: `AUTOMATION`, `MANUAL_APP`, `SAFETY_PROFILE`, `EXIT_MODE`. |
| `triggered_by_state` | UUID | NULLABLE, FK → detected_states.id | Estado fisiológico que desencadenó el comando. NULL si fue manual. |
| `payload` | JSONB | NOT NULL | Payload completo del comando enviado al hub. |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'SENT' | Estado: `SENT`, `ACKNOWLEDGED`, `FAILED`, `TIMEOUT`. |
| `sent_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Momento de envío. |
| `acknowledged_at` | TIMESTAMPTZ | NULLABLE | Momento de confirmación por el hub. |

**Política de retención:** 30 días.

---

### 4.12 `system_events`

Log operativo del sistema: conexiones, errores, reconexiones, cambios de estado.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único. |
| `hub_id` | UUID | NULLABLE, FK → hubs.id | Hub relacionado (si aplica). |
| `user_id` | UUID | NULLABLE, FK → users.id | Usuario relacionado (si aplica). |
| `event_type` | VARCHAR(50) | NOT NULL | Tipo de evento: `HUB_CONNECTED`, `HUB_DISCONNECTED`, `HUB_ERROR`, `BAND_CONNECTED`, `BAND_DISCONNECTED`, `WIFI_LOST`, `MQTT_RECONNECTED`, `CALIBRATION_STARTED`, `CALIBRATION_COMPLETED`, `MODE_CHANGED`, `TOKEN_INVALID`, `FIRMWARE_UPDATED`. |
| `severity` | VARCHAR(10) | NOT NULL, DEFAULT 'INFO' | Severidad: `INFO`, `WARN`, `ERROR`. |
| `description` | TEXT | NULLABLE | Descripción legible del evento. |
| `metadata` | JSONB | NULLABLE | Datos adicionales del evento. |
| `occurred_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Momento del evento. |

**Política de retención:** 30 días.

---

### 4.13 `aggregated_daily_stats`

Estadísticas diarias precalculadas para el historial de la app. Se generan al final de cada día o bajo demanda.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Identificador único. |
| `user_id` | UUID | NOT NULL, FK → users.id | Usuario al que corresponden. |
| `date` | DATE | NOT NULL | Fecha del resumen (YYYY-MM-DD). |
| `avg_bpm` | DECIMAL(5,1) | NULLABLE | BPM promedio del día. |
| `avg_gsr` | DECIMAL(8,2) | NULLABLE | GSR promedio del día. |
| `high_stress_minutes` | INTEGER | NOT NULL, DEFAULT 0 | Minutos en estado HIGH_STRESS. |
| `moderate_stress_minutes` | INTEGER | NOT NULL, DEFAULT 0 | Minutos en estado MODERATE_STRESS. |
| `normal_minutes` | INTEGER | NOT NULL, DEFAULT 0 | Minutos en estado NORMAL. |
| `relaxed_minutes` | INTEGER | NOT NULL, DEFAULT 0 | Minutos en estados RELAXED o MODERATE_RELAXED. |
| `no_data_minutes` | INTEGER | NOT NULL, DEFAULT 0 | Minutos sin datos de pulsera. |
| `profile_activations` | INTEGER | NOT NULL, DEFAULT 0 | Cantidad de perfiles ambientales activados en el día. |
| `recovery_avg_seconds` | INTEGER | NULLABLE | Tiempo promedio de recuperación de estrés a normal (en segundos). NULL si no hubo episodios. |
| `total_readings` | INTEGER | NOT NULL, DEFAULT 0 | Total de lecturas biométricas recibidas en el día. |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Momento en que se calculó el agregado. |

**Restricciones adicionales:**
- `(user_id, date)` debe ser UNIQUE.

**Retención:** Mientras la cuenta esté activa. Se elimina con la cuenta del usuario.

---

## 5. DDL completo — PostgreSQL

```sql
-- ============================================================
-- STRESS-LESS MVP — DDL v1.0
-- Base de datos: PostgreSQL 14+
-- Esquema: public (default)
-- ============================================================

-- Extensión para UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ENUMS
-- ============================================================

CREATE TYPE disconnection_policy_enum AS ENUM (
    'KEEP_LAST',
    'RETURN_NORMAL', 
    'EXIT_MODE',
    'DO_NOTHING'
);

CREATE TYPE band_status_enum AS ENUM (
    'REGISTERED',
    'CONNECTED',
    'DISCONNECTED',
    'ERROR'
);

CREATE TYPE hub_status_enum AS ENUM (
    'PENDING',
    'ACTIVE',
    'OFFLINE',
    'ERROR'
);

CREATE TYPE operational_state_enum AS ENUM (
    'ACTIVE',
    'PAUSED',
    'MANUAL',
    'NO_DATA_MODE',
    'EXIT_MODE',
    'ERROR',
    'LOCAL_ONLY'
);

CREATE TYPE device_type_enum AS ENUM (
    'LIGHT',
    'FAN',
    'CLIMATE',
    'AUDIO',
    'DISPLAY',
    'TV',
    'CURTAIN',
    'GENERIC_SWITCH',
    'AROMA'
);

CREATE TYPE capability_enum AS ENUM (
    'ON_OFF',
    'BRIGHTNESS',
    'COLOR',
    'TEMPERATURE',
    'SPEED',
    'VOLUME',
    'MESSAGE',
    'MODE',
    'POSITION'
);

CREATE TYPE biometric_source_enum AS ENUM (
    'REAL',
    'SIMULATED'
);

CREATE TYPE physiological_state_enum AS ENUM (
    'HIGH_STRESS',
    'MODERATE_STRESS',
    'NORMAL',
    'MODERATE_RELAXED',
    'RELAXED',
    'NO_DATA',
    'LOW_CONFIDENCE'
);

CREATE TYPE action_enum AS ENUM (
    'TURN_ON',
    'TURN_OFF',
    'SET_BRIGHTNESS',
    'SET_COLOR_RGB',
    'SET_COLOR_HEX',
    'SET_SPEED',
    'SET_TEMPERATURE',
    'SET_MODE',
    'SET_VOLUME',
    'SHOW_MESSAGE',
    'SET_POSITION'
);

CREATE TYPE command_source_enum AS ENUM (
    'AUTOMATION',
    'MANUAL_APP',
    'SAFETY_PROFILE',
    'EXIT_MODE'
);

CREATE TYPE command_status_enum AS ENUM (
    'SENT',
    'ACKNOWLEDGED',
    'FAILED',
    'TIMEOUT'
);

CREATE TYPE event_type_enum AS ENUM (
    'HUB_CONNECTED',
    'HUB_DISCONNECTED',
    'HUB_ERROR',
    'BAND_CONNECTED',
    'BAND_DISCONNECTED',
    'WIFI_LOST',
    'MQTT_RECONNECTED',
    'CALIBRATION_STARTED',
    'CALIBRATION_COMPLETED',
    'MODE_CHANGED',
    'TOKEN_INVALID',
    'FIRMWARE_UPDATED'
);

CREATE TYPE event_severity_enum AS ENUM (
    'INFO',
    'WARN',
    'ERROR'
);

-- ============================================================
-- TABLAS
-- ============================================================

-- users
CREATE TABLE users (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(100)    NOT NULL,
    email                   VARCHAR(255)    NOT NULL UNIQUE,
    password_hash           VARCHAR(255)    NOT NULL,
    baseline_bpm            DECIMAL(5,1)    NOT NULL DEFAULT 70.0,
    baseline_gsr            DECIMAL(8,2)    NOT NULL DEFAULT 500.0,
    baseline_movement       DECIMAL(4,3)    NOT NULL DEFAULT 0.100,
    baseline_updated_at     TIMESTAMPTZ     NULL,
    is_calibrated           BOOLEAN         NOT NULL DEFAULT false,
    disconnection_policy    disconnection_policy_enum NOT NULL DEFAULT 'KEEP_LAST',
    data_retention_consent  BOOLEAN         NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ     NULL
);

-- rooms
CREATE TABLE rooms (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100)    NOT NULL DEFAULT 'Mi habitación',
    is_primary  BOOLEAN         NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Restricción MVP: una habitación principal por usuario
CREATE UNIQUE INDEX rooms_one_primary_per_user 
    ON rooms (user_id) 
    WHERE is_primary = true;

-- bands
CREATE TABLE bands (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    band_id         VARCHAR(100)    NOT NULL UNIQUE,
    serial_number   VARCHAR(100)    NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT false,
    status          band_status_enum NOT NULL DEFAULT 'REGISTERED',
    battery_level   INTEGER         NULL CHECK (battery_level BETWEEN 0 AND 100),
    last_seen_at    TIMESTAMPTZ     NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Restricción: solo una band activa por usuario
CREATE UNIQUE INDEX bands_one_active_per_user 
    ON bands (user_id) 
    WHERE is_active = true;

-- hubs
CREATE TABLE hubs (
    id                  UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id             UUID                    NOT NULL UNIQUE REFERENCES rooms(id) ON DELETE RESTRICT,
    hub_id              VARCHAR(100)            NOT NULL UNIQUE,
    device_token_hash   VARCHAR(255)            NULL,
    status              hub_status_enum         NOT NULL DEFAULT 'PENDING',
    operational_state   operational_state_enum  NOT NULL DEFAULT 'NO_DATA_MODE',
    firmware_version    VARCHAR(30)             NULL,
    last_seen_at        TIMESTAMPTZ             NULL,
    ip_address          VARCHAR(45)             NULL,
    created_at          TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ             NOT NULL DEFAULT NOW()
);

-- devices
CREATE TABLE devices (
    id              UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    hub_id          UUID                NOT NULL REFERENCES hubs(id) ON DELETE CASCADE,
    device_key      VARCHAR(100)        NOT NULL,
    name            VARCHAR(100)        NOT NULL,
    type            device_type_enum    NOT NULL,
    enabled         BOOLEAN             NOT NULL DEFAULT true,
    current_state   JSONB               NULL,
    metadata        JSONB               NULL,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    UNIQUE (hub_id, device_key)
);

-- device_capabilities
CREATE TABLE device_capabilities (
    id          UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id   UUID                NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    capability  capability_enum     NOT NULL,
    min_value   DECIMAL(10,2)       NULL,
    max_value   DECIMAL(10,2)       NULL,
    created_at  TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    UNIQUE (device_id, capability)
);

-- biometric_events
CREATE TABLE biometric_events (
    id          UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    band_id     UUID                    NOT NULL REFERENCES bands(id) ON DELETE RESTRICT,
    hub_id      UUID                    NOT NULL REFERENCES hubs(id) ON DELETE RESTRICT,
    user_id     UUID                    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bpm         DECIMAL(5,1)            NOT NULL,
    gsr         DECIMAL(10,2)           NOT NULL,
    movement    DECIMAL(5,3)            NOT NULL,
    battery     INTEGER                 NULL CHECK (battery BETWEEN 0 AND 100),
    source      biometric_source_enum   NOT NULL DEFAULT 'REAL',
    timestamp   TIMESTAMPTZ             NOT NULL,
    received_at TIMESTAMPTZ             NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_biometric_user_time    ON biometric_events (user_id, timestamp DESC);
CREATE INDEX idx_biometric_band_time    ON biometric_events (band_id, timestamp DESC);
CREATE INDEX idx_biometric_received     ON biometric_events (received_at);

-- detected_states
CREATE TABLE detected_states (
    id                      UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID                        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hub_id                  UUID                        NOT NULL REFERENCES hubs(id) ON DELETE RESTRICT,
    state                   physiological_state_enum    NOT NULL,
    confidence              DECIMAL(4,3)                NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    bpm_delta               DECIMAL(5,1)                NULL,
    gsr_delta               DECIMAL(10,2)               NULL,
    movement_at_detection   DECIMAL(5,3)                NULL,
    reason                  JSONB                       NULL,
    profile_applied         UUID                        NULL REFERENCES environment_profiles(id) ON DELETE SET NULL,
    detected_at             TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    resolved_at             TIMESTAMPTZ                 NULL
);

CREATE INDEX idx_detected_user_time ON detected_states (user_id, detected_at DESC);
CREATE INDEX idx_detected_state     ON detected_states (state, detected_at);

-- environment_profiles
CREATE TABLE environment_profiles (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                    VARCHAR(100) NOT NULL,
    target_state            physiological_state_enum NOT NULL,
    is_active               BOOLEAN     NOT NULL DEFAULT true,
    use_automatic_fallback  BOOLEAN     NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- profile_actions
CREATE TABLE profile_actions (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id  UUID            NOT NULL REFERENCES environment_profiles(id) ON DELETE CASCADE,
    device_id   UUID            NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    action      action_enum     NOT NULL,
    value       JSONB           NULL,
    order_index INTEGER         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- commands
CREATE TABLE commands (
    id                  UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    hub_id              UUID                    NOT NULL REFERENCES hubs(id) ON DELETE RESTRICT,
    room_id             UUID                    NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT,
    user_id             UUID                    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source              command_source_enum     NOT NULL,
    triggered_by_state  UUID                    NULL REFERENCES detected_states(id) ON DELETE SET NULL,
    payload             JSONB                   NOT NULL,
    status              command_status_enum     NOT NULL DEFAULT 'SENT',
    sent_at             TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    acknowledged_at     TIMESTAMPTZ             NULL
);

CREATE INDEX idx_commands_hub_time  ON commands (hub_id, sent_at DESC);
CREATE INDEX idx_commands_sent      ON commands (sent_at);

-- system_events
CREATE TABLE system_events (
    id          UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    hub_id      UUID                    NULL REFERENCES hubs(id) ON DELETE SET NULL,
    user_id     UUID                    NULL REFERENCES users(id) ON DELETE SET NULL,
    event_type  event_type_enum         NOT NULL,
    severity    event_severity_enum     NOT NULL DEFAULT 'INFO',
    description TEXT                    NULL,
    metadata    JSONB                   NULL,
    occurred_at TIMESTAMPTZ             NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_system_events_time     ON system_events (occurred_at DESC);
CREATE INDEX idx_system_events_hub      ON system_events (hub_id, occurred_at DESC);

-- aggregated_daily_stats
CREATE TABLE aggregated_daily_stats (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date                    DATE        NOT NULL,
    avg_bpm                 DECIMAL(5,1) NULL,
    avg_gsr                 DECIMAL(8,2) NULL,
    high_stress_minutes     INTEGER     NOT NULL DEFAULT 0,
    moderate_stress_minutes INTEGER     NOT NULL DEFAULT 0,
    normal_minutes          INTEGER     NOT NULL DEFAULT 0,
    relaxed_minutes         INTEGER     NOT NULL DEFAULT 0,
    no_data_minutes         INTEGER     NOT NULL DEFAULT 0,
    profile_activations     INTEGER     NOT NULL DEFAULT 0,
    recovery_avg_seconds    INTEGER     NULL,
    total_readings          INTEGER     NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, date)
);

CREATE INDEX idx_daily_stats_user_date ON aggregated_daily_stats (user_id, date DESC);
```

---

## 6. Datos de prueba para el prototipo

```sql
-- Usuario de prueba
INSERT INTO users (id, name, email, password_hash, baseline_bpm, baseline_gsr, baseline_movement, is_calibrated, data_retention_consent)
VALUES (
    'a1b2c3d4-0000-0000-0000-000000000001',
    'Usuario Demo',
    'demo@stressless.local',
    '$2b$12$placeholder_hash_for_dev_only',
    70.0,
    500.0,
    0.100,
    false,
    true
);

-- Habitación principal
INSERT INTO rooms (id, user_id, name, is_primary)
VALUES (
    'a1b2c3d4-0000-0000-0000-000000000002',
    'a1b2c3d4-0000-0000-0000-000000000001',
    'Habitación principal',
    true
);

-- Hub de prueba
INSERT INTO hubs (id, room_id, hub_id, status, operational_state, firmware_version)
VALUES (
    'a1b2c3d4-0000-0000-0000-000000000003',
    'a1b2c3d4-0000-0000-0000-000000000002',
    'hub-001',
    'ACTIVE',
    'ACTIVE',
    '0.1.0'
);

-- Pulsera simulada activa
INSERT INTO bands (id, user_id, band_id, is_active, status)
VALUES (
    'a1b2c3d4-0000-0000-0000-000000000004',
    'a1b2c3d4-0000-0000-0000-000000000001',
    'band-sim-001',
    true,
    'CONNECTED'
);

-- Dispositivos del prototipo
INSERT INTO devices (id, hub_id, device_key, name, type, enabled)
VALUES
    ('a1b2c3d4-0000-0000-0000-000000000010', 'a1b2c3d4-0000-0000-0000-000000000003', 'led-rgb-001',  'LED RGB',        'LIGHT',   true),
    ('a1b2c3d4-0000-0000-0000-000000000011', 'a1b2c3d4-0000-0000-0000-000000000003', 'fan-001',      'Ventilador',     'FAN',     true),
    ('a1b2c3d4-0000-0000-0000-000000000012', 'a1b2c3d4-0000-0000-0000-000000000003', 'display-001',  'LCD 16x2',       'DISPLAY', true),
    ('a1b2c3d4-0000-0000-0000-000000000013', 'a1b2c3d4-0000-0000-0000-000000000003', 'buzzer-001',   'Buzzer audio',   'AUDIO',   true);

-- Capacidades de los dispositivos de prueba
INSERT INTO device_capabilities (device_id, capability, min_value, max_value)
VALUES
    ('a1b2c3d4-0000-0000-0000-000000000010', 'ON_OFF',     NULL, NULL),
    ('a1b2c3d4-0000-0000-0000-000000000010', 'BRIGHTNESS', 0,    100),
    ('a1b2c3d4-0000-0000-0000-000000000010', 'COLOR',      NULL, NULL),
    ('a1b2c3d4-0000-0000-0000-000000000011', 'ON_OFF',     NULL, NULL),
    ('a1b2c3d4-0000-0000-0000-000000000011', 'SPEED',      0,    100),
    ('a1b2c3d4-0000-0000-0000-000000000012', 'ON_OFF',     NULL, NULL),
    ('a1b2c3d4-0000-0000-0000-000000000012', 'MESSAGE',    NULL, NULL),
    ('a1b2c3d4-0000-0000-0000-000000000013', 'ON_OFF',     NULL, NULL),
    ('a1b2c3d4-0000-0000-0000-000000000013', 'VOLUME',     0,    100);
```

---

## 7. Política de retención — resumen

| Tabla | Retención | Cómo se purga |
|---|---|---|
| `biometric_events` | 30 días | Job diario: `DELETE WHERE received_at < NOW() - INTERVAL '30 days'` |
| `detected_states` | 90 días | Job diario: `DELETE WHERE detected_at < NOW() - INTERVAL '90 days'` |
| `commands` | 30 días | Job diario: `DELETE WHERE sent_at < NOW() - INTERVAL '30 days'` |
| `system_events` | 30 días | Job diario: `DELETE WHERE occurred_at < NOW() - INTERVAL '30 days'` |
| `aggregated_daily_stats` | Mientras la cuenta esté activa | CASCADE al eliminar usuario |
| `users` (soft delete) | El usuario elimina su cuenta | SET `deleted_at`, luego purga diferida |
| `users` (datos de calibración) | Mientras la cuenta esté activa | Se eliminan en cascada con la cuenta |

---

## 8. Notas de implementación para el backend (Ktor + Exposed)

- Usar `ExposedDateTime` para todos los campos `TIMESTAMPTZ`.
- Usar `EntityID<UUID>` con `UUIDTable` de Exposed para todas las tablas.
- Los enums de PostgreSQL se mapean como `customEnumeration` en Exposed o como `VARCHAR` con validación en servicio.
- Los campos `JSONB` se manejan como `String` serializado o con un mapper de Jackson/kotlinx.serialization.
- Los índices parciales (`WHERE is_primary = true`, `WHERE is_active = true`) se crean con SQL raw fuera de Exposed DSL.
- Para el prototipo se puede usar `DATABASE_URL` como variable de entorno compatible con Render/Railway.

---

*Fin del documento — Modelo ER STRESS-LESS v1.0*
