package com.houshmandhesab.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id")
    fun all(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun byId(id: Long): Account?

    @Upsert
    suspend fun upsert(account: Account): Long

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT acc AS accountId, COALESCE(SUM(v), 0) AS balance FROM (SELECT accountId AS acc, CASE WHEN type = 'INCOME' THEN amount ELSE -amount END AS v FROM transactions WHERE type IN ('INCOME', 'EXPENSE') UNION ALL SELECT toAccountId AS acc, amount AS v FROM transactions WHERE type = 'TRANSFER' UNION ALL SELECT accountId AS acc, -amount AS v FROM transactions WHERE type = 'TRANSFER') WHERE acc IS NOT NULL GROUP BY acc")
    fun rawBalances(): Flow<List<AccountBalance>>

    @Query("DELETE FROM accounts")
    suspend fun clear()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type, name")
    fun all(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name")
    fun byType(type: String): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY type, name")
    suspend fun allOnce(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun byId(id: Long): Category?

    @Upsert
    suspend fun upsert(category: Category): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM categories")
    suspend fun clear()
}

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<TxWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun range(from: Long, to: Long): Flow<List<TxWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to AND type = :type ORDER BY date DESC")
    fun rangeByType(from: Long, to: Long, type: String): Flow<List<TxWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    suspend fun rangeOnce(from: Long, to: Long): List<TxWithCategory>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun allOnce(): List<TransactionEntity>

    @Upsert
    suspend fun upsert(tx: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clear()

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount END), 0) AS income, COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount END), 0) AS expense FROM transactions WHERE date BETWEEN :from AND :to")
    fun monthTotals(from: Long, to: Long): Flow<MonthTotals?>

    @Query("SELECT date / 86400000 * 86400000 AS day, COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount END), 0) AS income, COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount END), 0) AS expense FROM transactions WHERE date BETWEEN :from AND :to GROUP BY day ORDER BY day")
    fun dailyTotals(from: Long, to: Long): Flow<List<DailyTotal>>

    @Query("SELECT c.id AS categoryId, c.name AS name, c.icon AS icon, c.color AS color, COALESCE(SUM(t.amount), 0) AS total FROM transactions t LEFT JOIN categories c ON c.id = t.categoryId WHERE t.type = :type AND t.date BETWEEN :from AND :to GROUP BY c.id ORDER BY total DESC")
    fun categoryTotals(type: String, from: Long, to: Long): Flow<List<CategoryTotal>>

    @Query("SELECT COUNT(*) FROM transactions")
    fun count(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :from AND :to ORDER BY amount DESC LIMIT 1")
    fun biggestExpense(from: Long, to: Long): Flow<TransactionEntity?>
}

@Dao
interface BudgetDao {
    @Query("SELECT b.id AS budgetId, b.categoryId AS categoryId, b.limitAmount AS limitAmount, c.name AS name, c.icon AS icon, c.color AS color, COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type = 'EXPENSE' AND t.categoryId = b.categoryId AND t.date BETWEEN :from AND :to), 0) AS spent FROM budgets b LEFT JOIN categories c ON c.id = b.categoryId ORDER BY b.id")
    fun progress(from: Long, to: Long): Flow<List<BudgetProgress>>

    @Query("SELECT * FROM budgets")
    suspend fun allOnce(): List<Budget>

    @Upsert
    suspend fun upsert(budget: Budget): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun clear()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY settled, CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate")
    fun all(): Flow<List<Debt>>

    @Upsert
    suspend fun upsert(debt: Debt): Long

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE debts SET settled = 1 WHERE id = :id")
    suspend fun settle(id: Long)

    @Query("SELECT * FROM debts WHERE settled = 0 AND dueDate IS NOT NULL AND dueDate BETWEEN :from AND :to")
    suspend fun dueBetween(from: Long, to: Long): List<Debt>

    @Query("DELETE FROM debts")
    suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun all(): Flow<List<SavingsGoal>>

    @Upsert
    suspend fun upsert(goal: SavingsGoal): Long

    @Query("UPDATE goals SET savedAmount = savedAmount + :amount WHERE id = :id")
    suspend fun addToGoal(id: Long, amount: Long)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM goals")
    suspend fun clear()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY nextDate")
    fun all(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring WHERE active = 1 AND nextDate <= :now")
    suspend fun due(now: Long): List<RecurringTransaction>

    @Upsert
    suspend fun upsert(r: RecurringTransaction): Long

    @Query("UPDATE recurring SET nextDate = :nextDate WHERE id = :id")
    suspend fun setNextDate(id: Long, nextDate: Long)

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM recurring")
    suspend fun clear()
}
