package com.vaulto.lite

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vaulto.lite.data.local.VaultoDatabase
import com.vaulto.lite.data.repository.BudgetRepository
import com.vaulto.lite.data.settings.SettingsRepository
import com.vaulto.lite.worker.BudgetNotificationWorker
import com.vaulto.lite.worker.RecurringExpenseWorker
import java.util.concurrent.TimeUnit

/**
 * Lightweight manual DI: exposes singleton database + repositories.
 * No Hilt to keep the offline app minimal and dependency-light.
 */
class VaultoApplication : Application() {

    val database: VaultoDatabase by lazy { VaultoDatabase.getInstance(this) }

    val repository: BudgetRepository by lazy {
        BudgetRepository(
            categoryDao = database.categoryDao(),
            expenseDao = database.expenseDao(),
            budgetDao = database.budgetDao()
        )
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicWork()
    }

    private fun schedulePeriodicWork() {
        val workManager = WorkManager.getInstance(this)

        val budgetCheck = PeriodicWorkRequestBuilder<BudgetNotificationWorker>(
            12, TimeUnit.HOURS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            "budget_notification_check",
            ExistingPeriodicWorkPolicy.KEEP,
            budgetCheck
        )

        val recurringCheck = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(
            24, TimeUnit.HOURS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            "recurring_expense_check",
            ExistingPeriodicWorkPolicy.KEEP,
            recurringCheck
        )
    }
}
