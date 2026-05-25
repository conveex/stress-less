-- ============================================================
-- STRESS-LESS MVP — DDL CORREGIDO v1.0.1
-- Base de datos: PostgreSQL 14+
-- Esquema: public
--
-- Objetivo:
--   Crear el esquema completo del MVP de STRESS-LESS.
--
-- Correcciones aplicadas respecto al DDL documental v1.0:
--   1. Se crea environment_profiles antes de detected_states,
--      porque detected_states.profile_applied referencia environment_profiles(id).
--   2. Se agregan CREATE TABLE IF NOT EXISTS e índices IF NOT EXISTS para facilitar re-ejecución.
--   3. Los ENUM se crean de forma segura con bloques DO para evitar error si ya existen.
--   4. Se agregan CHECK constraints útiles para rangos y estados aplicables.
--   5. Se agrega trigger genérico para updated_at.
--   6. Se agrega índice único parcial para un perfil activo por usuario y estado.
-- ============================================================

BEGIN;

-- ============================================================
-- EXTENSIONES
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ENUMS
-- ============================================================

DO $$ BEGIN
    CREATE TYPE disconnection_policy_enum AS ENUM (
        'KEEP_LAST',
        'RETURN_NORMAL',
        'EXIT_MODE',
        'DO_NOTHING'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE band_status_enum AS ENUM (
        'REGISTERED',
        'CONNECTED',
        'DISCONNECTED',
        'ERROR'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE hub_status_enum AS ENUM (
        'PENDING',
        'ACTIVE',
        'OFFLINE',
        'ERROR'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE operational_state_enum AS ENUM (
        'ACTIVE',
        'PAUSED',
        'MANUAL',
        'NO_DATA_MODE',
        'EXIT_MODE',
        'ERROR',
        'LOCAL_ONLY'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
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
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
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
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE biometric_source_enum AS ENUM (
        'REAL',
        'SIMULATED'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE physiological_state_enum AS ENUM (
        'HIGH_STRESS',
        'MODERATE_STRESS',
        'NORMAL',
        'MODERATE_RELAXED',
        'RELAXED',
        'NO_DATA',
        'LOW_CONFIDENCE'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
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
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE command_source_enum AS ENUM (
        'AUTOMATION',
        'MANUAL_APP',
        'SAFETY_PROFILE',
        'EXIT_MODE'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE command_status_enum AS ENUM (
        'SENT',
        'ACKNOWLEDGED',
        'FAILED',
        'TIMEOUT'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
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
        'FIRMWARE_UPDATED',
        'COMMAND_ACK',
        'COMMAND_FAILED',
        'WIFI_DISCONNECTED',
        'WIFI_RECONNECTED',
        'DEVICE_ERROR',
        'STARTUP',
        'EXIT_MODE_ACTIVATED'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE event_severity_enum AS ENUM (
        'INFO',
        'WARN',
        'ERROR'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- ============================================================
-- FUNCIÓN updated_at
-- ============================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- TABLAS BASE
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id                      UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(100)                NOT NULL,
    email                   VARCHAR(255)                NOT NULL UNIQUE,
    password_hash           VARCHAR(255)                NOT NULL,
    baseline_bpm            DECIMAL(5,1)                NOT NULL DEFAULT 70.0 CHECK (baseline_bpm BETWEEN 30 AND 250),
    baseline_gsr            DECIMAL(8,2)                NOT NULL DEFAULT 500.0 CHECK (baseline_gsr BETWEEN 0 AND 4095),
    baseline_movement       DECIMAL(4,3)                NOT NULL DEFAULT 0.100 CHECK (baseline_movement BETWEEN 0 AND 1),
    baseline_updated_at     TIMESTAMPTZ                 NULL,
    is_calibrated           BOOLEAN                     NOT NULL DEFAULT false,
    disconnection_policy    disconnection_policy_enum   NOT NULL DEFAULT 'KEEP_LAST',
    data_retention_consent  BOOLEAN                     NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ                 NULL
);

CREATE TABLE IF NOT EXISTS rooms (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100)    NOT NULL DEFAULT 'Mi habitación',
    is_primary  BOOLEAN         NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS rooms_one_primary_per_user
    ON rooms (user_id)
    WHERE is_primary = true;

CREATE TABLE IF NOT EXISTS bands (
    id              UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID                NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    band_id         VARCHAR(100)        NOT NULL UNIQUE,
    serial_number   VARCHAR(100)        NULL,
    is_active       BOOLEAN             NOT NULL DEFAULT false,
    status          band_status_enum    NOT NULL DEFAULT 'REGISTERED',
    battery_level   INTEGER             NULL CHECK (battery_level BETWEEN 0 AND 100),
    last_seen_at    TIMESTAMPTZ         NULL,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS bands_one_active_per_user
    ON bands (user_id)
    WHERE is_active = true;

CREATE TABLE IF NOT EXISTS hubs (
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

CREATE TABLE IF NOT EXISTS devices (
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

CREATE TABLE IF NOT EXISTS device_capabilities (
    id          UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id   UUID                NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    capability  capability_enum     NOT NULL,
    min_value   DECIMAL(10,2)       NULL,
    max_value   DECIMAL(10,2)       NULL,
    created_at  TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    UNIQUE (device_id, capability),
    CHECK (
        min_value IS NULL
        OR max_value IS NULL
        OR min_value <= max_value
    )
);

-- IMPORTANTE:
-- environment_profiles se crea antes de detected_states porque detected_states.profile_applied
-- referencia environment_profiles(id).

CREATE TABLE IF NOT EXISTS environment_profiles (
    id                      UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID                        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                    VARCHAR(100)                NOT NULL,
    target_state            physiological_state_enum    NOT NULL,
    is_active               BOOLEAN                     NOT NULL DEFAULT true,
    use_automatic_fallback  BOOLEAN                     NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    CHECK (target_state IN ('HIGH_STRESS', 'MODERATE_STRESS', 'NORMAL', 'MODERATE_RELAXED', 'RELAXED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS environment_profiles_one_active_per_state
    ON environment_profiles (user_id, target_state)
    WHERE is_active = true;

-- ============================================================
-- TABLAS OPERATIVAS
-- ============================================================

CREATE TABLE IF NOT EXISTS biometric_events (
    id          UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    band_id     UUID                    NOT NULL REFERENCES bands(id) ON DELETE RESTRICT,
    hub_id      UUID                    NOT NULL REFERENCES hubs(id) ON DELETE RESTRICT,
    user_id     UUID                    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bpm         DECIMAL(5,1)            NOT NULL CHECK (bpm BETWEEN 30 AND 250),
    gsr         DECIMAL(10,2)           NOT NULL CHECK (gsr BETWEEN 0 AND 4095),
    movement    DECIMAL(5,3)            NOT NULL CHECK (movement BETWEEN 0 AND 1),
    battery     INTEGER                 NULL CHECK (battery BETWEEN 0 AND 100),
    source      biometric_source_enum   NOT NULL DEFAULT 'REAL',
    timestamp   TIMESTAMPTZ             NOT NULL,
    received_at TIMESTAMPTZ             NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_biometric_user_time ON biometric_events (user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_biometric_band_time ON biometric_events (band_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_biometric_received  ON biometric_events (received_at);

CREATE TABLE IF NOT EXISTS detected_states (
    id                      UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID                        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hub_id                  UUID                        NOT NULL REFERENCES hubs(id) ON DELETE RESTRICT,
    state                   physiological_state_enum    NOT NULL,
    confidence              DECIMAL(4,3)                NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    bpm_delta               DECIMAL(5,1)                NULL,
    gsr_delta               DECIMAL(10,2)               NULL,
    movement_at_detection   DECIMAL(5,3)                NULL CHECK (movement_at_detection IS NULL OR movement_at_detection BETWEEN 0 AND 1),
    reason                  JSONB                       NULL,
    profile_applied         UUID                        NULL REFERENCES environment_profiles(id) ON DELETE SET NULL,
    detected_at             TIMESTAMPTZ                 NOT NULL DEFAULT NOW(),
    resolved_at             TIMESTAMPTZ                 NULL,
    CHECK (resolved_at IS NULL OR resolved_at >= detected_at)
);

CREATE INDEX IF NOT EXISTS idx_detected_user_time ON detected_states (user_id, detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_detected_state     ON detected_states (state, detected_at);

CREATE TABLE IF NOT EXISTS profile_actions (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id  UUID            NOT NULL REFERENCES environment_profiles(id) ON DELETE CASCADE,
    device_id   UUID            NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    action      action_enum     NOT NULL,
    value       JSONB           NULL,
    order_index INTEGER         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_profile_actions_profile_order
    ON profile_actions (profile_id, order_index);

CREATE TABLE IF NOT EXISTS commands (
    id                  UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    hub_id              UUID                    NOT NULL REFERENCES hubs(id) ON DELETE RESTRICT,
    room_id             UUID                    NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT,
    user_id             UUID                    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source              command_source_enum     NOT NULL,
    triggered_by_state  UUID                    NULL REFERENCES detected_states(id) ON DELETE SET NULL,
    payload             JSONB                   NOT NULL,
    status              command_status_enum     NOT NULL DEFAULT 'SENT',
    sent_at             TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    acknowledged_at     TIMESTAMPTZ             NULL,
    CHECK (acknowledged_at IS NULL OR acknowledged_at >= sent_at)
);

CREATE INDEX IF NOT EXISTS idx_commands_hub_time ON commands (hub_id, sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_commands_sent     ON commands (sent_at);

CREATE TABLE IF NOT EXISTS system_events (
    id          UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    hub_id      UUID                    NULL REFERENCES hubs(id) ON DELETE SET NULL,
    user_id     UUID                    NULL REFERENCES users(id) ON DELETE SET NULL,
    event_type  event_type_enum         NOT NULL,
    severity    event_severity_enum     NOT NULL DEFAULT 'INFO',
    description TEXT                    NULL,
    metadata    JSONB                   NULL,
    occurred_at TIMESTAMPTZ             NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_system_events_time ON system_events (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_system_events_hub  ON system_events (hub_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_system_events_user ON system_events (user_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS aggregated_daily_stats (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date                    DATE            NOT NULL,
    avg_bpm                 DECIMAL(5,1)    NULL,
    avg_gsr                 DECIMAL(8,2)    NULL,
    high_stress_minutes     INTEGER         NOT NULL DEFAULT 0 CHECK (high_stress_minutes >= 0),
    moderate_stress_minutes INTEGER         NOT NULL DEFAULT 0 CHECK (moderate_stress_minutes >= 0),
    normal_minutes          INTEGER         NOT NULL DEFAULT 0 CHECK (normal_minutes >= 0),
    relaxed_minutes         INTEGER         NOT NULL DEFAULT 0 CHECK (relaxed_minutes >= 0),
    no_data_minutes         INTEGER         NOT NULL DEFAULT 0 CHECK (no_data_minutes >= 0),
    profile_activations     INTEGER         NOT NULL DEFAULT 0 CHECK (profile_activations >= 0),
    recovery_avg_seconds    INTEGER         NULL CHECK (recovery_avg_seconds IS NULL OR recovery_avg_seconds >= 0),
    total_readings          INTEGER         NOT NULL DEFAULT 0 CHECK (total_readings >= 0),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, date)
);

CREATE INDEX IF NOT EXISTS idx_daily_stats_user_date
    ON aggregated_daily_stats (user_id, date DESC);

-- ============================================================
-- TRIGGERS updated_at
-- ============================================================

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_hubs_updated_at ON hubs;
CREATE TRIGGER trg_hubs_updated_at
BEFORE UPDATE ON hubs
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_devices_updated_at ON devices;
CREATE TRIGGER trg_devices_updated_at
BEFORE UPDATE ON devices
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_environment_profiles_updated_at ON environment_profiles;
CREATE TRIGGER trg_environment_profiles_updated_at
BEFORE UPDATE ON environment_profiles
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

COMMIT;
