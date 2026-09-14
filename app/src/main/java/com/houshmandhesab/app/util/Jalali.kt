package com.houshmandhesab.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class JDate(val jy: Int, val jm: Int, val jd: Int) {
    override fun toString(): String = "$jy/${"%02d".format(jm)}/${"%02d".format(jd)}"
}

object Jalali {

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private fun div(a: Int, b: Int): Int = a / b
    private fun mod(a: Int, b: Int): Int = a % b

    private fun jalCal(jy: Int): Triple<Int, Int, Int> {
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val gy = jy + 621
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
        var leap = mod(mod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return Triple(leap, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
            div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
        d -= div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    private fun d2g(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j += div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return Triple(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r.second, 3, r.third) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
    }

    private fun d2j(jdn: Int): Triple<Int, Int, Int> {
        val gy = d2g(jdn).first
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(gy, 3, r.third)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) return Triple(jy, 1 + div(k, 31), mod(k, 31) + 1)
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.first == 1) k += 1
        }
        return Triple(jy, 7 + div(k, 30), mod(k, 30) + 1)
    }

    fun isLeap(jy: Int): Boolean = jalCal(jy).first == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        isLeap(jy) -> 30
        else -> 29
    }

    fun monthName(jm: Int): String = when (jm) {
        1 -> "فروردین"
        2 -> "اردیبهشت"
        3 -> "خرداد"
        4 -> "تیر"
        5 -> "مرداد"
        6 -> "شهریور"
        7 -> "مهر"
        8 -> "آبان"
        9 -> "آذر"
        10 -> "دی"
        11 -> "بهمن"
        else -> "اسفند"
    }

    fun localDateToJalali(d: LocalDate): JDate {
        val jdn = g2d(d.year, d.monthValue, d.dayOfMonth)
        val (jy, jm, jd) = d2j(jdn)
        return JDate(jy, jm, jd)
    }

    fun jalaliToLocalDate(jy: Int, jm: Int, jd: Int): LocalDate {
        val jdn = j2d(jy, jm, jd)
        val (gy, gm, gd) = d2g(jdn)
        return LocalDate.of(gy, gm, gd)
    }

    fun fromMillis(millis: Long, zone: ZoneId = ZoneId.systemDefault()): JDate =
        localDateToJalali(Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())

    fun today(zone: ZoneId = ZoneId.systemDefault()): JDate =
        localDateToJalali(LocalDate.now(zone))

    fun toMillis(jy: Int, jm: Int, jd: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
        jalaliToLocalDate(jy, jm, jd).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    fun startOfMonthMillis(jy: Int, jm: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val g = jalaliToLocalDate(jy, jm, 1)
        return g.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun endOfMonthMillis(jy: Int, jm: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val len = monthLength(jy, jm)
        val g = jalaliToLocalDate(jy, jm, len)
        return g.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    }

    fun format(millis: Long, persianDigits: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
        val jd = fromMillis(millis, zone)
        val s = "%04d/%02d/%02d".format(jd.jy, jd.jm, jd.jd)
        return if (persianDigits) toPersianDigits(s) else s
    }

    fun formatLong(millis: Long, persianDigits: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
        val jd = fromMillis(millis, zone)
        val day = jd.jd.toString()
        val year = jd.jy.toString()
        val text = "${monthName(jd.jm)} ${if (persianDigits) toPersianDigits(day) else day}، ${if (persianDigits) toPersianDigits(year) else year}"
        return text
    }

    fun localDateTimeString(millis: Long, persianDigits: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
        val time = "%02d:%02d".format(ldt.hour, ldt.minute)
        val t = if (persianDigits) toPersianDigits(time) else time
        return "${format(millis, persianDigits, zone)} - $t"
    }
}
