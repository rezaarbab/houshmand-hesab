package com.houshmandhesab.app.util

object Format {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(s: String): String = buildString {
        for (c in s) {
            if (c.isDigit()) append(persianDigits[c - '0']) else append(c)
        }
    }

    fun fromPersianDigits(s: String): String = buildString {
        for (c in s) {
            append(if (c in '۰'..'۹') ('۰'..'۹').toList()[c - '۰'] else c)
        }
    }

    fun money(amount: Long, persianDigits: Boolean, symbol: String = ""): String {
        val grouped = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(amount)
        val body = if (persianDigits) toPersianDigits(grouped) else grouped
        return if (symbol.isBlank()) body else "$body $symbol"
    }

    fun compact(amount: Long, persianDigits: Boolean): String {
        val abs = kotlin.math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        val s = when {
            abs >= 1_000_000_000 -> "%.1fB".format(abs / 1_000_000_000.0)
            abs >= 1_000_000 -> "%.1fM".format(abs / 1_000_000.0)
            abs >= 1_000 -> "%.1fK".format(abs / 1_000.0)
            else -> abs.toString()
        }
        val out = sign + s
        return if (persianDigits) toPersianDigits(out) else out
    }

    fun parseAmount(raw: String): Long {
        val cleaned = fromPersianDigits(raw).filter { it.isDigit() }
        return cleaned.toLongOrNull() ?: 0L
    }

    fun sha256(text: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
