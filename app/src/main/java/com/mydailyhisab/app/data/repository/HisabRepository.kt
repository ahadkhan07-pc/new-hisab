package com.mydailyhisab.app.data.repository

import com.mydailyhisab.app.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HisabRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()

    fun getTransactionsForMonth(year: Int, month: Int): Flow<List<TransactionEntity>> {
        val monthPrefix = String.format("%04d-%02d", year, month)
        return transactionDao.getTransactionsByMonthFlow(monthPrefix)
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun addCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(id: String): Boolean {
        return categoryDao.deleteCustomCategoryById(id) > 0
    }
}
