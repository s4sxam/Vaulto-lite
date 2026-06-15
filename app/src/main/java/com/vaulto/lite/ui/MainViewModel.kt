package com.vaulto.lite.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulto.lite.data.local.entity.BudgetEntity
import com.vaulto.lite.data.local.entity.CategoryEntity
import com.vaulto.lite.data.local.entity.ExpenseEntity
import com.vaulto.lite.data.repository.BudgetRepository
import com.vaulto.lite.data.settings.Currency
import com.vaulto.lite.data.settings.SettingsRepository
import com.vaulto.lite.data.settings.ThemeMode
import com.vaulto.lite.domain.insights.Insight
import com.vaulto.lite.domain.insights.InsightsEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(
    private val repository: BudgetRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Currently viewed month/year on Home (navigable via chevrons/swipe)
    private val _month = MutableStateFlow(currentMonth())
    val month: StateFlow<Int> = _month.asStateFlow()

    private val _year = MutableStateFlow(currentYear())
    val year: StateFlow<Int> = _year.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaultCategoriesIfEmpty()
        }
    }

    // ---- Categories ----

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Expenses for the currently selected month ----

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<List<ExpenseEntity>> = _month.combine(_year) { m, y -> m to y }
        .flatMapLatest { (m, y) -> repository.observeExpensesForMonth(m, y) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalSpent: StateFlow<Double> = _month.combine(_year) { m, y -> m to y }
        .flatMapLatest { (m, y) -> repository.observeTotalForMonth(m, y) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Total spend for the month immediately preceding the selected one, for delta comparisons. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val previousMonthTotal: StateFlow<Double> = _month.combine(_year) { m, y -> m to y }
        .flatMapLatest { (m, y) ->
            val (pm, py) = previousMonth(m, y)
            repository.observeTotalForMonth(pm, py)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ---- Budget for the currently selected month ----

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentBudget: StateFlow<BudgetEntity?> = _month.combine(_year) { m, y -> m to y }
        .flatMapLatest { (m, y) -> repository.observeBudgetForMonth(m, y) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categoryBudgets: StateFlow<Map<Long, Double>> = currentBudget
        .map { it?.categoryBudgets ?: emptyMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** categoryId -> amount spent this month, derived from [expenses]. */
    val categorySpending: StateFlow<Map<Long, Double>> = expenses
        .map { list -> list.groupBy { it.categoryId }.mapValues { (_, items) -> items.sumOf { it.amount } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ---- Derived: remaining + daily average ----

    val remaining: StateFlow<Double> = combine(totalSpent, currentBudget) { spent, budget ->
        (budget?.amount ?: 0.0) - spent
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val dailyAverage: StateFlow<Double> = combine(totalSpent, _month, _year) { spent, m, y ->
        val daysElapsed = daysElapsedInMonth(m, y)
        if (daysElapsed <= 0) 0.0 else spent / daysElapsed
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Sum of today's expenses, for the pull-down "Today's spend" chip on Home. */
    val todaysSpend: StateFlow<Double> = expenses.map { list ->
        val todayStart = com.vaulto.lite.ui.util.startOfDay(System.currentTimeMillis())
        val todayEnd = todayStart + 24L * 60 * 60 * 1000
        list.filter { it.date in todayStart until todayEnd }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ---- Insights (computed from current + previous period) ----

    @OptIn(ExperimentalCoroutinesApi::class)
    val insights: StateFlow<List<Insight>> = _month.combine(_year) { m, y -> m to y }
        .flatMapLatest { (m, y) ->
            val (prevMonth, prevYear) = previousMonth(m, y)
            combine(
                repository.observeExpensesForMonth(m, y),
                repository.observeExpensesForMonth(prevMonth, prevYear),
                repository.observeBudgetForMonth(m, y),
                repository.observeAllExpenses()
            ) { current, previous, budget, all ->
                InsightsEngine.generateInsights(
                    currentPeriodExpenses = current,
                    previousPeriodExpenses = previous,
                    monthlyBudget = budget?.amount,
                    daysElapsedInMonth = daysElapsedInMonth(m, y),
                    daysInMonth = daysInMonth(m, y),
                    recentMonthlyTotals = recentMonthlyTotals(all, m, y, months = 6)
                        .filterValues { it > 0.0 }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Recurring expenses ----

    val recurringExpenses: StateFlow<List<ExpenseEntity>> = repository.observeRecurringExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Appearance & currency (DataStore-backed) ----

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DEFAULT)

    val currency: StateFlow<Currency> = settingsRepository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Currency.DEFAULT)

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(mode)
    }

    fun setCurrency(currency: Currency) = viewModelScope.launch {
        settingsRepository.setCurrency(currency)
    }

    // ---- Backup / Restore ----

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    suspend fun restoreFromBackupJson(json: String) = repository.restoreFromBackupJson(json)

    // ---- Actions ----

    fun goToNextMonth() {
        val (m, y) = nextMonth(_month.value, _year.value)
        _month.value = m
        _year.value = y
    }

    fun goToPreviousMonth() {
        val (m, y) = previousMonth(_month.value, _year.value)
        _month.value = m
        _year.value = y
    }

    fun addExpense(expense: ExpenseEntity) = viewModelScope.launch {
        repository.addExpense(expense)
    }

    fun updateExpense(expense: ExpenseEntity) = viewModelScope.launch {
        repository.updateExpense(expense)
    }

    fun deleteExpense(expense: ExpenseEntity) = viewModelScope.launch {
        repository.deleteExpense(expense)
    }

    suspend fun getExpenseById(id: Long): ExpenseEntity? = repository.getExpenseById(id)

    fun setBudget(budget: BudgetEntity) = viewModelScope.launch {
        repository.upsertBudget(budget)
    }

    fun addCategory(category: CategoryEntity) = viewModelScope.launch {
        repository.addCategory(category)
    }

    fun updateCategory(category: CategoryEntity) = viewModelScope.launch {
        repository.updateCategory(category)
    }

    fun deleteCategory(category: CategoryEntity) = viewModelScope.launch {
        repository.deleteCategory(category)
    }

    // ---- Date helpers ----

    private fun currentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    private fun nextMonth(m: Int, y: Int): Pair<Int, Int> =
        if (m == 12) 1 to (y + 1) else (m + 1) to y

    private fun previousMonth(m: Int, y: Int): Pair<Int, Int> =
        if (m == 1) 12 to (y - 1) else (m - 1) to y

    private fun daysInMonth(m: Int, y: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(y, m - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun daysElapsedInMonth(m: Int, y: Int): Int {
        val now = Calendar.getInstance()
        return if (now.get(Calendar.MONTH) + 1 == m && now.get(Calendar.YEAR) == y) {
            now.get(Calendar.DAY_OF_MONTH)
        } else {
            daysInMonth(m, y) // past month is fully elapsed
        }
    }

    /**
     * Builds a "Month Year" -> total spend map for the [months] periods ending
     * at (and including) [endMonth]/[endYear], used by [InsightsEngine.bestMonthInsight].
     */
    private fun recentMonthlyTotals(
        allExpenses: List<ExpenseEntity>,
        endMonth: Int,
        endYear: Int,
        months: Int
    ): Map<String, Double> {
        val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        val result = LinkedHashMap<String, Double>()
        var m = endMonth
        var y = endYear
        repeat(months) {
            val total = allExpenses
                .filter { it.month == m && it.year == y }
                .sumOf { it.amount }
            result["${monthNames[m - 1]} $y"] = total
            val (pm, py) = previousMonth(m, y)
            m = pm
            y = py
        }
        return result
    }
}
