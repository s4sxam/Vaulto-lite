package com.vaulto.lite.data.repository

import com.google.gson.Gson
import com.vaulto.lite.data.local.dao.BudgetDao
import com.vaulto.lite.data.local.dao.CategoryDao
import com.vaulto.lite.data.local.dao.ExpenseDao
import com.vaulto.lite.data.local.entity.BudgetEntity
import com.vaulto.lite.data.local.entity.CategoryEntity
import com.vaulto.lite.data.local.entity.DefaultCategories
import com.vaulto.lite.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Single source of truth for all local data. ViewModels depend on this,
 * never directly on DAOs.
 */
class BudgetRepository(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao
) {
    private val gson = Gson()

    // ---- Categories ----

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun seedDefaultCategoriesIfEmpty() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(DefaultCategories)
        }
    }

    suspend fun addCategory(category: CategoryEntity): Long = categoryDao.insert(category)

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.delete(category)

    // ---- Expenses ----

    fun observeExpensesForMonth(month: Int, year: Int): Flow<List<ExpenseEntity>> =
        expenseDao.observeForMonth(month, year)

    fun observeExpensesBetween(startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>> =
        expenseDao.observeBetween(startMillis, endMillis)

    fun observeAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeAll()

    fun observeTotalForMonth(month: Int, year: Int): Flow<Double> =
        expenseDao.observeTotalForMonth(month, year)

    fun observeCategoryTotalForMonth(categoryId: Long, month: Int, year: Int): Flow<Double> =
        expenseDao.observeCategoryTotalForMonth(categoryId, month, year)

    fun observeRecurringExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeRecurring()

    suspend fun getExpenseById(id: Long): ExpenseEntity? = expenseDao.getById(id)

    suspend fun addExpense(expense: ExpenseEntity): Long = expenseDao.insert(expense)

    suspend fun updateExpense(expense: ExpenseEntity) = expenseDao.update(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.delete(expense)

    // ---- Budgets ----

    fun observeBudgetForMonth(month: Int, year: Int): Flow<BudgetEntity?> =
        budgetDao.observeForMonth(month, year)

    fun observeAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.observeAll()

    suspend fun getBudgetForMonth(month: Int, year: Int): BudgetEntity? =
        budgetDao.getForMonth(month, year)

    suspend fun upsertBudget(budget: BudgetEntity): Long = budgetDao.upsert(budget)

    // ---- Backup / Restore ----

    /** Collects current categories, expenses, and budgets into a JSON string. */
    suspend fun exportBackupJson(): String {
        val categories = categoryDao.observeAll().first()
        val expenses = expenseDao.observeAll().first()
        val budgets = budgetDao.observeAll().first()
        val snapshot = BackupSnapshot(categories, expenses, budgets)
        return gson.toJson(snapshot)
    }

    /**
     * Replaces all local data with the contents of [json]. Existing rows are
     * cleared first (categories, expenses, budgets) so restore is a full
     * overwrite, matching the "Restore from JSON" confirmation flow in Settings.
     */
    suspend fun restoreFromBackupJson(json: String) {
        val snapshot = gson.fromJson(json, BackupSnapshot::class.java)
            ?: throw IllegalArgumentException("Invalid backup file")

        expenseDao.deleteAll()

        // Categories: insert any not already present (matched by name)
        val existingNames = categoryDao.observeAll().first().map { it.name }.toSet()
        val newCategories = snapshot.categories.filter { it.name !in existingNames }
        if (newCategories.isNotEmpty()) {
            categoryDao.insertAll(newCategories.map { it.copy(id = 0) })
        }

        // Re-map expenses to category ids by name (ids may differ after restore)
        val categoryByName = categoryDao.observeAll().first().associateBy { it.name }
        val restoredExpenses = snapshot.expenses.mapNotNull { expense ->
            val category = categoryByName[expense.categoryName] ?: return@mapNotNull null
            expense.copy(id = 0, categoryId = category.id)
        }
        expenseDao.insertAll(restoredExpenses)

        snapshot.budgets.forEach { budget ->
            budgetDao.upsert(budget.copy(id = 0))
        }
    }

    suspend fun clearAllExpenses() = expenseDao.deleteAll()
}

/** Serializable container used for JSON backup/restore (Settings > Data). */
data class BackupSnapshot(
    val categories: List<CategoryEntity>,
    val expenses: List<ExpenseEntity>,
    val budgets: List<BudgetEntity>
)
