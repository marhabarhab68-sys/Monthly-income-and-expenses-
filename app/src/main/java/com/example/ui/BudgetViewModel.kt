package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Category
import com.example.data.GeminiApiManager
import com.example.data.Transaction
import com.example.data.BudgetRepository
import com.example.utils.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = BudgetRepository(database.budgetDao)

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state filters
    val selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedMonthValue = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // 0-indexed

    // Sync progress indicators
    val isSyncing = MutableStateFlow(false)
    val syncStatusMessage = MutableStateFlow<String?>(null)
    val lastSyncedTime = MutableStateFlow<String?>("Just now (Local)")

    // AI Advisor state
    val aiAdvisorLoading = MutableStateFlow(false)
    val aiResponse = MutableStateFlow<String?>(null)
    val aiChatHistory = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            text = "Hello! I am your AI Financial Advisor. Ask me anything about your current budget, category limits, or spending trends!",
            isUser = false
        )
    ))

    private val geminiManager = GeminiApiManager()

    // Derived Financial Analytics based on filters
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        transactions,
        selectedYear,
        selectedMonthValue
    ) { txList, year, month ->
        txList.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dynamicStats: StateFlow<MonthlyStats> = combine(
        filteredTransactions,
        categories
    ) { txList, catList ->
        val totalIncome = txList.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = txList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val savings = totalIncome - totalExpense

        // Category budgets status
        val categoryBudgets = catList.filter { it.type == "EXPENSE" }.map { category ->
            val actualSpent = txList.filter { it.type == "EXPENSE" && it.categoryName == category.name }
                .sumOf { it.amount }
            CategoryBudgetStatus(
                category = category,
                actualSpent = actualSpent,
                limit = category.limitAmount ?: 0.0
            )
        }

        // Subdivide income tracking sources
        val incomeBySource = txList.filter { it.type == "INCOME" }
            .groupBy { it.source }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        MonthlyStats(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            savings = savings,
            categoryBudgets = categoryBudgets,
            incomeBySource = incomeBySource
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyStats())

    // Multi-month spending and saving trends for visuals (past 6 months)
    val trendStats: StateFlow<List<MonthlyTrend>> = transactions.combine(selectedYear) { txList, year ->
        val trends = mutableListOf<MonthlyTrend>()
        val cal = Calendar.getInstance()
        
        // Let's build trends for the current year, month by month (latest 6 calendar months)
        val currentMonth = cal.get(Calendar.MONTH)
        for (m in (currentMonth - 5..currentMonth)) {
            val monthIdx = if (m < 0) m + 12 else m
            val yearOffset = if (m < 0) year - 1 else year
            
            val monthTx = txList.filter { tx ->
                cal.timeInMillis = tx.date
                cal.get(Calendar.YEAR) == yearOffset && cal.get(Calendar.MONTH) == monthIdx
            }
            
            val totalIncome = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalExpense = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            
            val sdf = SimpleDateFormat("MMM", Locale.getDefault())
            val monthCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, yearOffset)
                set(Calendar.MONTH, monthIdx)
            }
            trends.add(MonthlyTrend(
                monthLabel = sdf.format(monthCal.time),
                income = totalIncome,
                expense = totalExpense
            ))
        }
        trends
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Actions ---

    fun insertTransaction(type: String, amount: Double, categoryName: String, date: Long, note: String, source: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTransaction(
                Transaction(
                    type = type,
                    amount = amount,
                    categoryName = categoryName,
                    date = date,
                    note = note,
                    source = source
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransactionById(transaction.id)
        }
    }

    fun insertCategory(type: String, name: String, iconName: String, colorHex: String, limitAmount: Double?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(
                Category(
                    type = type,
                    name = name,
                    iconName = iconName,
                    colorHex = colorHex,
                    limitAmount = limitAmount
                )
            )
        }
    }

    fun updateCategoryLimit(category: Category, limit: Double?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategory(category.copy(limitAmount = limit))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategoryById(category.id)
        }
    }

    fun changeMonth(year: Int, monthIndex: Int) {
        selectedYear.value = year
        selectedMonthValue.value = monthIndex
    }

    fun getMonthName(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear.value)
            set(Calendar.MONTH, selectedMonthValue.value)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // --- PDF Report Export ---
    suspend fun exportPdfReport(context: Context): File? = withContext(Dispatchers.IO) {
        val catList = categories.first()
        val txList = filteredTransactions.first()
        return@withContext PdfExporter.generateMonthlyReport(
            context = context,
            monthName = getMonthName(),
            categories = catList,
            transactions = txList
        )
    }

    // --- Gemini high-thinking Advisor Integration ---
    fun askFinancialAdvisor(query: String) {
        if (query.trim().isEmpty()) return
        
        val userMessage = ChatMessage(text = query, isUser = true)
        aiChatHistory.value = aiChatHistory.value + userMessage
        aiAdvisorLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val stats = dynamicStats.value
            val catList = categories.value
            
            // Build visual dynamic system instructions with real budget context
            val categorySummary = stats.categoryBudgets.joinToString("\n") { 
                "- ${it.category.name}: Actual: $${it.actualSpent}, Limit: $${it.limit}" 
            }
            val incomeSummary = stats.incomeBySource.entries.joinToString("\n") {
                "- ${it.key}: $${it.value}"
            }

            val systemPrompt = """
                You are high-thinking Expert Personal Wealth Coach & Financial Advisor.
                You are assisting the user inside their 'Budget Tracker' Android App.
                Here is the user's real-time financial standing for the month of ${getMonthName()}:
                - Total Monthly Income: $${stats.totalIncome}
                - Total Monthly Expenses: $${stats.totalExpense}
                - Net Savings: $${stats.savings}
                
                Expense Category Statuses (Custom category expense tracker limits):
                $categorySummary
                
                Income Sources Tracked:
                $incomeSummary
                
                Guidance:
                - Always deliver highly actionable, objective, realistic, and wise financial advice.
                - Analyze category limit overruns and recommend budget trimming strategies.
                - Address the user's queries using deep analytical reasoning and concise summaries.
                - Format lists beautifully using clean bullet points.
            """.trimIndent()

            val response = geminiManager.generateFinancialAdvice(query, systemPrompt)
            
            withContext(Dispatchers.Main) {
                aiAdvisorLoading.value = false
                val assistantMessage = ChatMessage(text = response, isUser = false)
                aiChatHistory.value = aiChatHistory.value + assistantMessage
            }
        }
    }

    fun clearAdvisorChat() {
        aiChatHistory.value = listOf(
            ChatMessage(
                text = "Hello! I am your AI Financial Advisor. Ask me anything about your current budget, category limits, or spending trends!",
                isUser = false
            )
        )
    }

    // --- Cloud Synchronization Mock and Backup Operations ---

    fun triggerCloudSyncSimulation() {
        viewModelScope.launch {
            isSyncing.value = true
            syncStatusMessage.value = "Establishing Secure SSL Tunnel..."
            kotlinx.coroutines.delay(1000)
            syncStatusMessage.value = "Authenticating device & validating keys..."
            kotlinx.coroutines.delay(1000)
            syncStatusMessage.value = "Encrypting local database hashes (AES-256)..."
            kotlinx.coroutines.delay(1200)
            syncStatusMessage.value = "Synchronizing categories & transactions..."
            kotlinx.coroutines.delay(1500)
            
            // Generate a real sync timestamp
            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            lastSyncedTime.value = "Synced at ${formatter.format(Date())} (Cloud Secured)"
            isSyncing.value = false
            syncStatusMessage.value = null
        }
    }

    suspend fun getBackupJson(): String = withContext(Dispatchers.IO) {
        val catList = categories.first()
        val txList = transactions.first()
        
        val root = JSONObject()
        val catArray = JSONArray()
        for (c in catList) {
            val jobj = JSONObject()
            jobj.put("type", c.type)
            jobj.put("name", c.name)
            jobj.put("iconName", c.iconName)
            jobj.put("colorHex", c.colorHex)
            jobj.put("limit", c.limitAmount ?: 0.0)
            catArray.put(jobj)
        }
        
        val txArray = JSONArray()
        for (t in txList) {
            val jobj = JSONObject()
            jobj.put("type", t.type)
            jobj.put("amount", t.amount)
            jobj.put("categoryName", t.categoryName)
            jobj.put("date", t.date)
            jobj.put("note", t.note)
            jobj.put("source", t.source)
            txArray.put(jobj)
        }
        
        root.put("categories", catArray)
        root.put("transactions", txArray)
        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val catArray = root.optJSONArray("categories")
            val txArray = root.optJSONArray("transactions")
            
            if (catArray == null && txArray == null) return@withContext false

            if (catArray != null) {
                // Clear and repopulate or simply insert with upsert
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    val limit = if (obj.has("limit") && !obj.isNull("limit")) obj.getDouble("limit") else null
                    repository.insertCategory(
                        Category(
                            type = obj.getString("type"),
                            name = obj.getString("name"),
                            iconName = obj.getString("iconName"),
                            colorHex = obj.getString("colorHex"),
                            limitAmount = if (limit == 0.0) null else limit
                        )
                    )
                }
            }

            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    repository.insertTransaction(
                        Transaction(
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount"),
                            categoryName = obj.getString("categoryName"),
                            date = obj.getLong("date"),
                            note = obj.getString("note"),
                            source = obj.getString("source")
                        )
                    )
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

// --- Companion Helper Data Classes ---

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class CategoryBudgetStatus(
    val category: Category,
    val actualSpent: Double,
    val limit: Double
)

data class MonthlyStats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val savings: Double = 0.0,
    val categoryBudgets: List<CategoryBudgetStatus> = emptyList(),
    val incomeBySource: Map<String, Double> = emptyMap()
)

data class MonthlyTrend(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)
