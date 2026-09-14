package com.houshmandhesab.app.ai

import com.houshmandhesab.app.data.db.Category
import com.houshmandhesab.app.data.db.MonthTotals
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.util.Jalali
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
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
        val system = buildString {
            append(SYSTEM_PROMPT)
            append("\n\n")
            append(financeContext())
        }
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
        val prompt = "یادداشت این تراکنش: «$note»\n" +
            "لیست دسته‌ها: $names\n" +
            "فقط و فقط اسم مناسب‌ترین دسته از لیست بالا را بنویس. هیچ توضیح اضافه‌ای ننویس."
        return runMessages(
            listOf(
                ChatMessage("system", "تو یک دسته‌بندی‌کننده تراکنش هستی. فقط اسم یکی از دسته‌های داده‌شده را خروجی بده."),
                ChatMessage("user", prompt)
            )
        )
    }

    suspend fun financeContext(): String {
        val (jy, jm) = wallet.currentMonth()
        val (from, to) = wallet.monthRange(jy, jm)
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
            append("داده‌های مالی کاربر:\n")
            append("- موجودی کل: $totalBalance\n")
            append("- درآمد این ماه: ${totals.income}\n")
            append("- هزینه این ماه: ${totals.expense}\n")
            if (topCats.isNotEmpty()) {
                append("- بیشترین دسته‌های خرج این ماه: ")
                append(topCats.joinToString("، ") { "${it.name ?: "بدون دسته"} (${it.total})" })
                append("\n")
            }
            if (accounts.isNotEmpty()) {
                append("- حساب‌ها: ")
                append(accounts.joinToString("، ") { "${it.name}: ${balances[it.id] ?: 0L}" })
                append("\n")
            }
            if (recent.isNotEmpty()) {
                append("- آخرین تراکنش‌ها:\n")
                recent.forEach { tx ->
                    val cat = tx.tx.categoryId?.let { catNames[it] } ?: "بدون دسته"
                    val acc = accNames[tx.tx.accountId] ?: ""
                    append("  * ${tx.tx.type} / $cat / ${tx.tx.amount} / $acc / ${tx.tx.note.ifBlank { "-" }}\n")
                }
            }
            append("- تاریخ امروز (شمسی): ${Jalali.today().jy}/${Jalali.today().jm}/${Jalali.today().jd}\n")
        }
    }

    companion object {
        val SYSTEM_PROMPT = """
            تو «هوشمند حساب» هستی؛ یک حسابدار شخصی هوشمند و صمیمی.
            قوانین پاسخ:
            1. همیشه به زبان فارسی ساده و روان جواب بده.
            2. جواب‌ها کوتاه، کاربردی و مرحله‌به‌مرحله باشند (حداکثر ۸ خط).
            3. فقط بر اساس داده‌های مالی واقعی کاربر که در ابتدای این پیام آمده تحلیل کن؛ عدد از خودت نساز.
            4. اگر پیشنهاد پس‌انداز می‌دهی، مبلغ دقیق و مشخص بگو.
            5. لحن دوستانه داشته باش و حداکثر دو ایموجی استفاده کن.
        """.trimIndent()
    }
}
