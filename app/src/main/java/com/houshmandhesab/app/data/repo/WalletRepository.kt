package com.houshmandhesab.app.data.repo

import android.content.Context
import com.houshmandhesab.app.data.db.Account
import com.houshmandhesab.app.data.db.AppDatabase
import com.houshmandhesab.app.data.db.Budget
import com.houshmandhesab.app.data.db.BudgetProgress
import com.houshmandhesab.app.data.db.Category
import com.houshmandhesab.app.data.db.CategoryTotal
import com.houshmandhesab.app.data.db.DailyTotal
import com.houshmandhesab.app.data.db.Debt
import com.houshmandhesab.app.data.db.GoalDao
import com.houshmandhesab.app.data.db.MonthTotals
import com.houshmandhesab.app.data.db.RecurringTransaction
import com.houshmandhesab.app.data.db.SavingsGoal
import com.houshmandhesab.app.data.db.TransactionDao
import com.houshmandhesab.app.data.db.TransactionEntity
import com.houshmandhesab.app.data.db.TxWithCategory
import com.houshmandhesab.app.data.prefs.SettingsRepository
import com.houshmandhesab.app.util.Jalali
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val db: AppDatabase,
    val settings: SettingsRepository,
    @ApplicationContext private val context: Context
) {
    val accountDao = db.accountDao()
    val categoryDao = db.categoryDao()
    val transactionDao: TransactionDao = db.transactionDao()
    val budgetDao = db.budgetDao()
    val debtDao = db.debtDao()
    val goalDao: GoalDao = db.goalDao()
    val recurringDao = db.recurringDao()

    fun accounts(): Flow<List<Account>> = accountDao.all()
    fun categories(type: String? = null): Flow<List<Category>> =
        if (type == null) categoryDao.all() else categoryDao.byType(type)

    fun balances(): Flow<Map<Long, Long>> =
        accountDao.rawBalances().combine(accounts()) { raw, accs ->
            val map = raw.associate { it.accountId to it.balance }
            accs.associate { it.id to (it.initialBalance + (map[it.id] ?: 0L)) }
        }

    fun totalBalance(): Flow<Long> = balances().map { it.values.sum() }

    fun recentTransactions(limit: Int = 10): Flow<List<TxWithCategory>> = transactionDao.recent(limit)

    fun transactionsRange(from: Long, to: Long): Flow<List<TxWithCategory>> = transactionDao.range(from, to)

    fun transactionsRangeByType(from: Long, to: Long, type: String): Flow<List<TxWithCategory>> =
        transactionDao.rangeByType(from, to, type)

    fun monthTotals(from: Long, to: Long): Flow<MonthTotals?> = transactionDao.monthTotals(from, to)

    fun dailyTotals(from: Long, to: Long): Flow<List<DailyTotal>> = transactionDao.dailyTotals(from, to)

    fun categoryTotals(type: String, from: Long, to: Long): Flow<List<CategoryTotal>> =
        transactionDao.categoryTotals(type, from, to)

    fun budgetProgress(from: Long, to: Long): Flow<List<BudgetProgress>> = budgetDao.progress(from, to)

    fun goals(): Flow<List<SavingsGoal>> = goalDao.all()
    fun debts(): Flow<List<Debt>> = debtDao.all()
    fun recurring(): Flow<List<RecurringTransaction>> = recurringDao.all()

    suspend fun transaction(id: Long): TransactionEntity? = transactionDao.byId(id)

    suspend fun saveTransaction(tx: TransactionEntity): Long = transactionDao.upsert(tx)

    suspend fun deleteTransaction(id: Long) = transactionDao.delete(id)

    suspend fun saveAccount(account: Account): Long = accountDao.upsert(account)

    suspend fun deleteAccount(id: Long) = accountDao.delete(id)

    suspend fun saveCategory(category: Category): Long = categoryDao.upsert(category)

    suspend fun deleteCategory(id: Long) = categoryDao.delete(id)

    suspend fun saveBudget(budget: Budget): Long = budgetDao.upsert(budget)

    suspend fun deleteBudget(id: Long) = budgetDao.delete(id)

    suspend fun saveGoal(goal: SavingsGoal): Long = goalDao.upsert(goal)

    suspend fun deleteGoal(id: Long) = goalDao.delete(id)

    suspend fun addToGoal(id: Long, amount: Long) = goalDao.addToGoal(id, amount)

    suspend fun saveDebt(debt: Debt): Long = debtDao.upsert(debt)

    suspend fun deleteDebt(id: Long) = debtDao.delete(id)

    suspend fun settleDebt(id: Long) = debtDao.settle(id)

    suspend fun saveRecurring(r: RecurringTransaction): Long = recurringDao.upsert(r)

    suspend fun deleteRecurring(id: Long) = recurringDao.delete(id)

    suspend fun monthRange(jy: Int, jm: Int): Pair<Long, Long> =
        Jalali.startOfMonthMillis(jy, jm) to Jalali.endOfMonthMillis(jy, jm)

    fun last6MonthsTotals(): Flow<List<MonthTotals>> {
        val today = Jalali.today()
        return kotlinx.coroutines.flow.flow {
            val out = mutableListOf<MonthTotals>()
            var jm = today.jm
            var jy = today.jy
            repeat(6) {
                val (from, to) = monthRange(jy, jm)
                val totals = monthTotals(from, to).firstOrNull() ?: MonthTotals(0, 0)
                out.add(0, totals)
                if (jm == 1) { jm = 12; jy -= 1 } else jm -= 1
            }
            emit(out)
        }
    }

    fun currentMonth(): Pair<Int, Int> {
        val today = Jalali.today()
        return today.jy to today.jm
    }

    suspend fun processRecurring(now: Long = System.currentTimeMillis()): Int {
        val due = recurringDao.due(now)
        for (r in due) {
            transactionDao.upsert(
                TransactionEntity(
                    type = r.type,
                    amount = r.amount,
                    accountId = r.accountId,
                    toAccountId = null,
                    categoryId = r.categoryId,
                    date = r.nextDate,
                    note = r.title
                )
            )
            val next = when (r.interval) {
                "DAILY" -> LocalDate.ofInstant(java.time.Instant.ofEpochMilli(r.nextDate), ZoneId.systemDefault()).plusDays(1)
                "WEEKLY" -> LocalDate.ofInstant(java.time.Instant.ofEpochMilli(r.nextDate), ZoneId.systemDefault()).plusWeeks(1)
                else -> LocalDate.ofInstant(java.time.Instant.ofEpochMilli(r.nextDate), ZoneId.systemDefault()).plusMonths(1)
            }
            recurringDao.setNextDate(r.id, next.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        }
        return due.size
    }

    suspend fun seedIfFirstRun() {
        if (!settings.isFirstRun()) return
        val cash = accountDao.upsert(Account(name = "نقد", type = "CASH", color = 0xFF34D399, icon = "payments"))
        accountDao.upsert(Account(name = "بانک", type = "BANK", color = 0xFF60A5FA, icon = "account_balance"))
        val defaults = listOf(
            Category(name = "خوراک", type = "EXPENSE", icon = "restaurant", color = 0xFFF59E0B),
            Category(name = "حمل‌ونقل", type = "EXPENSE", icon = "directions_car", color = 0xFF60A5FA),
            Category(name = "خرید", type = "EXPENSE", icon = "shopping_bag", color = 0xFFF472B6),
            Category(name = "قبض‌ها", type = "EXPENSE", icon = "receipt_long", color = 0xFF94A3B8),
            Category(name = "تفریح", type = "EXPENSE", icon = "movie", color = 0xFFA78BFA),
            Category(name = "سلامت", type = "EXPENSE", icon = "favorite", color = 0xFFF87171),
            Category(name = "خانه", type = "EXPENSE", icon = "home", color = 0xFF34D399),
            Category(name = "آموزش", type = "EXPENSE", icon = "school", color = 0xFF38BDF8),
            Category(name = "قهوه", type = "EXPENSE", icon = "local_cafe", color = 0xFFD97706),
            Category(name = "هدیه", type = "EXPENSE", icon = "card_giftcard", color = 0xFFFB7185),
            Category(name = "حقوق", type = "INCOME", icon = "payments", color = 0xFF34D399),
            Category(name = "فریلنس", type = "INCOME", icon = "work", color = 0xFF60A5FA),
            Category(name = "هدیه دریافتی", type = "INCOME", icon = "card_giftcard", color = 0xFFF472B6),
            Category(name = "سود سرمایه‌گذاری", type = "INCOME", icon = "trending_up", color = 0xFFA78BFA),
            Category(name = "فروش", type = "INCOME", icon = "storefront", color = 0xFF38BDF8)
        )
        defaults.forEach { categoryDao.upsert(it) }
        settings.setFirstRunDone()
    }

    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "houshmand-hesab")
        root.put("version", 1)
        root.put("accounts", accountsJson())
        root.put("categories", categoriesJson())
        root.put("transactions", transactionsJson())
        root.put("budgets", JSONArray().apply { budgetDao.allOnce().forEach { put(JSONObject().apply { put("id", it.id); put("categoryId", it.categoryId); put("limitAmount", it.limitAmount); put("period", it.period) }) } })
        root.put("debts", JSONArray().apply { debtDao.all().firstOrNull()?.forEach { d -> put(JSONObject().apply { put("id", d.id); put("person", d.person); put("amount", d.amount); put("owedToMe", d.owedToMe); put("dueDate", d.dueDate ?: JSONObject.NULL); put("note", d.note); put("settled", d.settled); put("installmentsTotal", d.installmentsTotal); put("installmentsPaid", d.installmentsPaid) }) } })
        root.put("goals", JSONArray().apply { goalDao.all().firstOrNull()?.forEach { g -> put(JSONObject().apply { put("id", g.id); put("title", g.title); put("targetAmount", g.targetAmount); put("savedAmount", g.savedAmount); put("deadline", g.deadline ?: JSONObject.NULL); put("color", g.color) }) } })
        root.put("recurring", JSONArray().apply { recurringDao.all().firstOrNull()?.forEach { r -> put(JSONObject().apply { put("id", r.id); put("title", r.title); put("type", r.type); put("amount", r.amount); put("accountId", r.accountId); put("categoryId", r.categoryId ?: JSONObject.NULL); put("interval", r.interval); put("nextDate", r.nextDate); put("active", r.active) }) } })
        return root.toString(2)
    }

    private suspend fun accountsJson() = JSONArray().apply {
        accountDao.all().firstOrNull()?.forEach { a ->
            put(JSONObject().apply {
                put("id", a.id); put("name", a.name); put("type", a.type)
                put("color", a.color); put("initialBalance", a.initialBalance); put("icon", a.icon)
            })
        }
    }

    private suspend fun categoriesJson() = JSONArray().apply {
        categoryDao.allOnce().forEach { c ->
            put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("type", c.type)
                put("icon", c.icon); put("color", c.color)
            })
        }
    }

    private suspend fun transactionsJson() = JSONArray().apply {
        transactionDao.allOnce().forEach { t ->
            put(JSONObject().apply {
                put("id", t.id); put("type", t.type); put("amount", t.amount)
                put("accountId", t.accountId); put("toAccountId", t.toAccountId ?: JSONObject.NULL)
                put("categoryId", t.categoryId ?: JSONObject.NULL); put("date", t.date)
                put("note", t.note); put("attachmentPath", t.attachmentPath ?: JSONObject.NULL)
            })
        }
    }

    suspend fun importJson(text: String): Boolean {
        return try {
            val root = JSONObject(text)
            if (root.optString("app") != "houshmand-hesab") return false
            transactionDao.clear(); budgetDao.clear(); debtDao.clear()
            goalDao.clear(); recurringDao.clear(); accountDao.clear(); categoryDao.clear()
            root.optJSONArray("accounts")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    accountDao.upsert(
                        Account(
                            id = o.getLong("id"), name = o.getString("name"), type = o.optString("type", "CASH"),
                            color = o.optLong("color", 0xFF34D399), initialBalance = o.optLong("initialBalance", 0),
                            icon = o.optString("icon", "wallet")
                        )
                    )
                }
            }
            root.optJSONArray("categories")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    categoryDao.upsert(
                        Category(
                            id = o.getLong("id"), name = o.getString("name"), type = o.optString("type", "EXPENSE"),
                            icon = o.optString("icon", "more_horiz"), color = o.optLong("color", 0xFF60A5FA)
                        )
                    )
                }
            }
            root.optJSONArray("transactions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    transactionDao.upsert(
                        TransactionEntity(
                            id = o.getLong("id"), type = o.getString("type"), amount = o.getLong("amount"),
                            accountId = o.getLong("accountId"),
                            toAccountId = if (o.isNull("toAccountId")) null else o.getLong("toAccountId"),
                            categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                            date = o.getLong("date"), note = o.optString("note", ""),
                            attachmentPath = if (o.isNull("attachmentPath")) null else o.optString("attachmentPath")
                        )
                    )
                }
            }
            root.optJSONArray("budgets")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    budgetDao.upsert(Budget(id = o.getLong("id"), categoryId = o.getLong("categoryId"), limitAmount = o.getLong("limitAmount"), period = o.optString("period", "MONTHLY")))
                }
            }
            root.optJSONArray("debts")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    debtDao.upsert(
                        Debt(
                            id = o.getLong("id"), person = o.getString("person"), amount = o.getLong("amount"),
                            owedToMe = o.getBoolean("owedToMe"),
                            dueDate = if (o.isNull("dueDate")) null else o.getLong("dueDate"),
                            note = o.optString("note", ""), settled = o.getBoolean("settled"),
                            installmentsTotal = o.optInt("installmentsTotal", 0),
                            installmentsPaid = o.optInt("installmentsPaid", 0)
                        )
                    )
                }
            }
            root.optJSONArray("goals")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    goalDao.upsert(
                        SavingsGoal(
                            id = o.getLong("id"), title = o.getString("title"), targetAmount = o.getLong("targetAmount"),
                            savedAmount = o.optLong("savedAmount", 0),
                            deadline = if (o.isNull("deadline")) null else o.getLong("deadline"),
                            color = o.optLong("color", 0xFF34D399)
                        )
                    )
                }
            }
            root.optJSONArray("recurring")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    recurringDao.upsert(
                        RecurringTransaction(
                            id = o.getLong("id"), title = o.getString("title"), type = o.getString("type"),
                            amount = o.getLong("amount"), accountId = o.getLong("accountId"),
                            categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                            interval = o.optString("interval", "MONTHLY"), nextDate = o.getLong("nextDate"),
                            active = o.optBoolean("active", true)
                        )
                    )
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exportCsv(): String {
        val sb = StringBuilder()
        sb.append("type,amount,account,category,date,note\n")
        val accs = accountDao.all().firstOrNull()?.associate { it.id to it.name } ?: emptyMap()
        val cats = categoryDao.allOnce().associate { it.id to it.name }
        transactionDao.allOnce().forEach { t ->
            val acc = accs[t.accountId] ?: ""
            val cat = t.categoryId?.let { cats[it] } ?: ""
            val note = if (t.note.contains(',') || t.note.contains('"')) "\"${t.note.replace("\"", "\"\"")}\"" else t.note
            val date = java.time.Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            sb.append("${t.type},${t.amount},$acc,$cat,$date,$note\n")
        }
        return sb.toString()
    }

    fun exportsDir(): File = File(context.filesDir, "exports").apply { mkdirs() }

    fun receiptsDir(): File = File(context.filesDir, "receipts").apply { mkdirs() }

    suspend fun clearAllData() {
        transactionDao.clear(); budgetDao.clear(); debtDao.clear()
        goalDao.clear(); recurringDao.clear(); accountDao.clear(); categoryDao.clear()
    }
}
