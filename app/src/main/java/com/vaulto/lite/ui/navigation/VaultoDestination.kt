package com.vaulto.lite.ui.navigation

/** Top-level navigation destinations. */
sealed class VaultoDestination(val route: String) {
    data object Home : VaultoDestination("home")
    data object Analytics : VaultoDestination("analytics")
    data object Settings : VaultoDestination("settings")

    data object AddExpense : VaultoDestination("add_expense?expenseId={expenseId}") {
        const val ARG_EXPENSE_ID = "expenseId"
        fun route(expenseId: Long? = null) = "add_expense?expenseId=${expenseId ?: -1L}"
    }

    companion object {
        val bottomNavItems = listOf(Home, Analytics, Settings)
    }
}
