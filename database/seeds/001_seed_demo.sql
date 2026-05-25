-- ============================================================
-- STRESS-LESS MVP — SEED DEMO v1.0.1
-- Base de datos: PostgreSQL 14+
--
-- Objetivo:
--   Insertar datos mínimos para probar el prototipo:
--   usuario demo, habitación, hub, pulsera simulada,
--   dispositivos, capacidades, perfiles ambientales y acciones.
--
-- Requisito:
--   Ejecutar primero database/ddl/001_init_schema.sql
-- ============================================================

BEGIN;

-- ============================================================
-- USUARIO DEMO
-- ============================================================

INSERT INTO users (
    id,
    name,
    email,
    password_hash,
    baseline_bpm,
    baseline_gsr,
    baseline_movement,
    is_calibrated,
    data_retention_consent
)
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
)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- HABITACIÓN PRINCIPAL
-- ============================================================

INSERT INTO rooms (
    id,
    user_id,
    name,
    is_primary
)
VALUES (
    'a1b2c3d4-0000-0000-0000-000000000002',
    'a1b2c3d4-0000-0000-0000-000000000001',
    'Habitación principal',
    true
)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- HUB DEMO
-- ============================================================

INSERT INTO hubs (
    id,
    room_id,
    hub_id,
    status,
    operational_state,
    firmware_version
)
VALUES (
    'a1b2c3d4-0000-0000-0000-000000000003',
    'a1b2c3d4-0000-0000-0000-000000000002',
    'hub-001',
    'ACTIVE',
    'ACTIVE',
    '0.1.0'
)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- PULSERA SIMULADA
-- ============================================================

INSERT INTO bands (
    id,
    user_id,
    band_id,
    is_active,
    status,
    battery_level
)
VALUES (
    'a1b2c3d4-0000-0000-0000-000000000004',
    'a1b2c3d4-0000-0000-0000-000000000001',
    'band-sim-001',
    true,
    'CONNECTED',
    85
)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- DISPOSITIVOS DEL PROTOTIPO
-- ============================================================

INSERT INTO devices (
    id,
    hub_id,
    device_key,
    name,
    type,
    enabled,
    current_state
)
VALUES
    (
        'a1b2c3d4-0000-0000-0000-000000000010',
        'a1b2c3d4-0000-0000-0000-000000000003',
        'led-rgb-001',
        'LED RGB',
        'LIGHT',
        true,
        '{"on": false, "brightness": 0, "color": "#000000"}'::jsonb
    ),
    (
        'a1b2c3d4-0000-0000-0000-000000000011',
        'a1b2c3d4-0000-0000-0000-000000000003',
        'fan-001',
        'Ventilador',
        'FAN',
        true,
        '{"on": false, "speed": "OFF"}'::jsonb
    ),
    (
        'a1b2c3d4-0000-0000-0000-000000000012',
        'a1b2c3d4-0000-0000-0000-000000000003',
        'display-001',
        'LCD 16x2',
        'DISPLAY',
        true,
        '{"on": true, "message": "Stress-Less"}'::jsonb
    ),
    (
        'a1b2c3d4-0000-0000-0000-000000000013',
        'a1b2c3d4-0000-0000-0000-000000000003',
        'buzzer-001',
        'Buzzer audio',
        'AUDIO',
        true,
        '{"on": false, "volume": 0}'::jsonb
    )
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- CAPACIDADES
-- ============================================================

INSERT INTO device_capabilities (
    device_id,
    capability,
    min_value,
    max_value
)
VALUES
    ('a1b2c3d4-0000-0000-0000-000000000010', 'ON_OFF',     NULL, NULL),
    ('a1b2c3d4-0000-0000-0000-000000000010', 'BRIGHTNESS', 0,    100),
    ('a1b2c3d4-0000-0000-0000-000000000010', 'COLOR',      NULL, NULL),

    ('a1b2c3d4-0000-0000-0000-000000000011', 'ON_OFF',     NULL, NULL),
    ('a1b2c3d4-0000-0000-0000-000000000011', 'SPEED',      0,    100),

    ('a1b2c3d4-0000-0000-0000-000000000012', 'ON_OFF',     NULL, NULL),
    ('a1b2c3d4-0000-0000-0000-000000000012', 'MESSAGE',    NULL, NULL),

    ('a1b2c3d4-0000-0000-0000-000000000013', 'ON_OFF',     NULL, NULL),
    ('a1b2c3d4-0000-0000-0000-000000000013', 'VOLUME',     0,    100)
ON CONFLICT (device_id, capability) DO NOTHING;

-- ============================================================
-- PERFIL AMBIENTAL DEMO: ESTRÉS ALTO
-- ============================================================

INSERT INTO environment_profiles (
    id,
    user_id,
    name,
    target_state,
    is_active,
    use_automatic_fallback
)
VALUES (
    'a1b2c3d4-0000-0000-0000-000000000020',
    'a1b2c3d4-0000-0000-0000-000000000001',
    'Calma profunda',
    'HIGH_STRESS',
    true,
    true
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO profile_actions (
    id,
    profile_id,
    device_id,
    action,
    value,
    order_index
)
VALUES
    (
        'a1b2c3d4-0000-0000-0000-000000000021',
        'a1b2c3d4-0000-0000-0000-000000000020',
        'a1b2c3d4-0000-0000-0000-000000000010',
        'SET_BRIGHTNESS',
        '25'::jsonb,
        0
    ),
    (
        'a1b2c3d4-0000-0000-0000-000000000022',
        'a1b2c3d4-0000-0000-0000-000000000020',
        'a1b2c3d4-0000-0000-0000-000000000010',
        'SET_COLOR_RGB',
        '{"r": 80, "g": 100, "b": 200}'::jsonb,
        1
    ),
    (
        'a1b2c3d4-0000-0000-0000-000000000023',
        'a1b2c3d4-0000-0000-0000-000000000020',
        'a1b2c3d4-0000-0000-0000-000000000011',
        'SET_SPEED',
        '"LOW"'::jsonb,
        2
    ),
    (
        'a1b2c3d4-0000-0000-0000-000000000024',
        'a1b2c3d4-0000-0000-0000-000000000020',
        'a1b2c3d4-0000-0000-0000-000000000012',
        'SHOW_MESSAGE',
        '"Respira profundo"'::jsonb,
        3
    ),
    (
        'a1b2c3d4-0000-0000-0000-000000000025',
        'a1b2c3d4-0000-0000-0000-000000000020',
        'a1b2c3d4-0000-0000-0000-000000000013',
        'TURN_OFF',
        'false'::jsonb,
        4
    )
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- EVENTO DE SISTEMA INICIAL
-- ============================================================

INSERT INTO system_events (
    id,
    hub_id,
    user_id,
    event_type,
    severity,
    description,
    metadata
)
VALUES (
    'a1b2c3d4-0000-0000-0000-000000000030',
    'a1b2c3d4-0000-0000-0000-000000000003',
    'a1b2c3d4-0000-0000-0000-000000000001',
    'STARTUP',
    'INFO',
    'Datos demo iniciales cargados correctamente',
    '{"source": "seed", "version": "1.0.1"}'::jsonb
)
ON CONFLICT (id) DO NOTHING;

COMMIT;
