package com.stressless.repositories

import com.stressless.db.DatabaseFactory
import com.stressless.dto.app.*
import com.stressless.mqtt.MqttClientService
import com.hivemq.client.mqtt.datatypes.MqttQos
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

object AppRepository {

    private val json = Json {
        explicitNulls = false
        prettyPrint = true
        encodeDefaults = true
    }

    fun getHome(userId: UUID): AppHomeResponse {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val userSql = """
                SELECT id, name, email, is_calibrated, baseline_bpm, baseline_gsr
                FROM users
                WHERE id = ?
                LIMIT 1
            """.trimIndent()

            val user = connection.prepareStatement(userSql).use { st ->
                st.setObject(1, userId)
                st.executeQuery().use { rs ->
                    if (!rs.next()) error("User not found")

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

            val stress = getLatestStress(userId, connection, baselines.first, baselines.second)
            val band = getActiveBand(userId, connection)
            val room = getPrimaryRoom(userId, connection)
            val hub = getPrimaryHub(userId, connection)
            val activeProfile = getLatestActiveProfile(userId, connection, stress.detectedState)
            val lastCommand = getLastCommand(userId, connection)

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

    fun getRoomPrimary(userId: UUID): RoomPrimaryResponse {
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
                st.setObject(1, userId)
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

    fun getBands(userId: UUID): BandsResponse {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                SELECT id, band_id, serial_number, is_active, status::text, battery_level, last_seen_at, created_at
                FROM bands
                WHERE user_id = ?
                ORDER BY created_at DESC
            """.trimIndent()

            connection.prepareStatement(sql).use { st ->
                st.setObject(1, userId)
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
                                batteryLevel = (rs.getObject("battery_level") as? Number)?.toInt(),
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

    fun getProfiles(userId: UUID): ProfilesResponse {
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
                st.setObject(1, userId)
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

    fun getRecentEvents(userId: UUID, limit: Int = 20): StressRecentEventsResponse {
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
                st.setObject(1, userId)
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
        userId: UUID,
        hubLogicalId: String,
        newState: String
    ): ChangeOperationalStateResponse {
        val allowed = setOf("ACTIVE", "PAUSED", "MANUAL", "EXIT_MODE")

        require(newState in allowed) {
            "Invalid operational state: $newState"
        }

        DatabaseFactory.getDataSource().connection.use { connection ->
            val selectSql = """
                SELECT h.id, r.id AS room_id, h.operational_state::text
                FROM hubs h
                JOIN rooms r ON r.id = h.room_id
                WHERE h.hub_id = ?
                  AND r.user_id = ?
                LIMIT 1
            """.trimIndent()

            val roomUuid: UUID
            val hubUuid: UUID
            val previousState: String

            connection.prepareStatement(selectSql).use { st ->
                st.setString(1, hubLogicalId)
                st.setObject(2, userId)

                st.executeQuery().use { rs ->
                    if (!rs.next()) error("Hub not found")

                    hubUuid = UUID.fromString(rs.getString("id"))
                    roomUuid = UUID.fromString(rs.getString("room_id"))
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

            if (newState == "EXIT_MODE") {
                publishExitModeCommand(
                    connection = connection,
                    hubUuid = hubUuid,
                    roomUuid = roomUuid,
                    userId = userId,
                    hubLogicalId = hubLogicalId
                )
            }

            if (newState == "ACTIVE" && previousState != "ACTIVE") {
                publishCurrentProfileCommand(
                    connection = connection,
                    hubUuid = hubUuid,
                    roomUuid = roomUuid,
                    userId = userId,
                    hubLogicalId = hubLogicalId
                )
            }

            return ChangeOperationalStateResponse(
                hubId = hubUuid.toString(),
                previousState = previousState,
                newState = newState,
                changedAt = Instant.now().toString()
            )
        }
    }

    private fun publishCurrentProfileCommand(
        connection: java.sql.Connection,
        hubUuid: UUID,
        roomUuid: UUID,
        userId: UUID,
        hubLogicalId: String
    ) {
        val latestStateSql = """
        SELECT id, state::text
        FROM detected_states
        WHERE user_id = ?
          AND hub_id = ?
        ORDER BY detected_at DESC
        LIMIT 1
    """.trimIndent()

        val detectedStateId: UUID
        val currentState: String

        connection.prepareStatement(latestStateSql).use { st ->
            st.setObject(1, userId)
            st.setObject(2, hubUuid)

            st.executeQuery().use { rs ->
                if (!rs.next()) {
                    return
                }

                detectedStateId = UUID.fromString(rs.getString("id"))
                currentState = rs.getString("state")
            }
        }

        val profileSql = """
        SELECT id
        FROM environment_profiles
        WHERE user_id = ?
          AND target_state = ?::physiological_state_enum
          AND is_active = true
        LIMIT 1
    """.trimIndent()

        val profileId: UUID

        connection.prepareStatement(profileSql).use { st ->
            st.setObject(1, userId)
            st.setString(2, currentState)

            st.executeQuery().use { rs ->
                if (!rs.next()) {
                    return
                }

                profileId = UUID.fromString(rs.getString("id"))
            }
        }

        val actionsSql = """
        SELECT
            d.device_key,
            pa.action::text AS action,
            pa.value::text AS value
        FROM profile_actions pa
        JOIN devices d ON d.id = pa.device_id
        WHERE pa.profile_id = ?
        ORDER BY pa.order_index ASC
    """.trimIndent()

        val actions = mutableListOf<String>()

        connection.prepareStatement(actionsSql).use { st ->
            st.setObject(1, profileId)

            st.executeQuery().use { rs ->
                while (rs.next()) {
                    val deviceKey = rs.getString("device_key")
                    val action = rs.getString("action")
                    val rawValue = rs.getString("value")

                    actions.add(
                        """
                    {
                      "deviceKey": "$deviceKey",
                      "action": "$action",
                      "value": $rawValue
                    }
                    """.trimIndent()
                    )
                }
            }
        }

        if (actions.isEmpty()) {
            return
        }

        val commandId = UUID.randomUUID().toString()

        val actionsJson = actions.joinToString(
            separator = ",\n",
            prefix = "[\n",
            postfix = "\n]"
        )

        val payloadJson = """
        {
          "commandId": "$commandId",
          "hubId": "$hubLogicalId",
          "source": "AUTOMATION",
          "triggeredByState": "$currentState",
          "reason": "REAPPLY_PROFILE_AFTER_ACTIVE_MODE",
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
        VALUES (?, ?, ?, 'AUTOMATION'::command_source_enum, ?, ?::jsonb, 'SENT'::command_status_enum, NOW())
    """.trimIndent()

        connection.prepareStatement(insertSql).use { st ->
            st.setObject(1, hubUuid)
            st.setObject(2, roomUuid)
            st.setObject(3, userId)
            st.setObject(4, detectedStateId)
            st.setString(5, payloadJson)
            st.executeUpdate()
        }

        MqttClientService.publishJson(
            topic = "stressless/hub/$hubLogicalId/commands",
            payload = payloadJson,
            qos = MqttQos.AT_LEAST_ONCE,
            retain = false
        )
    }

    private fun publishExitModeCommand(
        connection: java.sql.Connection,
        hubUuid: UUID,
        roomUuid: UUID,
        userId: UUID,
        hubLogicalId: String
    ) {
        val commandId = UUID.randomUUID().toString()

        val payloadJson = """
        {
          "commandId": "$commandId",
          "hubId": "$hubLogicalId",
          "source": "MANUAL_APP",
          "actions": [
            {
              "deviceKey": "led-rgb-001",
              "action": "TURN_OFF",
              "value": false
            },
            {
              "deviceKey": "fan-001",
              "action": "TURN_OFF",
              "value": false
            },
            {
              "deviceKey": "display-001",
              "action": "SHOW_MESSAGE",
              "value": "Modo salida"
            },
            {
              "deviceKey": "buzzer-001",
              "action": "TURN_OFF",
              "value": false
            }
          ],
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
        VALUES (?, ?, ?, 'MANUAL_APP'::command_source_enum, NULL, ?::jsonb, 'SENT'::command_status_enum, NOW())
    """.trimIndent()

        connection.prepareStatement(insertSql).use { st ->
            st.setObject(1, hubUuid)
            st.setObject(2, roomUuid)
            st.setObject(3, userId)
            st.setString(4, payloadJson)
            st.executeUpdate()
        }

        MqttClientService.publishJson(
            topic = "stressless/hub/$hubLogicalId/commands",
            payload = payloadJson,
            qos = MqttQos.AT_LEAST_ONCE,
            retain = false
        )
    }

    fun sendManualCommand(
        userId: UUID,
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
                st.setObject(2, userId)

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
                VALUES (?, ?, ?, 'MANUAL_APP'::command_source_enum, NULL, ?::jsonb, 'SENT'::command_status_enum, NOW())
            """.trimIndent()

            connection.prepareStatement(insertSql).use { st ->
                st.setObject(1, hubUuid)
                st.setObject(2, roomUuid)
                st.setObject(3, userId)
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
        userId: UUID,
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
            st.setObject(1, userId)

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

    private fun getActiveBand(userId: UUID, connection: java.sql.Connection): AppBandSummary? {
        val sql = """
            SELECT id, band_id, status::text, is_active, battery_level, last_seen_at
            FROM bands
            WHERE user_id = ?
              AND is_active = true
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { st ->
            st.setObject(1, userId)

            st.executeQuery().use { rs ->
                if (!rs.next()) return null

                return AppBandSummary(
                    bandId = rs.getString("id"),
                    bandLogicalId = rs.getString("band_id"),
                    status = rs.getString("status"),
                    isActive = rs.getBoolean("is_active"),
                    batteryLevel = (rs.getObject("battery_level") as? Number)?.toInt(),
                    lastSeenAt = rs.getTimestamp("last_seen_at")?.toInstant()?.toString()
                )
            }
        }
    }

    private fun getPrimaryRoom(userId: UUID, connection: java.sql.Connection): AppRoomSummary? {
        val sql = """
            SELECT id, name
            FROM rooms
            WHERE user_id = ?
              AND is_primary = true
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { st ->
            st.setObject(1, userId)

            st.executeQuery().use { rs ->
                if (!rs.next()) return null

                return AppRoomSummary(
                    roomId = rs.getString("id"),
                    name = rs.getString("name")
                )
            }
        }
    }

    private fun getPrimaryHub(userId: UUID, connection: java.sql.Connection): AppHubSummary? {
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
            st.setObject(1, userId)

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
        userId: UUID,
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
            st.setObject(1, userId)
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

    private fun getLastCommand(userId: UUID, connection: java.sql.Connection): AppCommandSummary? {
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
            st.setObject(1, userId)

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

    fun getProfileDetail(
        userId: UUID,
        profileId: UUID
    ): ProfileDetailResponse {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val profileSql = """
            SELECT id, name, target_state::text, is_active, use_automatic_fallback
            FROM environment_profiles
            WHERE id = ?
              AND user_id = ?
            LIMIT 1
        """.trimIndent()

            val profile: ProfileDetailResponse

            connection.prepareStatement(profileSql).use { st ->
                st.setObject(1, profileId)
                st.setObject(2, userId)

                st.executeQuery().use { rs ->
                    if (!rs.next()) {
                        error("Profile not found")
                    }

                    profile = ProfileDetailResponse(
                        profileId = rs.getString("id"),
                        name = rs.getString("name"),
                        targetState = rs.getString("target_state"),
                        isActive = rs.getBoolean("is_active"),
                        useAutomaticFallback = rs.getBoolean("use_automatic_fallback"),
                        actions = emptyList()
                    )
                }
            }

            val actions = getProfileActions(connection, profileId)

            return profile.copy(actions = actions)
        }
    }

    fun createProfile(
        userId: UUID,
        request: CreateProfileRequest
    ): SaveProfileResponse {
        validateProfileRequest(
            name = request.name,
            targetState = request.targetState,
            actions = request.actions
        )

        DatabaseFactory.getDataSource().connection.use { connection ->
            connection.autoCommit = false

            try {
                val profileId = UUID.randomUUID()

                if (request.isActive) {
                    deactivateOtherActiveProfilesForState(
                        connection = connection,
                        userId = userId,
                        targetState = request.targetState
                    )
                }

                val insertProfileSql = """
                INSERT INTO environment_profiles (
                    id,
                    user_id,
                    name,
                    target_state,
                    is_active,
                    use_automatic_fallback,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?::physiological_state_enum, ?, ?, NOW(), NOW())
            """.trimIndent()

                connection.prepareStatement(insertProfileSql).use { st ->
                    st.setObject(1, profileId)
                    st.setObject(2, userId)
                    st.setString(3, request.name.trim())
                    st.setString(4, request.targetState)
                    st.setBoolean(5, request.isActive)
                    st.setBoolean(6, request.useAutomaticFallback)
                    st.executeUpdate()
                }

                replaceProfileActions(
                    connection = connection,
                    userId = userId,
                    profileId = profileId,
                    actions = request.actions
                )

                connection.commit()

                return SaveProfileResponse(
                    profileId = profileId.toString(),
                    message = "Perfil creado correctamente."
                )
            } catch (ex: Exception) {
                connection.rollback()
                throw ex
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun updateProfile(
        userId: UUID,
        profileId: UUID,
        request: UpdateProfileRequest
    ): SaveProfileResponse {
        validateProfileRequest(
            name = request.name,
            targetState = request.targetState,
            actions = request.actions
        )

        DatabaseFactory.getDataSource().connection.use { connection ->
            connection.autoCommit = false

            try {

                if (request.isActive) {
                    deactivateOtherActiveProfilesForState(
                        connection = connection,
                        userId = userId,
                        targetState = request.targetState,
                        exceptProfileId = profileId
                    )
                }

                val updateSql = """
                UPDATE environment_profiles
                SET name = ?,
                    target_state = ?::physiological_state_enum,
                    is_active = ?,
                    use_automatic_fallback = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND user_id = ?
            """.trimIndent()

                val updatedRows = connection.prepareStatement(updateSql).use { st ->
                    st.setString(1, request.name.trim())
                    st.setString(2, request.targetState)
                    st.setBoolean(3, request.isActive)
                    st.setBoolean(4, request.useAutomaticFallback)
                    st.setObject(5, profileId)
                    st.setObject(6, userId)
                    st.executeUpdate()
                }

                if (updatedRows == 0) {
                    error("Profile not found")
                }

                replaceProfileActions(
                    connection = connection,
                    userId = userId,
                    profileId = profileId,
                    actions = request.actions
                )

                connection.commit()

                return SaveProfileResponse(
                    profileId = profileId.toString(),
                    message = "Perfil actualizado correctamente."
                )
            } catch (ex: Exception) {
                connection.rollback()
                throw ex
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun updateProfileActive(
        userId: UUID,
        profileId: UUID,
        isActive: Boolean
    ): SaveProfileResponse {
        DatabaseFactory.getDataSource().connection.use { connection ->
            connection.autoCommit = false

            try {
                val selectSql = """
                SELECT target_state::text
                FROM environment_profiles
                WHERE id = ?
                  AND user_id = ?
                LIMIT 1
            """.trimIndent()

                val targetState: String

                connection.prepareStatement(selectSql).use { st ->
                    st.setObject(1, profileId)
                    st.setObject(2, userId)

                    st.executeQuery().use { rs ->
                        if (!rs.next()) {
                            error("Profile not found")
                        }

                        targetState = rs.getString("target_state")
                    }
                }

                if (isActive) {
                    deactivateOtherActiveProfilesForState(
                        connection = connection,
                        userId = userId,
                        targetState = targetState,
                        exceptProfileId = profileId
                    )
                }

                val updateSql = """
                UPDATE environment_profiles
                SET is_active = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND user_id = ?
            """.trimIndent()

                val updatedRows = connection.prepareStatement(updateSql).use { st ->
                    st.setBoolean(1, isActive)
                    st.setObject(2, profileId)
                    st.setObject(3, userId)
                    st.executeUpdate()
                }

                if (updatedRows == 0) {
                    error("Profile not found")
                }

                connection.commit()

                return SaveProfileResponse(
                    profileId = profileId.toString(),
                    message = if (isActive) {
                        "Perfil activado correctamente."
                    } else {
                        "Perfil desactivado correctamente."
                    }
                )
            } catch (ex: Exception) {
                connection.rollback()
                throw ex
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun getProfileActions(
        connection: java.sql.Connection,
        profileId: UUID
    ): List<ProfileActionResponse> {
        val sql = """
        SELECT
            pa.id AS action_id,
            d.id AS device_id,
            d.device_key,
            d.name AS device_name,
            d.type::text AS device_type,
            pa.action::text AS action,
            pa.value,
            pa.order_index
        FROM profile_actions pa
        JOIN devices d ON d.id = pa.device_id
        WHERE pa.profile_id = ?
        ORDER BY pa.order_index ASC
    """.trimIndent()

        connection.prepareStatement(sql).use { st ->
            st.setObject(1, profileId)

            st.executeQuery().use { rs ->
                val actions = mutableListOf<ProfileActionResponse>()

                while (rs.next()) {
                    actions.add(
                        ProfileActionResponse(
                            actionId = rs.getString("action_id"),
                            deviceId = rs.getString("device_id"),
                            deviceKey = rs.getString("device_key"),
                            deviceName = rs.getString("device_name"),
                            deviceType = rs.getString("device_type"),
                            action = rs.getString("action"),
                            value = json.parseToJsonElement(rs.getString("value")),
                            orderIndex = rs.getInt("order_index")
                        )
                    )
                }

                return actions
            }
        }
    }

    private fun replaceProfileActions(
        connection: java.sql.Connection,
        userId: UUID,
        profileId: UUID,
        actions: List<SaveProfileActionRequest>
    ) {
        val deleteSql = """
        DELETE FROM profile_actions
        WHERE profile_id = ?
    """.trimIndent()

        connection.prepareStatement(deleteSql).use { st ->
            st.setObject(1, profileId)
            st.executeUpdate()
        }

        if (actions.isEmpty()) {
            return
        }

        val validateDeviceSql = """
        SELECT d.id
        FROM devices d
        JOIN hubs h ON h.id = d.hub_id
        JOIN rooms r ON r.id = h.room_id
        WHERE d.id = ?
          AND r.user_id = ?
        LIMIT 1
    """.trimIndent()

        val insertSql = """
            INSERT INTO profile_actions (
                profile_id,
                device_id,
                action,
                value,
                order_index
            )
            VALUES (?, ?, ?::action_enum, ?::jsonb, ?)
        """.trimIndent()

        actions.forEachIndexed { index, actionRequest ->
            val deviceUuid = UUID.fromString(actionRequest.deviceId)

            val validDevice = connection.prepareStatement(validateDeviceSql).use { st ->
                st.setObject(1, deviceUuid)
                st.setObject(2, userId)

                st.executeQuery().use { rs ->
                    rs.next()
                }
            }

            if (!validDevice) {
                error("Invalid device for profile action: ${actionRequest.deviceId}")
            }

            connection.prepareStatement(insertSql).use { st ->
                st.setObject(1, profileId)
                st.setObject(2, deviceUuid)
                st.setString(3, actionRequest.action)
                st.setString(4, actionRequest.value.toString())
                st.setInt(5, actionRequest.orderIndex.takeIf { it >= 0 } ?: index)
                st.executeUpdate()
            }
        }
    }

    private fun validateProfileRequest(
        name: String,
        targetState: String,
        actions: List<SaveProfileActionRequest>
    ) {
        val allowedStates = setOf(
            "HIGH_STRESS",
            "MODERATE_STRESS",
            "NORMAL",
            "RELAXED",
            "MODERATE_RELAXED"
        )

        require(name.trim().isNotBlank()) {
            "PROFILE_NAME_REQUIRED"
        }

        require(targetState in allowedStates) {
            "INVALID_TARGET_STATE"
        }

        require(actions.size <= 20) {
            "TOO_MANY_PROFILE_ACTIONS"
        }

        actions.forEach { action ->
            require(action.deviceId.isNotBlank()) {
                "DEVICE_ID_REQUIRED"
            }

            require(action.action.isNotBlank()) {
                "ACTION_REQUIRED"
            }
        }
    }

    private fun deactivateOtherActiveProfilesForState(
        connection: java.sql.Connection,
        userId: UUID,
        targetState: String,
        exceptProfileId: UUID? = null
    ) {
        val sql = if (exceptProfileId != null) {
            """
            UPDATE environment_profiles
            SET is_active = false,
                updated_at = NOW()
            WHERE user_id = ?
              AND target_state = ?::physiological_state_enum
              AND id <> ?
              AND is_active = true
        """.trimIndent()
        } else {
            """
            UPDATE environment_profiles
            SET is_active = false,
                updated_at = NOW()
            WHERE user_id = ?
              AND target_state = ?::physiological_state_enum
              AND is_active = true
        """.trimIndent()
        }

        connection.prepareStatement(sql).use { st ->
            st.setObject(1, userId)
            st.setString(2, targetState)

            if (exceptProfileId != null) {
                st.setObject(3, exceptProfileId)
            }

            st.executeUpdate()
        }
    }
}