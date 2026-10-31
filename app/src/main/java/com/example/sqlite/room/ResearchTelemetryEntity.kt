package com.example.sqlite.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Thực thể lưu trữ dữ liệu đo lường thụ động (Passive Telemetry) phục vụ nghiên cứu khoa học.
 * Ghi lại đầy đủ các chỉ số: thời gian phản hồi (latency), độ chính xác, điểm SM-2 khách quan,
 * độ bền trí nhớ và xác suất lưu giữ theo đường cong quên Ebbinghaus.
 */
@Entity(
    tableName = "research_telemetry",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["session_id"]),
        Index(value = ["timestamp"])
    ]
)
data class ResearchTelemetryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: String = "",

    @ColumnInfo(name = "user_id")
    val userId: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: String = "",

    @ColumnInfo(name = "slo_topic")
    val sloTopic: String = "",

    @ColumnInfo(name = "challenge_type")
    val challengeType: String = "",

    @ColumnInfo(name = "item_identifier")
    val itemIdentifier: String = "",

    @ColumnInfo(name = "response_latency_ms")
    val responseLatencyMs: Long = 0L,

    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean = false,

    @ColumnInfo(name = "used_hint")
    val usedHint: Boolean = false,

    @ColumnInfo(name = "is_distracted")
    val isDistracted: Boolean = false,

    @ColumnInfo(name = "objective_quality")
    val objectiveQuality: Int = 0,

    @ColumnInfo(name = "easiness_factor")
    val easinessFactor: Float = 2.5f,

    @ColumnInfo(name = "interval_days")
    val intervalDays: Int = 1,

    @ColumnInfo(name = "retention_probability")
    val retentionProbability: Float = 1.0f
)
