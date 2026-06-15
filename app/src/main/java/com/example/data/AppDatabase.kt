package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(entities = [Category::class, Transaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val budgetDao: BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_tracker_db"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.budgetDao)
                }
            }
        }

        private suspend fun populateInitialData(budgetDao: BudgetDao) {
            // Default Expense Categories
            val expenseCategories = listOf(
                Category(name = "Food & Dining", type = "EXPENSE", iconName = "Restaurant", colorHex = "#FF5722", limitAmount = 500.0),
                Category(name = "Rent & Housing", type = "EXPENSE", iconName = "Home", colorHex = "#3F51B5", limitAmount = 1500.0),
                Category(name = "Utilities", type = "EXPENSE", iconName = "Lightbulb", colorHex = "#FFEB3B", limitAmount = 250.0),
                Category(name = "Transportation", type = "EXPENSE", iconName = "DirectionsCar", colorHex = "#009688", limitAmount = 300.0),
                Category(name = "Entertainment", type = "EXPENSE", iconName = "Movie", colorHex = "#E91E63", limitAmount = 200.0),
                Category(name = "Shopping", type = "EXPENSE", iconName = "ShoppingBag", colorHex = "#9C27B0", limitAmount = 400.0),
                Category(name = "Healthcare", type = "EXPENSE", iconName = "MedicalServices", colorHex = "#4CAF50", limitAmount = 150.0)
            )

            // Default Income Categories
            val incomeCategories = listOf(
                Category(name = "Salary", type = "INCOME", iconName = "Work", colorHex = "#2E7D32"),
                Category(name = "Freelance", type = "INCOME", iconName = "LaptopMac", colorHex = "#1565C0"),
                Category(name = "Investments", type = "INCOME", iconName = "TrendingUp", colorHex = "#EF6C00"),
                Category(name = "Side Hustle", type = "INCOME", iconName = "Store", colorHex = "#00838F"),
                Category(name = "Gifts", type = "INCOME", iconName = "CardGiftcard", colorHex = "#AD1457")
            )

            expenseCategories.forEach { budgetDao.insertCategory(it) }
            incomeCategories.forEach { budgetDao.insertCategory(it) }

            // Add some initial transactions for visual charts
            val now = Calendar.getInstance()
            
            // Current Month Transactions
            budgetDao.insertTransaction(Transaction(
                type = "INCOME", amount = 3200.0, categoryName = "Salary",
                date = now.timeInMillis - 20 * 24 * 60 * 60 * 1000L, note = "Monthly base pay", source = "Primary Employer"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "INCOME", amount = 450.0, categoryName = "Freelance",
                date = now.timeInMillis - 12 * 24 * 60 * 60 * 1000L, note = "Landing Page Design", source = "Upwork"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "INCOME", amount = 120.0, categoryName = "Investments",
                date = now.timeInMillis - 5 * 24 * 60 * 60 * 1000L, note = "Dividend Payout", source = "Robinhood"
            ))

            budgetDao.insertTransaction(Transaction(
                type = "EXPENSE", amount = 1200.0, categoryName = "Rent & Housing",
                date = now.timeInMillis - 18 * 24 * 60 * 60 * 1000L, note = "Rent Payment", source = "Bank Account"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "EXPENSE", amount = 145.50, categoryName = "Food & Dining",
                date = now.timeInMillis - 15 * 24 * 60 * 60 * 1000L, note = "Weekly Groceries", source = "Credit Card"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "EXPENSE", amount = 34.20, categoryName = "Food & Dining",
                date = now.timeInMillis - 10 * 24 * 60 * 60 * 1000L, note = "Dinner with friends", source = "Cash"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "EXPENSE", amount = 85.00, categoryName = "Utilities",
                date = now.timeInMillis - 14 * 24 * 60 * 60 * 1000L, note = "Electric Bill", source = "Bank Account"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "EXPENSE", amount = 45.00, categoryName = "Transportation",
                date = now.timeInMillis - 8 * 24 * 60 * 60 * 1000L, note = "Gasoline Fill Up", source = "Credit Card"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "EXPENSE", amount = 65.00, categoryName = "Entertainment",
                date = now.timeInMillis - 4 * 24 * 60 * 60 * 1000L, note = "Concert Ticket", source = "Credit Card"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "EXPENSE", amount = 110.00, categoryName = "Shopping",
                date = now.timeInMillis - 2 * 24 * 60 * 60 * 1000L, note = "Running Shoes", source = "Credit Card"
            ))

            // Last Month Transactions (helps show trends!)
            val lastMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            budgetDao.insertTransaction(Transaction(
                type = "INCOME", amount = 3200.0, categoryName = "Salary",
                date = lastMonth.timeInMillis - 15 * 24 * 60 * 60 * 1000L, note = "Last month pay", source = "Primary Employer"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "INCOME", amount = 300.0, categoryName = "Freelance",
                date = lastMonth.timeInMillis - 8 * 24 * 60 * 60 * 1000L, note = "App Consulting", source = "Contract"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "EXPENSE", amount = 1200.0, categoryName = "Rent & Housing",
                date = lastMonth.timeInMillis - 15 * 24 * 60 * 60 * 1000L, note = "Last month rent", source = "Bank Account"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "EXPENSE", amount = 195.00, categoryName = "Food & Dining",
                date = lastMonth.timeInMillis - 10 * 24 * 60 * 60 * 1000L, note = "Restaurant & Grocery", source = "Credit Card"
            ))
            budgetDao.insertTransaction(Transaction(
                type = "EXPENSE", amount = 180.00, categoryName = "Shopping",
                date = lastMonth.timeInMillis - 5 * 24 * 60 * 60 * 1000L, note = "Electronics", source = "Credit Card"
            ))
        }
    }
}
