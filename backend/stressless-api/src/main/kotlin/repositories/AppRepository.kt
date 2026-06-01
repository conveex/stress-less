package com.stressless.repositories

import com.stressless.config.DemoConfig
import com.stressless.db.DatabaseFactory
import com.stressless.dto.app.*
import com.stressless.mqtt.MqttClientService
import com.hivemq.client.mqtt.datatypes.MqttQos
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

object AppRepository {

    private val json = Json {
        explicitNulls = false
        prettyPrint = true
        encodeDefaults = true
    }

    fun getHome(): AppHomeResponse {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val userSql = """
                SELECT id, name, email, is_calibrated, baseline_bpm, baseline_gsr
                FROM users
                WHERE id = ?
                LIMIT 1
            """.trimIndent()

            val user = connection.prepareStatement(userSql).use { st ->
                st.setObject(1, DemoConfig.DEMO_USER_ID)
                st.executeQuery().use { rs ->
                    if (!rs.next()) error("Demo user not found")

                    AppUserSummary(
                        userId = rs.getString("id"),
                        name = rs.getString("name"),
                        email = rs.getString("email"),
                        isCalibrated = rs.getBoolean("is_calibrated")
                    ) to Pair(
                        rs.getDouble("baseline_bpm"),
                        rs.getDouble("baseline_gsr")
                    )
                }
            }

            val userSummary = user.first
            val baselines = user.second

            val stress = getLatestStress(connection, baselines.first, baselines.second)
            val band = getActiveBand(connection)
            val room = getPrimaryRoom(connection)
            val hub = getPrimaryHub(connection)
            val activeProfile = getLatestActiveProfile(connection, stress.detectedState)
            val lastCommand = getLastCommand(connection)

            return AppHomeResponse(
                user = userSummary,
                stress = stress,
                band = band,
                hub = hub,
                room = room,
                activeProfile = activeProfile,
                lastCommand = lastCommand
            )
        }
    }

    fun getRoomPrimary(): RoomPrimaryResponse {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val roomSql = """
                SELECT r.id AS room_id, r.name,
                       h.id AS hub_uuid, h.hub_id, h.status::text, h.operational_state::text,
                       h.firmware_version, h.last_seen_at, h.ip_address
                FROM rooms r
                LEFT JOIN hubs h ON h.room_id = r.id
                WHERE r.user_id = ?
                  AND r.is_primary = true
                LIMIT 1
            """.trimIndent()

            connection.prepareStatement(roomSql).use { st ->
                st.setObject(1, DemoConfig.DEMO_USER_ID)
                st.executeQuery().use { rs ->
                    if (!rs.next()) error("Primary room not found")

                    val hubUuid = rs.getString("hub_uuid")

                    val hub = if (hubUuid != null) {
                        RoomHubResponse(
                            hubId = hubUuid,
                            hubLogicalId = rs.getString("hub_id"),
                            status = rs.getString("status"),
                            operationalState = rs.getString("operational_state"),
                            firmwareVersion = rs.getString("firmware_version"),
                            lastSeenAt = rs.getTimestamp("last_seen_at")?.toInstant()?.toString(),
                            ipAddress = rs.getString("ip_address")
                        )
                    } else {
                        null
                    }

                    val devices = if (hubUuid != null) {
                        getDevicesByHubUuid(connection, UUID.fromString(hubUuid))
                    } else {
                        emptyList()
                    }

                    return RoomPrimaryResponse(
                        roomId = rs.getString("room_id"),
                        name = rs.getString("name"),
                        hub = hub,
                        devices = devices
                    )
                }
            }
        }
    }

    fun getBands(): BandsResponse {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                SELECT id, band_id, serial_number, is_active, status::text, battery_level, last_seen_at, created_at
                FROM bands
                WHERE user_id = ?
                ORDER BY created_at DESC
            """.trimIndent()

            connection.prepareStatement(sql).use { st ->
                st.setObject(1, DemoConfig.DEMO_USER_ID)
                st.executeQuery().use { rs ->
                    val bands = mutableListOf<BandResponse>()

                    while (rs.next()) {
                        bands.add(
                            BandResponse(
                                bandId = rs.getString("id"),
                                bandLogicalId = rs.getString("band_id"),
                                serialNumber = rs.getString("serial_number"),
                                isActive = rs.getBoolean("is_active"),
                                status = rs.getString("status"),
                                batteryLevel = rs.getObject("battery_level") as Int?,
                                lastSeenAt = rs.getTimestamp("last_seen_at")?.toInstant()?.toString(),
                                createdAt = rs.getTimestamp("created_at").toInstant().toString()
                            )
                        )
                    }

                    return BandsResponse(bands)
                }
            }
        }
    }

    fun getProfiles(): ProfilesResponse {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                SELECT p.id, p.name, p.target_state::text, p.is_active, p.use_automatic_fallback,
                       p.created_at, COUNT(a.id) AS actions_count
                FROM environment_profiles p
                LEFT JOIN profile_actions a ON a.profile_id = p.id
                WHERE p.user_id = ?
                GROUP BY p.id
                ORDER BY p.target_state, p.name
            """.trimIndent()

            connection.prepareStatement(sql).use { st ->
                st.setObject(1, DemoConfig.DEMO_USER_ID)
                st.executeQuery().use { rs ->
                    val profiles = mutableListOf<ProfileResponse>()

                    while (rs.next()) {
                        profiles.add(
                            ProfileResponse(
                                profileId = rs.getString("id"),
                                name = rs.getString("name"),
                                targetState = rs.getString("target_state"),
                                isActive = rs.getBoolean("is_active"),
                                useAutomaticFallback = rs.getBoolean("use_automatic_fallback"),
                                actionsCount = rs.getInt("actions_count"),
                                createdAt = rs.getTimestamp("created_at").toInstant().toString()
                            )
                        )
                    }

                    return ProfilesResponse(profiles)
                }
            }
        }
    }

    fun getRecentEvents(limit: Int = 20): StressRecentEventsResponse {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                SELECT ds.id, ds.state::text, ds.confidence, ds.detected_at, ds.resolved_at,
                       ep.name AS profile_name
                FROM detected_states ds
                LEFT JOIN environment_profiles ep ON ep.id = ds.profile_applied
                WHERE ds.user_id = ?
                ORDER BY ds.detected_at DESC
                LIMIT ?
            """.trimIndent()

            connection.prepareStatement(sql).use { st ->
                st.setObject(1, DemoConfig.DEMO_USER_ID)
                st.setInt(2, limit)

                st.executeQuery().use { rs ->
                    val events = mutableListOf<StressRecentEventResponse>()

                    while (rs.next()) {
                        val detectedAt = rs.getTimestamp("detected_at").toInstant()
                        val resolvedAt = rs.getTimestamp("resolved_at")?.toInstant()

                        val durationMinutes = if (resolvedAt != null) {
                            java.time.Duration.between(detectedAt, resolvedAt).toMinutes()
                        } else {
                            null
                        }

                        events.add(
                            StressRecentEventResponse(
                                stateId = rs.getString("id"),
                                state = rs.getString("state"),
                                confidence = rs.getDouble("confidence"),
                                profileApplied = rs.getString("profile_name"),
                                detectedAt = detectedAt.toString(),
                                resolvedAt = resolvedAt?.toString(),
                                durationMinutes = durationMinutes
                            )
                        )
                    }

                    return StressRecentEventsResponse(events)
                }
            }
        }
    }

    fun changeOperationalState(
        hubLogicalId: String,
        newState: String
    ): ChangeOperationalStateResponse {
        val allowed = setOf("ACTIVE", "PAUSED", "MANUAL", "EXIT_MODE")

        require(newState in allowed) {
            "Invalid operational state: $newState"
        }

        DatabaseFactory.getDataSource().connection.use { connection ->
            val selectSql = """
                SELECT h.id, h.operational_state::text
                FROM hubs h
                JOIN rooms r ON r.id = h.room_id
                WHERE h.hub_id = ?
                  AND r.user_id = ?
                LIMIT 1
            """.trimIndent()

            val hubUuid: UUID
            val previousState: String

            connection.prepareStatement(selectSql).use { st ->
                st.setString(1, hubLogicalId)
                st.setObject(2, DemoConfig.DEMO_USER_ID)

                st.executeQuery().use { rs ->
                    if (!rs.next()) error("Hub not found")

                    hubUuid = UUID.fromString(rs.getString("id"))
                    previousState = rs.getString("operational_state")
                }
            }

            val updateSql = """
                UPDATE hubs
                SET operational_state = ?::operational_state_enum,
                    updated_at = NOW()
                WHERE id = ?
            """.trimIndent()

            connection.prepareStatement(updateSql).use { st ->
                st.setString(1, newState)
                st.setObject(2, hubUuid)
                st.executeUpdate()
            }

            return ChangeOperationalStateResponse(
                hubId = hubUuid.toString(),
                previousState = previousState,
                newState = newState,
                changedAt = Instant.now().toString()
            )
        }
    }

    fun sendManualCommand(
        hubLogicalId: String,
        request: ManualHubCommandRequest
    ): ManualHubCommandResponse {
        require(request.actions.isNotEmpty()) {
            "At least one action is required"
        }

        DatabaseFactory.getDataSource().connection.use { connection ->
            val contextSql = """
                SELECT h.id AS hub_uuid, r.id AS room_uuid
                FROM hubs h
                JOIN rooms r ON r.id = h.room_id
                WHERE h.hub_id = ?
                  AND r.user_id = ?
                LIMIT 1
            """.trimIndent()

            val hubUuid: UUID
            val roomUuid: UUID

            connection.prepareStatement(contextSql).use { st ->
                st.setString(1, hubLogicalId)
                st.setObject(2, DemoConfig.DEMO_USER_ID)

                st.executeQuery().use { rs ->
                    if (!rs.next()) error("Hub not found")

                    hubUuid = UUID.fromString(rs.getString("hub_uuid"))
                    roomUuid = UUID.fromString(rs.getString("room_uuid"))
                }
            }

            val commandId = UUID.randomUUID().toString()

            val actionsJson = json.encodeToString(request.actions)

            val payloadJson = """
                {
                  "commandId": "$commandId",
                  "hubId": "$hubLogicalId",
                  "source": "MANUAL_APP",
                  "actions": $actionsJson,
                  "timestamp": "${Instant.now()}"
                }
            """.trimIndent()

            val insertSql = """
                INSERT INTO commands (
                    hub_id,
                    room_id,
                    user_id,
                    source,
                    triggered_by_state,
                    payload,
                    status,
                    sent_at
                )
                VALUES (?, ?, ?, 'MANUAL_APP', NULL, ?::jsonb, 'SENT', NOW())
            """.trimIndent()

            connection.prepareStatement(insertSql).use { st ->
                st.setObject(1, hubUuid)
                st.setObject(2, roomUuid)
                st.setObject(3, DemoConfig.DEMO_USER_ID)
                st.setString(4, payloadJson)
                st.executeUpdate()
            }

            MqttClientService.publishJson(
                topic = "stressless/hub/$hubLogicalId/commands",
                payload = payloadJson,
                qos = MqttQos.AT_LEAST_ONCE,
                retain = false
            )

            return ManualHubCommandResponse(
                commandId = commandId,
                status = "SENT",
                sentAt = Instant.now().toString()
            )
        }
    }

    private fun getLatestStress(
        connection: java.sql.Connection,
        baselineBpm: Double,
        baselineGsr: Double
    ): AppStressSummary {
        val sql = """
            SELECT state::text, confidence, bpm_delta, gsr_delta, movement_at_detection,
                   reason::text, detected_at
            FROM detected_states
            WHERE user_id = ?
            ORDER BY detected_at DESC
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { st ->
            st.setObject(1, DemoConfig.DEMO_USER_ID)

            st.executeQuery().use { rs ->
                if (!rs.next()) {
                    return AppStressSummary(
                        detectedState = "NO_DATA",
                        confidence = 0.0,
                        bpmCurrent = null,
                        gsrCurrent = null,
                        bpmBaseline = baselineBpm,
                        gsrBaseline = baselineGsr,
                        movementAtDetection = null,
                        reason = null,
                        detectedAt = null
                    )
                }

                val bpmDelta = rs.getDouble("bpm_delta")
                val gsrDelta = rs.getDouble("gsr_delta")

                return AppStressSummary(
                    detectedState = rs.getString("state"),
                    confidence = rs.getDouble("confidence"),
                    bpmCurrent = baselineBpm + bpmDelta,
                    gsrCurrent = baselineGsr + gsrDelta,
                    bpmBaseline = baselineBpm,
                    gsrBaseline = baselineGsr,
                    movementAtDetection = rs.getDouble("movement_at_detection"),
                    reason = rs.getString("reason"),
                    detectedAt = rs.getTimestamp("detected_at")?.toInstant()?.toString()
                )
            }
        }
    }

    private fun getActiveBand(connection: java.sql.Connection): AppBandSummary? {
        val sql = """
            SELECT id, band_id, status::text, is_active, battery_level, last_seen_at
            FROM bands
            WHERE user_id = ?
              AND is_active = true
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { st ->
            st.setObject(1, DemoConfig.DEMO_USER_ID)

            st.executeQuery().use { rs ->
                if (!rs.next()) return null

                return AppBandSummary(
                    bandId = rs.getString("id"),
                    bandLogicalId = rs.getString("band_id"),
                    status = rs.getString("status"),
                    isActive = rs.getBoolean("is_active"),
                    batteryLevel = rs.getObject("battery_level") as Int?,
                    lastSeenAt = rs.getTimestamp("last_seen_at")?.toInstant()?.toString()
                )
            }
        }
    }

    private fun getPrimaryRoom(connection: java.sql.Connection): AppRoomSummary? {
        val sql = """
            SELECT id, name
            FROM rooms
            WHERE user_id = ?
              AND is_primary = true
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { st ->
            st.setObject(1, DemoConfig.DEMO_USER_ID)

            st.executeQuery().use { rs ->
                if (!rs.next()) return null

                return AppRoomSummary(
                    roomId = rs.getString("id"),
                    name = rs.getString("name")
                )
            }
        }
    }

    private fun getPrimaryHub(connection: java.sql.Connection): AppHubSummary? {
        val sql = """
            SELECT h.id, h.hub_id, h.status::text, h.operational_state::text,
                   h.firmware_version, h.last_seen_at, h.ip_address
            FROM hubs h
            JOIN rooms r ON r.id = h.room_id
            WHERE r.user_id = ?
              AND r.is_primary = true
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { st ->
            st.setObject(1, DemoConfig.DEMO_USER_ID)

            st.executeQuery().use { rs ->
                if (!rs.next()) return null

                return AppHubSummary(
                    hubId = rs.getString("id"),
                    hubLogicalId = rs.getString("hub_id"),
                    status = rs.getString("status"),
                    operationalState = rs.getString("operational_state"),
                    firmwareVersion = rs.getString("firmware_version"),
                    lastSeenAt = rs.getTimestamp("last_seen_at")?.toInstant()?.toString(),
                    ipAddress = rs.getString("ip_address")
                )
            }
        }
    }

    private fun getLatestActiveProfile(
        connection: java.sql.Connection,
        state: String
    ): AppProfileSummary? {
        val sql = """
            SELECT id, name, target_state::text
            FROM environment_profiles
            WHERE user_id = ?
              AND target_state = ?::physiological_state_enum
              AND is_active = true
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { st ->
            st.setObject(1, DemoConfig.DEMO_USER_ID)
            st.setString(2, state)

            st.executeQuery().use { rs ->
                if (!rs.next()) return null

                return AppProfileSummary(
                    profileId = rs.getString("id"),
                    name = rs.getString("name"),
                    targetState = rs.getString("target_state")
                )
            }
        }
    }

    private fun getLastCommand(connection: java.sql.Connection): AppCommandSummary? {
        val sql = """
            SELECT payload ->> 'commandId' AS command_id,
                   source::text,
                   status::text,
                   sent_at,
                   acknowledged_at
            FROM commands
            WHERE user_id = ?
            ORDER BY sent_at DESC
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { st ->
            st.setObject(1, DemoConfig.DEMO_USER_ID)

            st.executeQuery().use { rs ->
                if (!rs.next()) return null

                return AppCommandSummary(
                    commandId = rs.getString("command_id"),
                    source = rs.getString("source"),
                    status = rs.getString("status"),
                    sentAt = rs.getTimestamp("sent_at").toInstant().toString(),
                    acknowledgedAt = rs.getTimestamp("acknowledged_at")?.toInstant()?.toString()
                )
            }
        }
    }

    private fun getDevicesByHubUuid(
        connection: java.sql.Connection,
        hubUuid: UUID
    ): List<RoomDeviceResponse> {
        val sql = """
            SELECT d.id, d.device_key, d.name, d.type::text, d.enabled,
                   d.current_state::text,
                   COALESCE(
                       ARRAY_AGG(dc.capability::text ORDER BY dc.capability::text)
                       FILTER (WHERE dc.capability IS NOT NULL),
                       ARRAY[]::text[]
                   ) AS capabilities
            FROM devices d
            LEFT JOIN device_capabilities dc ON dc.device_id = d.id
            WHERE d.hub_id = ?
            GROUP BY d.id
            ORDER BY d.device_key
        """.trimIndent()

        connection.prepareStatement(sql).use { st ->
            st.setObject(1, hubUuid)

            st.executeQuery().use { rs ->
                val devices = mutableListOf<RoomDeviceResponse>()

                while (rs.next()) {
                    val capsArray = rs.getArray("capabilities")
                    val capabilities = (capsArray.array as Array<*>)
                        .map { it.toString() }

                    devices.add(
                        RoomDeviceResponse(
                            deviceId = rs.getString("id"),
                            deviceKey = rs.getString("device_key"),
                            name = rs.getString("name"),
                            type = rs.getString("type"),
                            enabled = rs.getBoolean("enabled"),
                            capabilities = capabilities,
                            currentState = rs.getString("current_state")
                        )
                    )
                }

                return devices
            }
        }
    }
}