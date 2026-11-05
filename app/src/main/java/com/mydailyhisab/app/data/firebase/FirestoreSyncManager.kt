package com.mydailyhisab.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mydailyhisab.app.data.local.CategoryEntity
import com.mydailyhisab.app.data.local.PaymentMethod
import com.mydailyhisab.app.data.local.TransactionEntity
import com.mydailyhisab.app.data.local.TransactionType
import com.mydailyhisab.app.data.repository.HisabRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

enum class AndroidSyncStatus {
    IDLE, SYNCING, SYNCED, FAILED
}

class FirestoreSyncManager(
    private val repository: HisabRepository
) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _syncStatus = MutableStateFlow(AndroidSyncStatus.IDLE)
    val syncStatus: StateFlow<AndroidSyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    suspend fun performFullSync(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("User not authenticated"))

        _syncStatus.value = AndroidSyncStatus.SYNCING
        _lastSyncError.value = null

        return try {
            val userDocRef = firestore.collection("users").document(user.uid)
            val transactionsCol = userDocRef.collection("transactions")
            val categoriesCol = userDocRef.collection("categories")

            // 1. Fetch remote transactions from Firestore
            val remoteTxsSnapshot = transactionsCol.get().await()
            val remoteTxs = remoteTxsSnapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val amount = doc.getDouble("amount") ?: 0.0
                    val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                    val categoryId = doc.getString("categoryId") ?: ""
                    val date = doc.getString("date") ?: ""
                    val time = doc.getString("time") ?: "12:00"
                    val methodStr = doc.getString("paymentMethod") ?: PaymentMethod.CASH.name
                    val note = doc.getString("note") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                    TransactionEntity(
                        id = id,
                        amount = amount,
                        type = TransactionType.valueOf(typeStr),
                        categoryId = categoryId,
                        date = date,
                        time = time,
                        paymentMethod = PaymentMethod.valueOf(methodStr),
                        note = note,
                        createdAt = createdAt
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 2. Fetch local transactions
            val localTxs = repository.allTransactions.first()
            val localTxMap = localTxs.associateBy { it.id }.toMutableMap()

            // Restore any remote transactions not yet in local Room DB
            remoteTxs.forEach { remoteTx ->
                if (!localTxMap.containsKey(remoteTx.id)) {
                    repository.addTransaction(remoteTx)
                    localTxMap[remoteTx.id] = remoteTx
                }
            }

            // 3. Push local transactions to Firestore in batches
            val allTxsToSync = localTxMap.values.toList()
            val batch = firestore.batch()
            allTxsToSync.forEach { tx ->
                val docRef = transactionsCol.document(tx.id)
                val data = mapOf(
                    "id" to tx.id,
                    "amount" to tx.amount,
                    "type" to tx.type.name,
                    "categoryId" to tx.categoryId,
                    "date" to tx.date,
                    "time" to tx.time,
                    "paymentMethod" to tx.paymentMethod.name,
                    "note" to tx.note,
                    "createdAt" to tx.createdAt,
                    "userId" to user.uid
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            // 4. Push categories to Firestore
            val localCategories = repository.allCategories.first()
            localCategories.forEach { cat ->
                val docRef = categoriesCol.document(cat.id)
                val data = mapOf(
                    "id" to cat.id,
                    "nameEn" to cat.nameEn,
                    "nameBn" to cat.nameBn,
                    "type" to cat.type.name,
                    "icon" to cat.icon,
                    "colorHex" to cat.colorHex,
                    "isDefault" to cat.isDefault,
                    "userId" to user.uid
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            batch.commit().await()

            _syncStatus.value = AndroidSyncStatus.SYNCED
            _lastSyncError.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = AndroidSyncStatus.FAILED
            _lastSyncError.value = e.localizedMessage ?: "Sync error"
            Result.failure(e)
        }
    }
}
