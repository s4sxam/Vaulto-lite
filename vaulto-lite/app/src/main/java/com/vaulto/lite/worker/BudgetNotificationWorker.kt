package com.vaulto.lite.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vaulto.lite.R
import com.vaulto.lite.VaultoApplication
import java.util.Calendar
import kotlinx.coroutines.flow.first

/**
 * Periodic worker that checks the current month's spend against the active
 * budget and fires a local notification at the 80% and 100% thresholds.
 * Styled with saffron accent + category emoji per spec (handled via
 * NotificationCompat builder + small icon / color).
 */
class BudgetNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "budget_alerts"
        const val NOTIFICATION_ID = 1001
        private const val THRESHOLD_WARNING = 0.8
        private const val THRESHOLD_EXCEEDED = 1.0
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as VaultoApplication
        val repository = app.repository

        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        val budget = repository.getBudgetForMonth(month, year) ?: return Result.success()
        if (budget.amount <= 0.0) return Result.success()

        val totalSpent = repository.observeTotalForMonth(month, year).first()
        val ratio = totalSpent / budget.amount

        when {
            ratio >= THRESHOLD_EXCEEDED -> notify(
                title = "🚨 Budget exceeded",
                text = "You've spent ${formatAmount(totalSpent)} of your ${formatAmount(budget.amount)} budget this month."
            )
            ratio >= THRESHOLD_WARNING -> notify(
                title = "⚠️ 80% of budget used",
                text = "You've used ${(ratio * 100).toInt()}% of your monthly budget."
            )
        }

        return Result.success()
    }

    private fun notify(title: String, text: String) {
        val context = applicationContext
        createChannelIfNeeded(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFF9F43.toInt()) // saffron accent
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifies you when nearing or exceeding your monthly budget"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun formatAmount(amount: Double): String =
        com.vaulto.lite.ui.screens.home.formatCurrency(amount)
}
