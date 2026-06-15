package com.vaulto.lite.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vaulto.lite.VaultoApplication
import com.vaulto.lite.data.local.entity.RecurrenceType
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Periodic worker (daily) that checks all expenses marked [isRecurring] and,
 * if the recurrence interval has elapsed since their last occurrence,
 * inserts a new expense row for the current period.
 *
 * Recurrence cadence is derived from [RecurrenceType]:
 * DAILY -> +1 day, WEEKLY -> +7 days, MONTHLY -> +1 month, YEARLY -> +1 year.
 */
class RecurringExpenseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as VaultoApplication
        val repository = app.repository

        val recurring = repository.observeRecurringExpenses().first()
        val now = Calendar.getInstance()

        recurring.forEach { expense ->
            if (isDue(expense.date, expense.recurrenceType, now.timeInMillis)) {
                val cal = Calendar.getInstance().apply { timeInMillis = now.timeInMillis }
                repository.addExpense(
                    expense.copy(
                        id = 0,
                        date = cal.timeInMillis,
                        month = cal.get(Calendar.MONTH) + 1,
                        year = cal.get(Calendar.YEAR)
                    )
                )
            }
        }

        return Result.success()
    }

    private fun isDue(lastDate: Long, type: RecurrenceType, nowMillis: Long): Boolean {
        val intervalMillis = when (type) {
            RecurrenceType.DAILY -> TimeUnit.DAYS.toMillis(1)
            RecurrenceType.WEEKLY -> TimeUnit.DAYS.toMillis(7)
            RecurrenceType.MONTHLY -> TimeUnit.DAYS.toMillis(30) // approximate
            RecurrenceType.YEARLY -> TimeUnit.DAYS.toMillis(365)
            RecurrenceType.NONE -> return false
        }
        return (nowMillis - lastDate) >= intervalMillis
    }
}
