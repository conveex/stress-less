-- ============================================================
-- STRESS-LESS MVP — VALIDACIÓN DE BD v1.0.1
-- Ejecutar después de:
--   1. database/ddl/001_init_schema.sql
--   2. database/seeds/001_seed_demo.sql
-- ============================================================

-- 1. Validar conteo de tablas principales
SELECT
    'users' AS table_name,
    COUNT(*) AS total
FROM users
UNION ALL
SELECT 'rooms', COUNT(*) FROM rooms
UNION ALL
SELECT 'hubs', COUNT(*) FROM hubs
UNION ALL
SELECT 'bands', COUNT(*) FROM bands
UNION ALL
SELECT 'devices', COUNT(*) FROM devices
UNION ALL
SELECT 'device_capabilities', COUNT(*) FROM device_capabilities
UNION ALL
SELECT 'environment_profiles', COUNT(*) FROM environment_profiles
UNION ALL
SELECT 'profile_actions', COUNT(*) FROM profile_actions
UNION ALL
SELECT 'system_events', COUNT(*) FROM system_events
ORDER BY table_name;

-- 2. Validar relación usuario → room → hub → devices
SELECT
    u.name AS user_name,
    u.email,
    r.name AS room_name,
    h.hub_id,
    h.status AS hub_status,
    h.operational_state,
    COUNT(d.id) AS devices_count
FROM users u
JOIN rooms r ON r.user_id = u.id
JOIN hubs h ON h.room_id = r.id
LEFT JOIN devices d ON d.hub_id = h.id
WHERE u.email = 'demo@stressless.local'
GROUP BY u.name, u.email, r.name, h.hub_id, h.status, h.operational_state;

-- 3. Validar pulsera activa demo
SELECT
    u.email,
    b.band_id,
    b.is_active,
    b.status,
    b.battery_level
FROM users u
JOIN bands b ON b.user_id = u.id
WHERE u.email = 'demo@stressless.local';

-- 4. Validar capacidades por dispositivo
SELECT
    d.device_key,
    d.name,
    d.type,
    ARRAY_AGG(dc.capability ORDER BY dc.capability) AS capabilities
FROM devices d
JOIN device_capabilities dc ON dc.device_id = d.id
GROUP BY d.device_key, d.name, d.type
ORDER BY d.device_key;

-- 5. Validar perfil ambiental y acciones
SELECT
    ep.name AS profile_name,
    ep.target_state,
    ep.is_active,
    d.device_key,
    pa.action,
    pa.value,
    pa.order_index
FROM environment_profiles ep
JOIN profile_actions pa ON pa.profile_id = ep.id
JOIN devices d ON d.id = pa.device_id
WHERE ep.user_id = 'a1b2c3d4-0000-0000-0000-000000000001'
ORDER BY ep.target_state, pa.order_index;

-- 6. Simular una lectura biométrica válida
INSERT INTO biometric_events (
    band_id,
    hub_id,
    user_id,
    bpm,
    gsr,
    movement,
    battery,
    source,
    timestamp
)
SELECT
    b.id,
    h.id,
    u.id,
    110.0,
    850.0,
    0.050,
    85,
    'SIMULATED',
    NOW()
FROM users u
JOIN bands b ON b.user_id = u.id AND b.band_id = 'band-sim-001'
JOIN rooms r ON r.user_id = u.id AND r.is_primary = true
JOIN hubs h ON h.room_id = r.id AND h.hub_id = 'hub-001'
WHERE u.email = 'demo@stressless.local';

-- 7. Validar que la lectura se insertó
SELECT
    be.id,
    b.band_id,
    h.hub_id,
    be.bpm,
    be.gsr,
    be.movement,
    be.source,
    be.timestamp,
    be.received_at
FROM biometric_events be
JOIN bands b ON b.id = be.band_id
JOIN hubs h ON h.id = be.hub_id
ORDER BY be.received_at DESC
LIMIT 5;

-- 8. Simular estado detectado para comprobar FK con environment_profiles
INSERT INTO detected_states (
    user_id,
    hub_id,
    state,
    confidence,
    bpm_delta,
    gsr_delta,
    movement_at_detection,
    reason,
    profile_applied
)
SELECT
    u.id,
    h.id,
    'HIGH_STRESS',
    0.820,
    40.0,
    350.0,
    0.050,
    '{"bpm": "above_baseline", "gsr": "above_baseline", "movement": "low", "duration": "sustained"}'::jsonb,
    ep.id
FROM users u
JOIN rooms r ON r.user_id = u.id AND r.is_primary = true
JOIN hubs h ON h.room_id = r.id
JOIN environment_profiles ep ON ep.user_id = u.id AND ep.target_state = 'HIGH_STRESS' AND ep.is_active = true
WHERE u.email = 'demo@stressless.local';

-- 9. Validar estado detectado
SELECT
    ds.state,
    ds.confidence,
    ds.reason,
    ep.name AS profile_applied,
    ds.detected_at
FROM detected_states ds
LEFT JOIN environment_profiles ep ON ep.id = ds.profile_applied
ORDER BY ds.detected_at DESC
LIMIT 5;
