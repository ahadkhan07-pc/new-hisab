package com.mydailyhisab.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class TransactionType {
    INCOME, EXPENSE, SAVINGS
}

enum class PaymentMethod {
    CASH, BKASH, NAGAD, BANK, CARD, OTHER
}

@Serializable
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val paymentMethod: PaymentMethod,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
