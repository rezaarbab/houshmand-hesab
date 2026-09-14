package com.houshmandhesab.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Account::class,
        Category::class,
        TransactionEntity::class,
        Budget::class,
        Debt::class,
        SavingsGoal::class,
        RecurringTransaction::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun debtDao(): DebtDao
    abstract fun goalDao(): GoalDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        const val NAME = "houshmand_hesab.db"
    }
}
