package com.example.data

import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    val allCategories: Flow<List<Category>> = budgetDao.getAllCategories()
    val allTransactions: Flow<List<Transaction>> = budgetDao.getAllTransactions()

    fun getTransactionsInRange(startAndOfEpoch: Long, endOfEpoch: Long): Flow<List<Transaction>> {
        return budgetDao.getTransactionsInRange(startAndOfEpoch, endOfEpoch)
    }

    suspend fun insertCategory(category: Category) {
        budgetDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        budgetDao.updateCategory(category)
    }

    suspend fun deleteCategoryById(id: Int) {
        budgetDao.deleteCategoryById(id)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        budgetDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        budgetDao.updateTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        budgetDao.deleteTransactionById(id)
    }
}
