package com.example.sqlite.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    var userId: Long = 0,

    @ColumnInfo(name = "email")
    var email: String = "",

    @ColumnInfo(name = "password_hash")
    var passwordHash: String = "",

    @ColumnInfo(name = "created_at")
    var createdAt: String? = null
)
