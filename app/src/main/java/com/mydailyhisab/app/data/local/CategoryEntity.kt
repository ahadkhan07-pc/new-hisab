package com.mydailyhisab.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val type: TransactionType,
    val icon: String,
    val colorHex: String,
    val isDefault: Boolean = false
)
