package com.vaulto.lite.domain.insights

import com.vaulto.lite.data.local.entity.ExpenseEntity
import java.util.Calendar

/** A single auto-generated insight card shown in the Analytics carousel. */
data class Insight(
    val icon: String,
    val text: String,
    val priority: Int = 0 // higher = shown first
)

/**
 * Pure, stateless functions that turn raw expense history into [Insight] cards.
 * No Android/DB dependencies so these are trivially unit-testable.
 */
object InsightsEngine {

    /**
     * Entry point: generates the full set of insights for the current period
     * given current-period and previous-period expenses, plus the active budget
     * and a map of recent month labels -> totals (for "best month" comparisons).
     */
    fun generateInsights(
        currentPeriodExpenses: List<ExpenseEntity>,
        previousPeriodExpenses: List<ExpenseEntity>,
        monthlyBudget: Double?,
        daysElapsedInMonth: Int,
        daysInMonth: Int,
        recentMonthlyTotals: Map<String, Double> = emptyMap()
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        categoryDeltaInsight(currentPeriodExpenses, previousPeriodExpenses)?.let { insights += it }
        biggestSpendingDayInsight(currentPeriodExpenses)?.let { insights += it }
        budgetPacingInsight(currentPeriodExpenses, monthlyBudget, daysElapsedInMonth, daysInMonth)?.let { insights += it }
        fastestGrowingCategoryInsight(currentPeriodExpenses, previousPeriodExpenses)?.let { insights += it }
        bestMonthInsight(recentMonthlyTotals)?.let { insights += it }

        return insights.sortedByDescending { it.priority }
    }

    /** "🔥 You spent X% more on <Category> this month vs last" */
    fun categoryDeltaInsight(
        current: List<ExpenseEntity>,
        previous: List<ExpenseEntity>
    ): Insight? {
        val currentByCategory = current.groupBy { it.categoryName }.mapValues { it.value.sumOf { e -> e.amount } }
        val previousByCategory = previous.groupBy { it.categoryName }.mapValues { it.value.sumOf { e -> e.amount } }

        val biggest = currentByCategory.entries
            .mapNotNull { (name, curAmount) ->
                val prevAmount = previousByCategory[name] ?: 0.0
                if (prevAmount <= 0.0) return@mapNotNull null
                val pctChange = ((curAmount - prevAmount) / prevAmount) * 100
                name to pctChange
            }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?: return null

        val (category, pct) = biggest
        return Insight(
            icon = "🔥",
            text = "You spent ${pct.toInt()}% more on $category this month vs last",
            priority = 3
        )
    }

    /** "📅 Your biggest spending day was <Day> — avg Nx weekdays" */
    fun biggestSpendingDayInsight(expenses: List<ExpenseEntity>): Insight? {
        if (expenses.isEmpty()) return null

        val byDayOfWeek = expenses.groupBy { dayOfWeek(it.date) }
            .mapValues { (_, list) -> list.sumOf { it.amount } / list.size }

        if (byDayOfWeek.isEmpty()) return null

        val (topDay, topAvg) = byDayOfWeek.maxByOrNull { it.value } ?: return null
        val otherDaysAvg = byDayOfWeek.filterKeys { it != topDay }.values
            .let { if (it.isEmpty()) 0.0 else it.average() }

        if (otherDaysAvg <= 0.0) return null

        val ratio = topAvg / otherDaysAvg
        return Insight(
            icon = "📅",
            text = "Your biggest spending day was ${dayName(topDay)} — avg ${String.format("%.1f", ratio)}x weekdays",
            priority = 2
        )
    }

    /**
     * "🎯 You're on track to stay under budget by ₹X this month" or
     * "⚠️ You're projected to go ₹X over budget this month"
     */
    fun budgetPacingInsight(
        currentExpenses: List<ExpenseEntity>,
        monthlyBudget: Double?,
        daysElapsed: Int,
        daysInMonth: Int
    ): Insight? {
        if (monthlyBudget == null || monthlyBudget <= 0.0 || daysElapsed <= 0) return null

        val spentSoFar = currentExpenses.sumOf { it.amount }
        val dailyAverage = spentSoFar / daysElapsed
        val projectedTotal = dailyAverage * daysInMonth
        val diff = monthlyBudget - projectedTotal

        return if (diff >= 0) {
            Insight(
                icon = "🎯",
                text = "You're on track to stay under budget by ₹${diff.toInt()} this month",
                priority = 4
            )
        } else {
            Insight(
                icon = "⚠️",
                text = "You're projected to go ₹${(-diff).toInt()} over budget this month",
                priority = 5
            )
        }
    }

    /** "⚠️ <Category> is your fastest-growing category (+X% over period)" */
    fun fastestGrowingCategoryInsight(
        current: List<ExpenseEntity>,
        previous: List<ExpenseEntity>
    ): Insight? {
        val currentByCategory = current.groupBy { it.categoryName }.mapValues { it.value.sumOf { e -> e.amount } }
        val previousByCategory = previous.groupBy { it.categoryName }.mapValues { it.value.sumOf { e -> e.amount } }

        val fastest = currentByCategory.entries
            .mapNotNull { (name, curAmount) ->
                val prevAmount = previousByCategory[name]
                if (prevAmount == null || prevAmount <= 0.0) return@mapNotNull null
                val pctChange = ((curAmount - prevAmount) / prevAmount) * 100
                name to pctChange
            }
            .filter { it.second >= 25 } // only flag significant growth
            .maxByOrNull { it.second }
            ?: return null

        val (category, pct) = fastest
        return Insight(
            icon = "⚠️",
            text = "$category is your fastest-growing category (+${pct.toInt()}% over this period)",
            priority = 1
        )
    }

    /** "🏆 Best month in the last N: <Month Year> (lowest spend)" */
    fun bestMonthInsight(monthlyTotals: Map<String, Double>): Insight? {
        if (monthlyTotals.size < 2) return null
        val (bestMonth, _) = monthlyTotals.minByOrNull { it.value } ?: return null
        return Insight(
            icon = "🏆",
            text = "Best month in the last ${monthlyTotals.size}: $bestMonth (lowest spend)",
            priority = 0
        )
    }

    // ---- helpers ----

    private fun dayOfWeek(epochMillis: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMillis
        return cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday ... 7 = Saturday
    }

    private fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.SUNDAY -> "Sunday"
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Unknown"
    }
}
