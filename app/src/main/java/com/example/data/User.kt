package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String? = null,
    val avatarName: String = "avatar_piggy", // "avatar_piggy", "avatar_coin", "avatar_card", "avatar_wallet", etc.
    val isGoogleUser: Boolean = false
)
