package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Category
import com.example.data.Transaction
import com.example.ui.MonthlyStats
import com.example.ui.BudgetViewModel
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.MultiMonthTrendChart
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBudgetScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf("dashboard") }

    // Dialog trigger states
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "Budget Tracker",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerCloudSyncSimulation() },
                        modifier = Modifier.testTag("sync_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync Now",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == "dashboard",
                    onClick = { currentTab = "dashboard" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Overview") }
                )
                NavigationBarItem(
                    selected = currentTab == "transactions",
                    onClick = { currentTab = "transactions" },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                    label = { Text("Activity") }
                )
                NavigationBarItem(
                    selected = currentTab == "categories",
                    onClick = { currentTab = "categories" },
                    icon = { Icon(Icons.Default.Category, contentDescription = "Categories") },
                    label = { Text("Categories") }
                )
                NavigationBarItem(
                    selected = currentTab == "ai_coach",
                    onClick = { currentTab = "ai_coach" },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = "AI Coach") },
                    label = { Text("AI Coach") }
                )
                NavigationBarItem(
                    selected = currentTab == "toolbox",
                    onClick = { currentTab = "toolbox" },
                    icon = { Icon(Icons.Default.FolderZip, contentDescription = "Backup & PDF") },
                    label = { Text("Backup") }
                )
            }
        },
        floatingActionButton = {
            if (currentTab == "dashboard" || currentTab == "transactions") {
                FloatingActionButton(
                    onClick = { showAddTransactionDialog = true },
                    modifier = Modifier.testTag("add_transaction_fab_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            } else if (currentTab == "categories") {
                FloatingActionButton(
                    onClick = { showAddCategoryDialog = true },
                    modifier = Modifier.testTag("add_category_fab_button")
                ) {
                    Icon(Icons.Default.LibraryAdd, contentDescription = "Add Category")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen contents
            when (currentTab) {
                "dashboard" -> DashboardView(viewModel)
                "transactions" -> TransactionsView(viewModel)
                "categories" -> CategoriesView(viewModel)
                "ai_coach" -> AiCoachView(viewModel)
                "toolbox" -> ToolboxView(viewModel)
            }

            // Global Sync Banner
            val syncingState by viewModel.isSyncing.collectAsStateWithLifecycle()
            val syncMsg by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
            
            AnimatedVisibility(
                visible = syncingState && syncMsg != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = syncMsg ?: "Syncing database cloud copies safely...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            viewModel = viewModel,
            onDismiss = { showAddTransactionDialog = false }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            viewModel = viewModel,
            onDismiss = { showAddCategoryDialog = false }
        )
    }
}

// --- sub views ---

@Composable
fun DashboardView(viewModel: BudgetViewModel) {
    val stats by viewModel.dynamicStats.collectAsStateWithLifecycle()
    val trends by viewModel.trendStats.collectAsStateWithLifecycle()
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()
    val year by viewModel.selectedYear.collectAsStateWithLifecycle()
    val monthVal by viewModel.selectedMonthValue.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_view_pane")
    ) {
        // Month navigation banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val nextMonth = if (monthVal == 0) 11 else monthVal - 1
                            val nextYear = if (monthVal == 0) year - 1 else year
                            viewModel.changeMonth(nextYear, nextMonth)
                        },
                        modifier = Modifier.testTag("prev_month_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month")
                    }

                    Text(
                        text = viewModel.getMonthName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = {
                            val nextMonth = if (monthVal == 11) 0 else monthVal + 1
                            val nextYear = if (monthVal == 11) year + 1 else year
                            viewModel.changeMonth(nextYear, nextMonth)
                        },
                        modifier = Modifier.testTag("next_month_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
            }
        }

        // Summary Statistics Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Income Log", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(
                            String.format(Locale.getDefault(), "$%.2f", stats.totalIncome),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Expenses Spend", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            String.format(Locale.getDefault(), "$%.2f", stats.totalExpense),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }
        }

        item {
            val netSavings = stats.savings
            val savingsColor = if (netSavings >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Net Monthly Savings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            text = String.format(Locale.getDefault(), "$%.2f", netSavings),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = savingsColor
                        )
                    }
                    Icon(
                        imageVector = if (netSavings >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = "Trend",
                        tint = savingsColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Canvas Donut Chart for Expense distribution
        item {
            Text(
                "Expense Distribution",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val expenseTotals = mutableMapOf<String, Double>()
                val expenseColors = mutableMapOf<String, String>()
                
                stats.categoryBudgets.forEach { item ->
                    if (item.actualSpent > 0.0) {
                        expenseTotals[item.category.name] = item.actualSpent
                        expenseColors[item.category.name] = item.category.colorHex
                    }
                }

                CategoryDonutChart(expenseTotals, expenseColors)
            }
        }

        // Categorized Income breakdown (multiple sources)
        item {
            Text(
                "Income Sources",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (stats.incomeBySource.isEmpty()) {
                        Text(
                            "No income streams logged in this month.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        stats.incomeBySource.forEach { (source, amt) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = "Income Coin",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(source, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    String.format(Locale.getDefault(), "$%.2f", amt),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Budget progress & Limits
        item {
            Text(
                "Budget vs Custom Limits",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        val expenseBudgets = stats.categoryBudgets
        if (expenseBudgets.isEmpty()) {
            item {
                Text(
                    "Assign custom limits by creating an Expense category.",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            items(expenseBudgets) { item ->
                val category = item.category
                val spent = item.actualSpent
                val limit = item.limit
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(category.colorHex))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconVector(category.iconName),
                                        contentDescription = category.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(category.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = String.format(Locale.getDefault(), "$%.1f spent", spent),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (limit > 0.0) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "$%.0f Max", limit),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    val percent = (spent / limit).coerceIn(0.0, 1.0).toFloat()
                                    val colorRatio = if (spent > limit) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                                    
                                    Box(modifier = Modifier.width(100.dp)) {
                                        LinearProgressIndicator(
                                            progress = { percent },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.6.dp) // adjusted for padding height spacing
                                                .clip(RoundedCornerShape(3.dp))
                                                .padding(top = 4.dp),
                                            color = colorRatio
                                        )
                                    }
                                } else {
                                    Text("Uncapped limit", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Multi month visual Canvas trends
        item {
            Text(
                "Savings & Income Trends",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                MultiMonthTrendChart(trends)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun TransactionsView(viewModel: BudgetViewModel) {
    val txList by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("transactions_list_pane")
    ) {
        Text(
            text = "Activity Log (${viewModel.getMonthName()})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        if (txList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = "Oasis Empty",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No expenses or income streams recorded here.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(txList) { tx ->
                    val colorSide = if (tx.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Indicator dot colored by type
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(colorSide)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(tx.categoryName, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${tx.note} • via ${tx.source}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(tx.date))
                                    Text(
                                        text = formattedDate,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(Locale.getDefault(), (if (tx.type == "INCOME") "+" else "-") + "$%.2f", tx.amount),
                                    fontWeight = FontWeight.Bold,
                                    color = colorSide,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteTransaction(tx) },
                                    modifier = Modifier.testTag("delete_tx_${tx.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}@Composable
fun CategoriesView(viewModel: BudgetViewModel) {
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()
    var editCategoryLimitSelect by remember { mutableStateOf<Category?>(null) }
    var inputLimitVal by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("categories_management_pane")
    ) {
        Text(
            text = "Categories Customization",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            item {
                Text(
                    text = "EXPENSE CATEGORIES",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            val expenseList = categoriesList.filter { it.type == "EXPENSE" }
            items(expenseList) { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(cat.colorHex))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconVector(cat.iconName),
                                    contentDescription = cat.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(cat.name, fontWeight = FontWeight.Bold)
                                if (cat.limitAmount != null) {
                                    Text(
                                        "Monthly Limit: $${cat.limitAmount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text("Unlimited Budget", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }

                        Row {
                            IconButton(onClick = {
                                editCategoryLimitSelect = cat
                                inputLimitVal = cat.limitAmount?.toString() ?: ""
                            }) {
                                Icon(Icons.Default.EditCalendar, contentDescription = "Edit Limit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "INCOME CATEGORIES",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            val incomeList = categoriesList.filter { it.type == "INCOME" }
            items(incomeList) { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(cat.colorHex))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconVector(cat.iconName),
                                    contentDescription = cat.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cat.name, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Limit Editor Dialog
    if (editCategoryLimitSelect != null) {
        val targetCat = editCategoryLimitSelect!!
        AlertDialog(
            onDismissRequest = { editCategoryLimitSelect = null },
            title = { Text("Configure Budget Limit") },
            text = {
                Column {
                    Text("Category: ${targetCat.name}", style = MaterialTheme.typography.titleSmall)
                    Text("Enter a custom budget ceiling for this expense category to track monthly status.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = inputLimitVal,
                        onValueChange = { inputLimitVal = it },
                        label = { Text("Limit Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val limitNum = inputLimitVal.toDoubleOrNull()
                    viewModel.updateCategoryLimit(targetCat, limitNum)
                    editCategoryLimitSelect = null
                }) {
                    Text("Save Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { editCategoryLimitSelect = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AiCoachView(viewModel: BudgetViewModel) {
    val chatHistory by viewModel.aiChatHistory.collectAsStateWithLifecycle()
    val loadingState by viewModel.aiAdvisorLoading.collectAsStateWithLifecycle()
    var rawInputQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_advisor_pane")
    ) {
        // AI Advisor Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Head",
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "AI Financial Advisor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "High Thinking Reasoning Coach",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                IconButton(onClick = { viewModel.clearAdvisorChat() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Clear Conversation")
                }
            }
        }

        // Bubble Chat history
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatHistory) { msg ->
                val bubbleBg = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                val alignSide = if (msg.isUser) Alignment.End else Alignment.Start
                
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignSide) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (msg.isUser) 16.dp else 0.dp,
                                    bottomEnd = if (msg.isUser) 0.dp else 16.dp
                                )
                            )
                            .background(bubbleBg)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                    Text(
                        text = if (msg.isUser) "You" else "Advisor (Gemini 3.1 Pro Thinking)",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp)
                    )
                }
            }

            if (loadingState) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI is organizing and thinking with HIGH reasoning precision...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Input controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = rawInputQuery,
                onValueChange = { rawInputQuery = it },
                placeholder = { Text("Ask about my categories, overspending risk...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_advisor_terminal_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.askFinancialAdvisor(rawInputQuery)
                    rawInputQuery = ""
                },
                enabled = !loadingState && rawInputQuery.isNotBlank(),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Submit Request",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun ToolboxView(viewModel: BudgetViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var lastImportJson by remember { mutableStateOf("") }
    val lastSyncedTimeVal by viewModel.lastSyncedTime.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("toolbox_view_pane")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Reports & Backup Cloud Sync", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Export monthly reports or perform offline JSON data backups & recovery.", fontSize = 12.sp, color = Color.Gray)
        }

        // Section 1: PDF Export
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly HTML/PDF Ledger", fontWeight = FontWeight.Bold)
                    Text("Generates structured document accounting categories limit margins, totals, and actual ledgers.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val file = viewModel.exportPdfReport(context)
                                if (file != null && file.exists()) {
                                    Toast.makeText(context, "PDF Report compiled successfully!", Toast.LENGTH_SHORT).show()
                                    sharePdfFile(context, file)
                                } else {
                                    Toast.makeText(context, "Error generating report. Check data.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("generate_pdf_report_button")
                    ) {
                        Icon(Icons.Default.DownloadForOffline, contentDescription = "Export PDF")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export PDF Monthly Report")
                    }
                }
            }
        }

        // Section 2: Real-time Cloud Synchronization Simulator
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cross-Device Sync Simulation", fontWeight = FontWeight.Bold)
                    Text(
                        "Checks secure connection endpoints, hashes categorizations, and synchronization nodes.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Status: $lastSyncedTimeVal",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.triggerCloudSyncSimulation() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SyncLock, contentDescription = "Simulate cloud node sync")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify & Run Cloud Sync")
                    }
                }
            }
        }

        // Section 3: Manual JSON backup restore
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Offline JSON Clipboard Backup", fontWeight = FontWeight.Bold)
                    Text("Copy or paste actual category configurations and transaction history across devices.", fontSize = 11.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val json = viewModel.getBackupJson()
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Backup JSON copy")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Ledger Database Backup")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = lastImportJson,
                        onValueChange = { lastImportJson = it },
                        label = { Text("Paste Backup JSON here") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                if (lastImportJson.isNotBlank()) {
                                    val ok = viewModel.importBackupJson(lastImportJson)
                                    if (ok) {
                                        Toast.makeText(context, "Data successfully restored!", Toast.LENGTH_LONG).show()
                                        lastImportJson = ""
                                    } else {
                                        Toast.makeText(context, "Invalid backup encoding. Try again.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Restore paste Database")
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- Dialog Creators ---

@Composable
fun AddTransactionDialog(
    viewModel: BudgetViewModel,
    onDismiss: () -> Unit
) {
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()
    
    var transactionType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedCategoryName by remember { mutableStateOf("") }
    var sourceText by remember { mutableStateOf("Primary Account") } // default source

    val filteredCats = categoriesList.filter { it.type == transactionType }
    
    // Automatically select first category of filtered if current selectedCategoryName is empty
    LaunchedEffect(transactionType, filteredCats) {
        if (filteredCats.isNotEmpty()) {
            selectedCategoryName = filteredCats.first().name
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Insert Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Type Toggle (Expense or Income)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { transactionType = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (transactionType == "EXPENSE") Color(0xFFC62828) else Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Expense")
                    }

                    Button(
                        onClick = { transactionType = "INCOME" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (transactionType == "INCOME") Color(0xFF2E7D32) else Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Income")
                    }
                }

                // Amount Textfield
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input")
                )

                // Custom category dropdown description or item selector (Simulated Selector)
                Text("Category Selection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (filteredCats.isEmpty()) {
                    Text("Please add a category first inside the Categories tab.", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LazyColumn(modifier = Modifier.height(70.dp).fillMaxWidth()) {
                            items(filteredCats) { cat ->
                                val isSelected = selectedCategoryName == cat.name
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable { selectedCategoryName = cat.name },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(
                                        text = cat.name,
                                        modifier = Modifier.padding(8.dp),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Income Source or Payment method
                OutlinedTextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    label = { Text(if (transactionType == "INCOME") "Income Source (e.g. Salary, Upwork)" else "Payment Source (e.g. Card, Cash)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Note description
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note description") },
                    modifier = Modifier.fillMaxWidth().testTag("transaction_note_input")
                )

                // Bottom Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && selectedCategoryName.isNotEmpty()) {
                                viewModel.insertTransaction(
                                    type = transactionType,
                                    amount = amt,
                                    categoryName = selectedCategoryName,
                                    date = System.currentTimeMillis(),
                                    note = noteText.ifEmpty { "Transaction Entry" },
                                    source = sourceText
                                )
                                onDismiss()
                            }
                        },
                        enabled = amountText.isNotBlank() && selectedCategoryName.isNotEmpty()
                    ) {
                        Text("Add Ledger Transaction")
                    }
                }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    viewModel: BudgetViewModel,
    onDismiss: () -> Unit
) {
    var categoryType by remember { mutableStateOf("EXPENSE") }
    var nameText by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("Restaurant") }
    var selectedHex by remember { mutableStateOf("#FF5722") }
    var limitText by remember { mutableStateOf("") }

    val icons = listOf("Restaurant", "Home", "Lightbulb", "DirectionsCar", "Movie", "ShoppingBag", "Payments", "Work", "TrendingUp")
    val colors = listOf("#FF5722", "#3F51B5", "#F44336", "#009688", "#E91E63", "#9C27B0", "#4CAF50", "#FFEB3B", "#607D8B")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Insert Custom Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { categoryType = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (categoryType == "EXPENSE") Color(0xFFC62828) else Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Expense")
                    }

                    Button(
                        onClick = { categoryType = "INCOME" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (categoryType == "INCOME") Color(0xFF2E7D32) else Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Income")
                    }
                }

                // Name field
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth().testTag("category_name_input")
                )

                // Optional budget limit for Expenses
                if (categoryType == "EXPENSE") {
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Monthly Budget Limit ($ - Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Icon list (Interactive Selector)
                Text("Select Vector Icon", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyColumn(modifier = Modifier.height(60.dp).fillMaxWidth()) {
                        items(icons.chunked(4)) { chunk ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                chunk.forEach { item ->
                                    val isSel = selectedIconName == item
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(2.dp)
                                            .clickable { selectedIconName = item },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(getIconVector(item), contentDescription = item, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(item, fontSize = 9.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Theme Colors list (Interactive grid selection)
                Text("Select Theme Color", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    colors.forEach { hexColor ->
                        val isColorSelected = selectedHex == hexColor
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hexColor)))
                                .clickable { selectedHex = hexColor }
                                .padding(2.dp)
                        ) {
                            if (isColorSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            if (nameText.isNotBlank()) {
                                val limitNum = limitText.toDoubleOrNull()
                                viewModel.insertCategory(
                                    type = categoryType,
                                    name = nameText,
                                    iconName = selectedIconName,
                                    colorHex = selectedHex,
                                    limitAmount = if (categoryType == "EXPENSE") limitNum else null
                                )
                                onDismiss()
                            }
                        },
                        enabled = nameText.isNotBlank()
                    ) {
                        Text("Create Category")
                    }
                }
            }
        }
    }
}

// --- helpers ---

fun getIconVector(iconName: String): ImageVector {
    return when (iconName) {
        "Restaurant" -> Icons.Default.Restaurant
        "Home" -> Icons.Default.Home
        "Lightbulb" -> Icons.Default.Lightbulb
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "Movie" -> Icons.Default.Movie
        "ShoppingBag" -> Icons.Default.ShoppingBag
        "Payments" -> Icons.Default.Payments
        "Work" -> Icons.Default.Work
        "LaptopMac" -> Icons.Default.LaptopMac
        "TrendingUp" -> Icons.Default.TrendingUp
        "Store" -> Icons.Default.Store
        "CardGiftcard" -> Icons.Default.CardGiftcard
        "Leisure" -> Icons.Default.SportsEsports
        else -> Icons.Default.Category
    }
}

private fun sharePdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Monthly PDF Ledger"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failure matching secure transport: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
