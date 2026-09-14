package com.houshmandhesab.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "CASH",
    val color: Long = 0xFF34D399,
    val initialBalance: Long = 0,
    val icon: String = "wallet"
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "EXPENSE",
    val icon: String = "more_horiz",
    val color: Long = 0xFF60A5FA
)

@Entity(
    tableName = "transactions",
    indices = [Index("accountId"), Index("categoryId"), Index("date"), Index("type")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String = "EXPENSE",
    val amount: Long = 0,
    val accountId: Long = 0,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val attachmentPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets", indices = [Index("categoryId")])
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val limitAmount: Long,
    val period: String = "MONTHLY"
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val amount: Long,
    val owedToMe: Boolean = false,
    val dueDate: Long? = null,
    val note: String = "",
    val settled: Boolean = false,
    val installmentsTotal: Int = 0,
    val installmentsPaid: Int = 0
)

@Entity(tableName = "goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Long,
    val savedAmount: Long = 0,
    val deadline: Long? = null,
    val color: Long = 0xFF34D399
)

@Entity(tableName = "recurring")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String = "EXPENSE",
    val amount: Long,
    val accountId: Long,
    val categoryId: Long? = null,
    val interval: String = "MONTHLY",
    val nextDate: Long = System.currentTimeMillis(),
    val active: Boolean = true
)

data class AccountBalance(val accountId: Long, val balance: Long)

data class TxWithCategory(
    @Embedded val tx: TransactionEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: Category?
)

data class MonthTotals(val income: Long, val expense: Long)

data class DailyTotal(val day: Long, val income: Long, val expense: Long)

data class CategoryTotal(
    val categoryId: Long?,
    val name: String?,
    val icon: String?,
    val color: Long?,
    val total: Long
)

data class BudgetProgress(
    val budgetId: Long,
    val categoryId: Long,
    val limitAmount: Long,
    val name: String?,
    val icon: String?,
    val color: Long?,
    val spent: Long
)
