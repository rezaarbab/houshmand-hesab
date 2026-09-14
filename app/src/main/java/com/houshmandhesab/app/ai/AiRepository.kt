package com.houshmandhesab.app.ai

import com.houshmandhesab.app.data.db.Category
import com.houshmandhesab.app.data.db.MonthTotals
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.util.Jalali
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val api: CloudflareApi,
    private val wallet: WalletRepository
) {
    val configured: Flow<Boolean> = combine(
        wallet.settings.cfAccountId,
        wallet.settings.cfToken
    ) { a, t -> a.isNotBlank() && t.isNotBlank() }

    private suspend fun credentials(): Triple<String, String, String>? {
        val accountId = wallet.settings.cfAccountId.first()
        val token = wallet.settings.cfToken.first()
        val model = wallet.settings.cfModel.first()
        if (accountId.isBlank() || token.isBlank()) return null
        return Triple(accountId, token, model)
    }

    private suspend fun runMessages(messages: List<ChatMessage>): Result<String> {
        val creds = credentials() ?: return Result.failure(IllegalStateException("AI is not configured"))
        return try {
            val resp = api.run(
                accountId = creds.first,
                model = creds.third,
                auth = "Bearer ${creds.second}",
                body = AiRunRequest(messages)
            )
            val text = resp.result?.response
            if (resp.success && !text.isNullOrBlank()) Result.success(text.trim())
            else Result.failure(IllegalStateException("AI error: ${resp.errors ?: "empty response"}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chat(history: List<ChatMessage>, userMessage: String): Result<String> {
        val system = SYSTEM_PROMPT + "\n\n" + financeContext()
        val messages = buildList {
            add(ChatMessage("system", system))
            addAll(history.takeLast(8).filter { it.role != "system" })
            add(ChatMessage("user", userMessage))
        }
        return runMessages(messages)
    }

    suspend fun suggestCategory(note: String, categories: List<Category>): Result<String> {
        if (categories.isEmpty()) return Result.failure(IllegalStateException("no categories"))
        val names = categories.joinToString(" | ") { it.name }
        val prompt = "Transaction note: \"" + note + "\"\n" +
            "Categories: " + names + "\n" +
            "Reply with ONLY the best matching category name from the list above. No extra words."
        return runMessages(
            listOf(
                ChatMessage("system", "You are a transaction classifier. Output only one category name from the provided list."),
                ChatMessage("user", prompt)
            )
        )
    }

    suspend fun financeContext(): String {
        val today = Jalali.today()
        val (from, to) = wallet.monthRange(today.jy, today.jm)
        val totals = wallet.monthTotals(from, to).first() ?: MonthTotals(0, 0)
        val topCats = wallet.categoryTotals("EXPENSE", from, to).first().take(6)
        val accounts = wallet.accounts().first()
        val balances = wallet.balances().first()
        val totalBalance = balances.values.sum()
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
        val recent = wallet.transactionDao.rangeOnce(weekAgo, System.currentTimeMillis() + 1).take(10)
        val catNames = wallet.categoryDao.allOnce().associate { it.id to it.name }
        val accNames = accounts.associate { it.id to it.name }

        return buildString {
            append("User financial data:\n")
            append("- Total balance: ").append(totalBalance).append("\n")
            append("- This month income: ").append(totals.income).append("\n")
            append("- This month expense: ").append(totals.expense).append("\n")
            if (topCats.isNotEmpty()) {
                append("- Top expense categories this month: ")
                append(topCats.joinToString(", ") { (it.name ?: "uncategorized") + " (" + it.total + ")" })
                append("\n")
            }
            if (accounts.isNotEmpty()) {
                append("- Accounts: ")
                append(accounts.joinToString(", ") { it.name + ": " + (balances[it.id] ?: 0L) })
                append("\n")
            }
            if (recent.isNotEmpty()) {
                append("- Recent transactions:\n")
                recent.forEach { tx ->
                    val cat = tx.tx.categoryId?.let { catNames[it] } ?: "uncategorized"
                    val acc = accNames[tx.tx.accountId] ?: ""
                    append("  * ").append(tx.tx.type).append(" / ").append(cat)
                        .append(" / ").append(tx.tx.amount).append(" / ").append(acc)
                        .append(" / ").append(tx.tx.note.ifBlank { "-" }).append("\n")
                }
            }
            append("- Today (Jalali): ").append(today).append("\n")
        }
    }

    companion object {
        val SYSTEM_PROMPT = """
            You are the "Houshmand Hesab" app's smart personal accountant, chatting with a Persian-speaking user.
            Rules:
            1. Always respond in simple, fluent Persian (Farsi). Never respond in English.
            2. Keep answers short, practical and step-by-step (max 8 lines).
            3. Analyze ONLY based on the real financial data provided in this message; never invent numbers.
            4. When suggesting savings, give exact amounts based on the user's data.
            5. Be friendly; use at most two emojis.
        """.trimIndent()
    }
}
